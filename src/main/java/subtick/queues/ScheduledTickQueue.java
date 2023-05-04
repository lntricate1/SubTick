package subtick.queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import me.jellysquid.mods.lithium.common.world.scheduler.LithiumServerTickScheduler;
import me.jellysquid.mods.lithium.common.world.scheduler.TickEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import oshi.util.tuples.Pair;
import subtick.SubTick;
import subtick.TickPhase;
import subtick.TickingMode;
import subtick.mixins.lithium.LithiumServerTickSchedulerAccessor;

public class ScheduledTickQueue<T> extends TickingQueue
{
  private final TickingMode INDEX;
  private final TickingMode PRIORITY;

  private final ServerTickList<T> tickList;
  private int lithium_scheduled_tick_step_index = 0;

  public static ScheduledTickQueue<Block> block(ServerLevel level)
  {
    return new ScheduledTickQueue<Block>(
      new TickingMode("Block Tick", "Block Ticks"),
      new TickingMode("Block Tick Priority", "Block Tick Priorities"),
      level.blockTicks, level, TickPhase.BLOCK_TICK, "blockTick");
  }

  public static ScheduledTickQueue<Fluid> fluid(ServerLevel level)
  {
    return new ScheduledTickQueue<Fluid>(
      new TickingMode("Fluid Tick", "Fluid Ticks"),
      level.liquidTicks, level, TickPhase.FLUID_TICK, "fluidTick");
  }

  public ScheduledTickQueue(TickingMode INDEX, TickingMode PRIORITY, ServerTickList<T> tickList, ServerLevel level, TickPhase phase, String commandKey)
  {
    super(Map.of("index", INDEX, "priority", PRIORITY), INDEX, level, phase, commandKey);
    this.INDEX = INDEX;
    this.PRIORITY = PRIORITY;
    this.tickList = tickList;
  }

  public ScheduledTickQueue(TickingMode INDEX, ServerTickList<T> tickList, ServerLevel level, TickPhase phase, String commandKey)
  {
    super(new HashMap<>(), INDEX, level, phase, commandKey);
    this.INDEX = INDEX;
    this.PRIORITY = null;
    this.tickList = tickList;
  }

  @Override
  public void start()
  {
    if(SubTick.hasLithium)
      startLithium();
    else
      startVanilla();
  }

  @Override
  public Pair<Integer, Boolean> step(int count, BlockPos pos, int range)
  {
    if(SubTick.hasLithium)
      return stepLithium(count, pos, range);
    return stepVanilla(count, pos, range);
  }

  @Override
  public void end()
  {
    if(SubTick.hasLithium)
      endLithium();
    else
      endVanilla();
  }

  private void startVanilla()
  {
    Iterator<TickNextTickData<T>> iterator = tickList.tickNextTickList.iterator();
    for(int i = 0; i < 65536 && iterator.hasNext();)
    {
      TickNextTickData<T> tick = iterator.next();
      if(tick.triggerTick > level.getGameTime())
        break;
      if(level.isPositionTickingWithEntitiesLoaded(tick.pos))
      {
        iterator.remove();
        tickList.tickNextTickSet.remove(tick);
        tickList.currentlyTicking.add(tick);
        i ++;
      }
    }
  }

  private void startLithium()
  {
    ((LithiumServerTickScheduler<T>)tickList).selectTicks(level.getGameTime());
    lithium_scheduled_tick_step_index = 0;
  }

  public Pair<Integer, Boolean> stepVanilla(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    while(executed_steps < count)
    {
      TickNextTickData<T> tick = tickList.currentlyTicking.poll();
      if(tick == null)
      {
        exhausted = true;
        return new Pair<Integer, Boolean>(executed_steps, true);
      }

      if(level.isPositionTickingWithEntitiesLoaded(tick.pos))
      {
        tickList.alreadyTicked.add(tick);
        tickList.ticker.accept(tick);
      }
      else
        tickList.scheduleTick(tick.pos, tick.getType(), 0);

      if(rangeCheck(tick.pos, pos, range))
      {
        addBlockOutline(tick.pos);
        if(currentMode == INDEX)
          executed_steps ++;
      }

      if(currentMode == PRIORITY)
      {
        TickNextTickData<T> nextTick = tickList.currentlyTicking.peek();
        if(nextTick != null && nextTick.priority != tick.priority)
          executed_steps ++;
      }
    }
    return new Pair<Integer, Boolean>(executed_steps, exhausted = tickList.currentlyTicking.isEmpty());
  }

  // Accessor cast warnings
  @SuppressWarnings("unchecked")
  private Pair<Integer, Boolean> stepLithium(int count, BlockPos pos, int range)
  {
    LithiumServerTickSchedulerAccessor<T> scheduler = (LithiumServerTickSchedulerAccessor<T>)(LithiumServerTickScheduler<T>)tickList;
    int executed_steps = 0;
    ArrayList<TickEntry<T>> ticks = scheduler.getExecutingTicks();
    int ticksSize = ticks.size();
    for(; lithium_scheduled_tick_step_index < ticksSize && executed_steps < count; lithium_scheduled_tick_step_index++)
    {
      TickEntry<T> tick = ticks.get(lithium_scheduled_tick_step_index);
      if(tick == null)
        continue;
      tick.consumed = true;
      scheduler.getTickConsumer().accept(tick);
      if(rangeCheck(tick.pos, pos, range))
      {
        addBlockOutline(tick.pos);
        if(currentMode == INDEX)
          executed_steps ++;
      }

      if(currentMode == PRIORITY)
      {
        TickEntry<T> nextTick = ticks.get(lithium_scheduled_tick_step_index + 1);
        if(nextTick != null && nextTick.priority != tick.priority)
          executed_steps ++;
      }
    }
    return new Pair<Integer, Boolean>(executed_steps, exhausted = lithium_scheduled_tick_step_index == ticksSize);
  }

  private void endVanilla()
  {
    tickList.alreadyTicked.clear();
    tickList.currentlyTicking.clear();
  }

  // Accessor cast warnings
  @SuppressWarnings("unchecked")
  private void endLithium()
  {
    ((LithiumServerTickSchedulerAccessor<T>)tickList).getExecutingTicks().clear();
    ((LithiumServerTickSchedulerAccessor<T>)tickList).getExecutingTicksSet().clear();
  }
}
