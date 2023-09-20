package subtick;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.lang3.tuple.Pair;
import subtick.queues.TickingQueue;
import subtick.util.Translations;

public class Queues
{
  public static final DynamicCommandExceptionType INVALID_QUEUE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid queue '" + key + "'"));
  private static final Map<String, Function<ServerLevel, ? extends TickingQueue>> FACTORIES = new HashMap<>();
  private final Map<String, TickingQueue> BY_COMMAND_KEY = new HashMap<>();
  private final TickHandler handler;
  private final ServerLevel level;

  private TickingQueue queue;
  private TickingQueue prev_queue;
  private int count;
  private BlockPos pos;
  private int range;
  private CommandSourceStack actor;

  public boolean scheduled;
  private boolean stepping;
  private boolean should_end;

  public Queues(TickHandler handler)
  {
    this.handler = handler;
    this.level = handler.level;
    for(Map.Entry<String, Function<ServerLevel, ? extends TickingQueue>> entry : FACTORIES.entrySet())
      BY_COMMAND_KEY.put(entry.getKey(), entry.getValue().apply(level));
  }

  public static void registerQueue(String commandKey, Function<ServerLevel, ? extends TickingQueue> queueFactory)
  {
    FACTORIES.put(commandKey, queueFactory);
  }

  public static String[] getQueues()
  {
    return FACTORIES.keySet().toArray(new String[0]);
  }

  public TickingQueue byCommandKey(String commandKey) throws CommandSyntaxException
  {
    TickingQueue queue = BY_COMMAND_KEY.get(commandKey);
    if(queue == null)
      throw INVALID_QUEUE_EXCEPTION.create(commandKey);
    return queue;
  }

  public void schedule(CommandSourceStack c, String commandKey, String modeKey, int count, BlockPos pos, int range, boolean force) throws CommandSyntaxException
  {
    TickingQueue newQueue = byCommandKey(commandKey);
    if(canStep(newQueue))
      handler.step(0, newQueue.getPhase());
    else
    {
      if(force)
      {
        if(!handler.canStep(c, 1, newQueue.getPhase()))
          return;
        handler.step(1, newQueue.getPhase());
      }
      else
      {
        canStep(c, newQueue);
        return;
      }
    }

    queue = newQueue;
    queue.setMode(modeKey);
    this.actor = c;
    this.count = count;
    this.pos = pos;
    this.range = range;
    scheduled = true;
  }

  public void scheduleEnd()
  {
    should_end = true;
  }

  public void execute()
  {
    if(!scheduled) return;

    if(!stepping)
    {
      queue.start();
      stepping = true;
    }

    // Protects program state when stepping into an update suppressor
    try
    {
      Pair<Integer, Boolean> pair = queue.step(count, pos, range);
      queue.sendHighlights(actor);
      sendFeedback(pair.getLeft(), pair.getRight());
    }
    catch(Exception e)
    {
      Translations.m(actor, "queueCommand.err.crash", level, queue);
    }

    queue.emptyHighlights();

    prev_queue = queue;
    scheduled = false;
  }

  public void end()
  {
    if(!should_end) return;
    should_end = false;
    if(!stepping) return;

    prev_queue.step(1, BlockPos.ZERO, -2);
    prev_queue.end();
    prev_queue.exhausted = false;
    prev_queue.clearHighlights();
    handler.advancePhase();
    stepping = false;
  }

  private void sendFeedback(int steps, boolean exhausted)
  {
    if(steps == 0)
      Translations.m(actor, "queueCommand.err.exhausted", level, queue);
    else if(steps == 1)
      if(exhausted)
        Translations.m(actor, "queueCommand.success.single.exhausted", level, queue, steps);
      else
        Translations.m(actor, "queueCommand.success.single", level, queue, steps);
    else
      if(exhausted)
        Translations.m(actor, "queueCommand.success.multiple.exhausted", level, queue, steps);
      else
        Translations.m(actor, "queueCommand.success.multiple", level, queue, steps);
  }

  public boolean canStep(CommandSourceStack c, TickingQueue queue)
  {
    if(!handler.canStep(c, 0, queue.getPhase())) return false;

    if(handler.current_phase.isPriorTo(queue.getPhase()))
      return true;

    if(queue.cantStep())
    {
      Translations.m(c, "queueCommand.err.exhausted", level, queue);
      return false;
    }

    return true;
  }

  public boolean canStep(TickingQueue queue)
  {
    if(!handler.canStep(0, queue.getPhase())) return false;

    if(handler.current_phase.isPriorTo(queue.getPhase()))
      return true;

    if(queue.cantStep())
      return false;

    return true;
  }
}
