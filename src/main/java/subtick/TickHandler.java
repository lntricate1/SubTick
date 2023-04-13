package subtick;

import static subtick.SubTick.d;
import static subtick.SubTick.err;

import net.minecraft.server.level.ServerLevel;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import carpet.utils.Messenger;

import subtick.network.ServerNetworkHandler;

public class TickHandler
{
  public final ServerLevel level;
  public final Queues queues;
  public long time;

  // Freeze
  public boolean frozen = false;
  public boolean freezing = false;
  private boolean unfreezing = false;
  private TickPhase target_phase = TickPhase.byId(0);
  // Step
  private boolean stepping = false;
  private boolean in_first_stepped_phase = false;
  private int remaining_ticks = 0;
  public TickPhase current_phase = TickPhase.byId(0);

  public TickHandler(ServerLevel level)
  {
    this.level = level;
    this.time = level.getGameTime();
    queues = new Queues(this);
  }

  public void tickTime()
  {
    time += 1L;
  }

  public boolean shouldTick(TickPhase phase)
  {
    // Freezing
    if(freezing && phase == target_phase)
    {
      freezing = false;
      frozen = true;
      current_phase = phase;
      ServerNetworkHandler.updateFrozenStateToConnectedPlayers(level, true);
      return false;
    }

    // Normal ticking
    if(!frozen)
    {
      current_phase = phase;
      return true;
    }
    // Everything below this is frozen logic

    // Unfreezing
    if(unfreezing && phase == current_phase)
    {
      unfreezing = false;
      frozen = false;
      ServerNetworkHandler.updateFrozenStateToConnectedPlayers(level, false);
      return true;
    }

    if(!stepping || phase != current_phase) return false;
    // Continues only if stepping and in current_phase

    // Stepping
    if(in_first_stepped_phase)
      ServerNetworkHandler.updateTickPlayerActiveTimeoutToConnectedPlayers(level, remaining_ticks);
    else if(phase.isFirst())
      --remaining_ticks;

    in_first_stepped_phase = false;
    if(remaining_ticks < 1 && phase == target_phase)
    {
      stepping = false;
      queues.execute();
      return false;
    }
    advancePhase();
    return true;
  }

  public void advancePhase()
  {
    current_phase = current_phase.next();
  }

  public void step(int ticks, TickPhase phase)
  {
    stepping = true;
    in_first_stepped_phase = true;
    remaining_ticks = ticks;
    target_phase = phase;
    if(ticks != 0 || phase != current_phase)
      queues.end();
  }

  public void freeze(TickPhase phase)
  {
    freezing = true;
    target_phase = phase;
  }

  public void unfreeze()
  {
    if(freezing) freezing = false;
    else
    {
      unfreezing = true;
      stepping = false;
      queues.end();
    }
  }

  public boolean canStep(CommandContext<CommandSourceStack> c, int count, TickPhase phase)
  {
    if(!frozen)
    {
      Messenger.m(c.getSource(), d(level), err(" cannot step because it's not frozen"));
      return false;
    }

    if(stepping)
    {
      Messenger.m(c.getSource(), d(level), err(" cannot step because it's already tick stepping"));
      return false;
    }

    if(count == 0 && phase.isPriorTo(current_phase))
    {
      Messenger.m(c.getSource(), d(level), err(" cannot step to an earlier phase in the same tick"));
      return false;
    }

    if(queues.scheduled)
    {
      Messenger.m(c.getSource(), d(level), err(" cannot step because it's already queueStepping"));
      return false;
    }

    return true;
  }

  public boolean canStep(int count, TickPhase phase)
  {
    if(!frozen)
      return false;

    if(stepping)
      return false;

    if(count == 0 && phase.isPriorTo(current_phase))
      return false;

    if(queues.scheduled)
      return false;

    return true;
  }
}
