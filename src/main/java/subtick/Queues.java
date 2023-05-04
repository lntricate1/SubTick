package subtick;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import oshi.util.tuples.Pair;
import subtick.queues.TickingQueue;

import static subtick.SubTick.t;
import static subtick.SubTick.n;
import static subtick.SubTick.p;
import static subtick.SubTick.d;
import static subtick.SubTick.err;

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

  public void schedule(CommandContext<CommandSourceStack> c, String commandKey, String modeKey, int count, BlockPos pos, int range, boolean force) throws CommandSyntaxException
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
    this.actor = c.getSource();
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

    Pair<Integer, Boolean> pair = queue.step(count, pos, range);

    queue.sendHighlights(actor);
    queue.emptyHighlights();
    sendFeedback(pair.getA(), pair.getB());

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
      Messenger.m(actor, d(level), err(" "), p(queue, 1), err(" queue exhausted"));
    else if(exhausted)
      Messenger.m(actor, d(level), t(" stepped"), n(" " + steps + " "), p(queue, steps), t(" (queue exhausted)"));
    else
      Messenger.m(actor, d(level), t(" stepped"), n(" " + steps + " "), p(queue, steps));
  }

  public boolean canStep(CommandContext<CommandSourceStack> c, TickingQueue queue)
  {
    if(!handler.canStep(c, 0, queue.getPhase())) return false;

    if(handler.current_phase.isPriorTo(queue.getPhase()))
      return true;

    if(queue.cantStep())
    {
      Messenger.m(c.getSource(), d(level), err(" "), p(queue.getPhase()), err(" queue exhausted"));
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
