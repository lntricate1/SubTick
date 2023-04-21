package subtick.queues;

import java.util.ArrayList;
import java.util.Iterator;

import me.jellysquid.mods.lithium.common.world.scheduler.LithiumServerTickScheduler;
import me.jellysquid.mods.lithium.common.world.scheduler.TickEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import oshi.util.tuples.Pair;
import subtick.SubTick;
import subtick.TickHandler;
import subtick.TickingMode;
import subtick.mixins.lithium.LithiumServerTickSchedulerAccessor;

public class ScheduledTickQueue<T> extends TickingQueue
{
  public static ScheduledTickQueue<Block> block(TickHandler handler)
  {
    return new ScheduledTickQueue<>(handler, handler.level.blockTicks, "block");
  }

  public static ScheduledTickQueue<Fluid> fluid(TickHandler handler)
  {
    return new ScheduledTickQueue<>(handler, handler.level.liquidTicks, "fluid");
  }

  private final ServerTickList<T> tickList;
  private final String typeName;

  private int lithium_scheduled_tick_step_index = 0;

  public ScheduledTickQueue(TickHandler handler, ServerTickList<T> tickList, String typeName)
  {
    super(handler);
    this.tickList = tickList;
    this.typeName = typeName;
  }

  @Override
  public void start(TickingMode mode)
  {
    if(SubTick.hasLithium)
      startLithium();
    else
      startVanilla();
  }

  @Override
  public Pair<Integer, Boolean> step(TickingMode mode, int count, BlockPos pos, int range)
  {
    if(SubTick.hasLithium)
      return stepLithium(count, pos, range);
    return stepVanilla(count, pos, range);
  }

  @Override
  public void end(TickingMode mode)
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
        executed_steps ++;
      }
    }
    exhausted = false;
    return new Pair<Integer, Boolean>(executed_steps, false);
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

  @Override
  public String getName(TickingMode mode, int steps)
  {
    return typeName + (steps == 1 ? " tick" : " ticks");
  }
}
