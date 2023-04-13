package subtick;

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
import static subtick.SubTick.m;
import static subtick.SubTick.s;
import static subtick.SubTick.d;
import static subtick.SubTick.err;

public class Queues
{
  public static final DynamicCommandExceptionType INVALID_QUEUE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid queue '" + key + "'"));

  private final ServerLevel level;
  private final TickHandler handler;
  private final TickingQueue[] queues;

  private TickPhase currentPhase;
  private TickingQueue current;
  private TickingMode currentMode;
  private int count;
  private BlockPos pos;
  private int range;
  private CommandSourceStack actor;

  public boolean scheduled;
  private boolean stepping;

  public Queues(TickHandler handler)
  {
    this.level = handler.level;
    this.handler = handler;
    this.queues = new TickingQueue[SubTick.getTickPhaseOrder().length];
  }

  private TickingQueue get(TickPhase phase)
  {
    if (!phase.exists())
      return null;
    TickingQueue queue = queues[phase.getId()];
    if(queue == null)
      queue = queues[phase.getId()] = phase.newQueue(handler);
    return queue;
  }

  public void schedule(CommandContext<CommandSourceStack> c, TickPhase phase, TickingMode mode, int count, BlockPos pos, int range, boolean force) throws CommandSyntaxException
  {
    TickingQueue queue = get(phase);
    if (queue == null)
      throw INVALID_QUEUE_EXCEPTION.create(phase.getCommandKey());

    if(current == null)
      currentPhase = phase;
      current = queue;
      currentMode = mode;

    if(canStep(currentPhase, currentMode))
      handler.step(0, currentPhase);
    else
    {
      if(force)
      {
        if(!handler.canStep(c, 1, currentPhase))
          return;
        handler.step(1, currentPhase);
        currentPhase = phase;
        current = queue;
        currentMode = mode;
      }
      else
      {
        canStep(c, currentPhase, currentMode);
        return;
      }
    }

    this.actor = c.getSource();
    this.count = count;
    this.pos = pos;
    this.range = range;
    scheduled = true;
  }

  public void execute()
  {
    if(!scheduled) return;

    if(!stepping)
    {
      current.start(currentMode);
      stepping = true;
    }

    Pair<Integer, Boolean> pair = current.step(currentMode, count, pos, range);

    current.sendHighlights(level, actor);
    current.emptyHighlights();
    sendFeedback(pair.getA(), pair.getB());

    scheduled = false;
  }

  public void end()
  {
    if(!stepping) return;

    current.end(currentMode);
    current.clearHighlights();
    stepping = false;
    currentPhase = TickPhase.UNKNOWN;
    current = null;
    currentMode = null;
  }

  private void sendFeedback(int steps, boolean exhausted)
  {
    if(steps == 0)
      Messenger.m(actor, d(level), err(" "), p(currentPhase), err(" "), m(currentMode), err(" queue exhausted"));
    else if(exhausted)
      Messenger.m(actor, d(level), t(" stepped"), n(" " + steps + " "), s(current, currentMode, steps), t(" (queue exhausted)"));
    else
      Messenger.m(actor, d(level), t(" stepped"), n(" " + steps + " "), s(current, currentMode, steps));
  }

  public boolean canStep(CommandContext<CommandSourceStack> c, TickPhase phase, TickingMode mode)
  {
    if(!handler.canStep(c, 0, phase)) return false;

    if(handler.current_phase.isPriorTo(phase))
      return true;

    TickingQueue queue = get(phase);

    if(queue == null || queue.cantStep(mode))
    {
      Messenger.m(c.getSource(), d(level), err(" "), p(phase), err(" "), m(mode), err(" queue exhausted"));
      return false;
    }

    return true;
  }

  public boolean canStep(TickPhase phase, TickingMode mode)
  {
    if(!handler.canStep(0, phase)) return false;

    if(handler.current_phase.isPriorTo(phase))
      return true;

    TickingQueue queue = get(phase);

    if(queue == null || queue.cantStep(mode))
      return false;

    return true;
  }
}
