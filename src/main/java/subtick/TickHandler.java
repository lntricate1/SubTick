package subtick;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import subtick.network.ServerNetworkHandler;
import subtick.util.Translations;

/**
 * Stores the state of the server. Has public methods for scheduling actions.
 */
public class TickHandler
{
  private static CommandSourceStack actor;
  private static boolean frozen;
  private static boolean freezing, unfreezing;
  private static boolean stepping;
  private static int remainingTicks;

  private static TickPhase targetPhase = new TickPhase(0, 0);
  private static TickPhase currentPhase = new TickPhase(0, 0);

  public static boolean frozen(){return frozen;}
  public static boolean freezing(){return freezing;}
  public static TickPhase currentPhase(){return currentPhase;}
  public static TickPhase targetPhase(){return targetPhase;}

  /**
   * Placed in a {@link com.llamalad7.mixinextras.injector.WrapWithCondition} before each tick phase. Returns whether that phase should execute.
   */
  public static boolean shouldTick(ServerLevel level, int tickPhase)
  {
    TickPhase phase = new TickPhase(level, tickPhase);

    if(freezing && phase.equals(targetPhase))
    {
      freeze(phase);
      return false;
    }

    if(!frozen)
    {
      currentPhase = phase;
      return true;
    }
    // Everything below this is frozen logic

    Queues.end();

    if(unfreezing && phase.equals(currentPhase))
    {
      unfreeze();
      return true;
    }

    if(!stepping || !phase.equals(currentPhase))
      return false;
    // Continues only if stepping and in current phase and dim

    if(remainingTicks == 0 && phase.dim() == targetPhase.dim())
    {
      if(phase.phase() == targetPhase.phase())
      {
        stepping = false;
        Queues.execute();
        return false;
      }

      // This block will only execute if the step has to end at a phase that doesn't currently exist
      if(phase.phase() > targetPhase.phase())
      {
        // Go 2 phases back; From entityManagment to entity
        currentPhase = new TickPhase(phase.dim(), TickPhase.ENTITY);
        Translations.m(actor, "tickCommand.step.err.unloaded", phase);
        return false;
      }
    }

    if(phase.isLast())
      remainingTicks --;

    advancePhase(level);
    return true;
  }

  public static void advancePhase(ServerLevel level)
  {
    currentPhase = currentPhase.next(level);
  }

  private static void freeze(TickPhase phase)
  {
    freezing = false;
    frozen = true;
    currentPhase = phase;
  }

  private static void unfreeze()
  {
    unfreezing = false;
    frozen = false;
  }

  public static boolean scheduleFreeze(ServerLevel level, TickPhase phase)
  {
    if(frozen)
      return false;

    freezing = true;
    targetPhase = phase;
    ServerNetworkHandler.sendFrozen(level, phase);
    return true;
  }

  public static boolean scheduleUnfreeze(ServerLevel level)
  {
    if(!frozen)
      return false;

    if(freezing)
      freezing = false;
    else
    {
      unfreezing = true;
      stepping = false;
      Queues.scheduleEnd();
    }
    ServerNetworkHandler.sendUnfrozen(level);
    return true;
  }

  public static void scheduleStep(CommandSourceStack c, int ticks, TickPhase phase)
  {
    actor = c;
    stepping = true;
    remainingTicks = ticks;
    targetPhase = phase;
    if(ticks != 0 || !phase.equals(currentPhase))
    {
      Queues.scheduleEnd();
      ServerNetworkHandler.sendTickStep(c.getLevel(), ticks, phase);
    }
  }

  public static boolean canStep(CommandSourceStack c, int count, TickPhase phase)
  {
    if(!frozen)
    {
      Translations.m(c, "tickCommand.step.err.notfrozen");
      return false;
    }

    if(stepping)
    {
      Translations.m(c, "tickCommand.step.err.stepping");
      return false;
    }

    if(count == 0 && phase.isPriorTo(currentPhase))
    {
      Translations.m(c, "tickCommand.step.err.backwards");
      return false;
    }

    if(Queues.scheduled)
    {
      Translations.m(c, "tickCommand.step.err.qstepping");
      return false;
    }

    return true;
  }

  public static boolean canStep(int count, TickPhase phase)
  {
    if(!frozen)
      return false;

    if(stepping)
      return false;

    if(count == 0 && phase.isPriorTo(currentPhase))
      return false;

    if(Queues.scheduled)
      return false;

    return true;
  }
}
