package subtick;

import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import oshi.util.tuples.Pair;
import subtick.queues.AbstractQueue;
import subtick.queues.BlockEntityQueue;
import subtick.queues.BlockEventDepthQueue;
import subtick.queues.BlockEventQueue;
import subtick.queues.BlockTickQueue;
import subtick.queues.EntityQueue;
import subtick.queues.FluidTickQueue;

import static subtick.SubTick.t;
import static subtick.SubTick.n;
import static subtick.SubTick.p;
import static subtick.SubTick.d;
import static subtick.SubTick.err;

public class Queues
{
  public static final DynamicCommandExceptionType INVALID_QUEUE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid queue '" + key + "'"));
  private final Map<String, AbstractQueue> BY_COMMAND_KEY = new HashMap<>();
  private final TickHandler handler;
  private final ServerLevel level;

  private AbstractQueue queue;
  private AbstractQueue prev_queue;
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
    // This thing should really be improved
    BY_COMMAND_KEY.put("blockTick", new BlockTickQueue());
    BY_COMMAND_KEY.put("fluidTick", new FluidTickQueue());
    BY_COMMAND_KEY.put("blockEvent", new BlockEventQueue());
    BY_COMMAND_KEY.put("blockEventDepth", new BlockEventDepthQueue());
    BY_COMMAND_KEY.put("entity", new EntityQueue());
    BY_COMMAND_KEY.put("blockEntity", new BlockEntityQueue());
  }

  public AbstractQueue byCommandKey(String commandKey) throws CommandSyntaxException
  {
    AbstractQueue queue = BY_COMMAND_KEY.get(commandKey);
    if(queue == null)
      throw INVALID_QUEUE_EXCEPTION.create(commandKey);
    return queue;
  }

  public void schedule(CommandContext<CommandSourceStack> c, String commandKey, int count, BlockPos pos, int range, boolean force)
  {
    AbstractQueue newQueue = BY_COMMAND_KEY.get(commandKey);
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
      queue.start(level);
      stepping = true;
    }

    Pair<Integer, Boolean> pair = queue.step(count, level, pos, range);

    queue.sendHighlights(level, actor);
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

    prev_queue.step(1, level, BlockPos.ZERO, -2);
    prev_queue.end(level);
    prev_queue.exhausted = false;
    prev_queue.clearHighlights(level);
    handler.advancePhase();
    stepping = false;
  }

  private void sendFeedback(int steps, boolean exhausted)
  {
    if(steps == 0)
      Messenger.m(actor, d(level), err(" "), p(queue.getPhase()), err(" queue exhausted"));
    else if(exhausted)
      Messenger.m(actor, d(level), t(" stepped"), n(" " + steps + " "), p(queue, steps), t(" (queue exhausted)"));
    else
      Messenger.m(actor, d(level), t(" stepped"), n(" " + steps + " "), p(queue, steps));
  }

  public boolean canStep(CommandContext<CommandSourceStack> c, AbstractQueue queue)
  {
    if(!handler.canStep(c, 0, queue.getPhase())) return false;

    if(handler.current_phase.isPriorTo(queue.getPhase()))
      return true;

    if(queue.cantStep(level))
    {
      Messenger.m(c.getSource(), d(level), err(" "), p(queue.getPhase()), err(" queue exhausted"));
      return false;
    }

    return true;
  }

  public boolean canStep(AbstractQueue queue)
  {
    if(!handler.canStep(0, queue.getPhase())) return false;

    if(handler.current_phase.isPriorTo(queue.getPhase()))
      return true;

    if(queue.cantStep(level))
      return false;

    return true;
  }
}
