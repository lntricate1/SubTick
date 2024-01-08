package subtick;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;

import org.apache.commons.lang3.tuple.Triple;

import subtick.network.ServerNetworkHandler;
import subtick.queues.BlockEventQueue;
import subtick.queues.TickingQueue;
import subtick.util.Translations;

public class Queues
{
  public static final DynamicCommandExceptionType INVALID_QUEUE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid queue '" + key + "'"));
  // private final TickHandler handler;
  // private final ServerLevel level;

  private static TickingQueue queue;
  private static TickingQueue prev_queue;
  private static int count;
  private static BlockPos pos;
  private static int range;
  private static CommandSourceStack actor;
  private static ServerLevel level;

  public static boolean scheduled;
  private static boolean stepping;
  private static boolean should_end;

  private static void step(TickingQueue newQueue, CommandSourceStack c, int newCount, BlockPos newPos, int newRange) throws CommandSyntaxException
  {
    queue = newQueue;
    actor = c;
    count = newCount;
    pos = newPos;
    range = newRange;
    scheduled = true;
  }

  public static void schedule(CommandSourceStack c, TickingQueue newQueue, String modeKey, int count, BlockPos pos, int range, boolean force) throws CommandSyntaxException
  {
    level = c.getLevel();
    newQueue.setMode(modeKey);
    TickPhase phase = new TickPhase(level, newQueue.getPhase());

    if(force ? TickHandler.canStep(0, phase) : TickHandler.canStep(c, 0, phase))
    {
      step(newQueue, c, count, pos, range);
      TickHandler.scheduleStep(c, 0, phase);
    }
    else if(force && TickHandler.canStep(c, 1, phase))
    {
      step(newQueue, c, count, pos, range);
      TickHandler.scheduleStep(c, 1, phase);
    }
  }

  public static void scheduleEnd()
  {
    should_end = true;
  }

  public static void execute()
  {
    if(!scheduled) return;

    if(!stepping)
    {
      queue.start(level);
      stepping = true;
    }

    // Protects program state when stepping into an update suppressor
    try
    {
      Triple<Integer, Integer, Boolean> triple = queue.step(count, pos, range);
      queue.sendQueues(actor, triple.getLeft());
      sendFeedback(triple.getMiddle(), triple.getRight());
    }
    catch(Exception e)
    {
      Translations.m(actor, "queueCommand.err.crash", queue);
    }

    prev_queue = queue;
    scheduled = false;
  }

  public static void end()
  {
    if(!should_end)
      return;

    should_end = false;
    if(!stepping)
      return;

    prev_queue.step(1, BlockPos.ZERO, -2);
    prev_queue.end();
    prev_queue.exhausted = false;
    TickHandler.advancePhase(level);
    // this clear block event highlights
    ServerNetworkHandler.sendTickStep(level, 0, TickHandler.targetPhase());
    stepping = false;
  }

  private static void sendFeedback(int steps, boolean exhausted)
  {
    if(steps == 0)
      Translations.m(actor, "queueCommand.err.exhausted", queue);
    else if(steps == 1)
      if(exhausted)
        Translations.m(actor, "queueCommand.success.single.exhausted", queue, steps);
      else
        Translations.m(actor, "queueCommand.success.single", queue, steps);
    else
      if(exhausted)
        Translations.m(actor, "queueCommand.success.multiple.exhausted", queue, steps);
      else
        Translations.m(actor, "queueCommand.success.multiple", queue, steps);
  }

  public static void onScheduleBlockEvent(ServerLevel level, BlockEventData be)
  {
    if(stepping && queue instanceof BlockEventQueue beq)
      beq.updateQueue(level, be);
  }
}
