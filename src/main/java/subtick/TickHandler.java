package subtick;

import static subtick.TickHandlers.t;

import net.minecraft.server.level.ServerLevel;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import carpet.utils.Messenger;

import subtick.network.ServerNetworkHandler;

public class TickHandler
{
  public final ServerLevel level;
  public final String dimensionName;
  public final Queues queues = new Queues(this);
  public long time;

  // Freeze
  public boolean frozen = false;
  public boolean freezing = false;
  private boolean unfreezing = false;
  private int target_phase = 0;
  // Step
  private boolean stepping = false;
  private boolean in_first_stepped_phase = false;
  private int remaining_ticks = 0;
  public int current_phase = 0;

  public TickHandler(String dimensionName, ServerLevel level)
  {
    this.dimensionName = dimensionName.substring(0, 1).toUpperCase() + dimensionName.substring(1);
    this.level = level;
    this.time = level.getGameTime();
  }

  public void tickTime()
  {
    time += 1L;
  }

  public boolean shouldTick(int phase)
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
    else if(phase == 0)
      --remaining_ticks;

    in_first_stepped_phase = false;
    if(remaining_ticks < 1 && phase == target_phase)
    {
      stepping = false;
      queues.executeScheduledSteps();
      return false;
    }
    advancePhase();
    return true;
  }

  public void advancePhase()
  {
    current_phase = (current_phase + 1) % TickHandlers.TOTAL_PHASES;
  }

  public void step(int ticks, int phase)
  {
    stepping = true;
    in_first_stepped_phase = true;
    remaining_ticks = ticks;
    target_phase = phase;
    if(ticks != 0 || phase != current_phase)
      queues.finishQueueStep();
  }

  public void freeze(int phase)
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
      queues.finishQueueStep();
    }
  }

  public boolean canStep(CommandContext<CommandSourceStack> c, int count, int phase)
  {
    if(!frozen)
    {
      Messenger.m(c.getSource(), getDimension(), t(" cannot step because it's not frozen"));
      return false;
    }

    if(stepping)
    {
      Messenger.m(c.getSource(), getDimension(), t(" cannot step because it's already tick stepping"));
      return false;
    }

    if(count == 0 && phase < current_phase)
    {
      Messenger.m(c.getSource(), getDimension(), t(" cannot step to an earlier phase in the same tick"));
      return false;
    }

    if(queues.scheduled != -1)
    {
      Messenger.m(c.getSource(), getDimension(), t(" cannot step because it's already queueStepping"));
      return false;
    }

    return true;
  }

  public String getPhase()
  {
    return Settings.subtickPhaseFormat + " " + TickHandlers.tickPhaseNames[current_phase];
  }

  public String getDimension()
  {
    return Settings.subtickDimensionFormat + " " + dimensionName;
  }
}
