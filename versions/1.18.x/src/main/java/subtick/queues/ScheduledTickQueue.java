package subtick.queues;

// import java.util.ArrayList;
import java.util.HashMap;
// import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

// import me.jellysquid.mods.lithium.common.world.scheduler.LithiumServerTickScheduler;
// import me.jellysquid.mods.lithium.common.world.scheduler.TickEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import oshi.util.tuples.Pair;
// import subtick.SubTick;
import subtick.TickPhase;
import subtick.TickingMode;
// import subtick.mixins.lithium.LithiumServerTickSchedulerAccessor;

public class ScheduledTickQueue<T> extends TickingQueue
{
  private final TickingMode INDEX;
  private final TickingMode PRIORITY;

  private final LevelTicks<T> levelTicks;
  private final BiConsumer<BlockPos, T> biConsumer;
  // private int lithium_scheduled_tick_step_index = 0;

  public static ScheduledTickQueue<Block> block(ServerLevel level)
  {
    return new ScheduledTickQueue<Block>(
      new TickingMode("Block Tick", "Block Ticks"),
      new TickingMode("Block Tick Priority", "Block Tick Priorities"),
      level, level.blockTicks, level::tickBlock, TickPhase.BLOCK_TICK, "blockTick");
  }

  public static ScheduledTickQueue<Fluid> fluid(ServerLevel level)
  {
    return new ScheduledTickQueue<Fluid>(
      new TickingMode("Fluid Tick", "Fluid Ticks"),
      level, level.fluidTicks, level::tickFluid, TickPhase.FLUID_TICK, "fluidTick");
  }

  public ScheduledTickQueue(TickingMode INDEX, TickingMode PRIORITY, ServerLevel level, LevelTicks<T> levelTicks, BiConsumer<BlockPos, T> biConsumer, TickPhase phase, String commandKey)
  {
    super(Map.of("index", INDEX, "priority", PRIORITY), INDEX, level, phase, commandKey);
    this.INDEX = INDEX;
    this.PRIORITY = PRIORITY;
    this.levelTicks = levelTicks;
    this.biConsumer = biConsumer;
  }

  public ScheduledTickQueue(TickingMode INDEX, ServerLevel level, LevelTicks<T> levelTicks, BiConsumer<BlockPos, T> biConsumer, TickPhase phase, String commandKey)
  {
    super(new HashMap<>(), INDEX, level, phase, commandKey);
    this.INDEX = INDEX;
    this.PRIORITY = null;
    this.levelTicks = levelTicks;
    this.biConsumer = biConsumer;
  }

  @Override
  public void start()
  {
    // if(SubTick.hasLithium)
    //   startLithium();
    // else
      startVanilla();
  }

  @Override
  public Pair<Integer, Boolean> step(int count, BlockPos pos, int range)
  {
    // if(SubTick.hasLithium)
    //   return stepLithium(count, pos, range);
    return stepVanilla(count, pos, range);
  }

  @Override
  public void end()
  {
    // if(SubTick.hasLithium)
    //   endLithium();
    // else
      endVanilla();
  }

  // private void startVanilla()
  // {
  //   Iterator<TickNextTickData<T>> iterator = levelTicks.tickNextTickList.iterator();
  //   for(int i = 0; i < 65536 && iterator.hasNext();)
  //   {
  //     TickNextTickData<T> tick = iterator.next();
  //     if(tick.triggerTick > level.getGameTime())
  //       break;
  //     if(level.isPositionTickingWithEntitiesLoaded(tick.pos))
  //     {
  //       iterator.remove();
  //       levelTicks.tickNextTickSet.remove(tick);
  //       levelTicks.currentlyTicking.add(tick);
  //       i ++;
  //     }
  //   }
  // }
  private void startVanilla()
  {
    levelTicks.collectTicks(level.getGameTime(), 65536, levelTicks.profiler.get());
  }

  // private void startLithium()
  // {
  //   ((LithiumServerTickScheduler<T>)levelTicks).selectTicks(level.getGameTime());
  //   lithium_scheduled_tick_step_index = 0;
  // }

  // public Pair<Integer, Boolean> stepVanilla(int count, BlockPos pos, int range)
  // {
  //   int executed_steps = 0;
  //   while(executed_steps < count)
  //   {
  //     TickNextTickData<T> tick = levelTicks.currentlyTicking.poll();
  //     if(tick == null)
  //     {
  //       exhausted = true;
  //       return new Pair<Integer, Boolean>(executed_steps, true);
  //     }
  //
  //     if(level.isPositionTickingWithEntitiesLoaded(tick.pos))
  //     {
  //       levelTicks.alreadyTicked.add(tick);
  //       levelTicks.ticker.accept(tick);
  //     }
  //     else
  //       levelTicks.scheduleTick(tick.pos, tick.getType(), 0);
  //
  //     if(rangeCheck(tick.pos, pos, range))
  //     {
  //       addBlockOutline(tick.pos);
  //       if(currentMode == INDEX)
  //         executed_steps ++;
  //     }
  //
  //     if(currentMode == PRIORITY)
  //     {
  //       TickNextTickData<T> nextTick = levelTicks.currentlyTicking.peek();
  //       if(nextTick != null && nextTick.priority != tick.priority)
  //         executed_steps ++;
  //     }
  //   }
  //   return new Pair<Integer, Boolean>(executed_steps, exhausted = levelTicks.currentlyTicking.isEmpty());
  // }
  public Pair<Integer, Boolean> stepVanilla(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    while(executed_steps < count && !levelTicks.toRunThisTick.isEmpty())
    {
      ScheduledTick<T> scheduledTick = levelTicks.toRunThisTick.poll();
      if(!levelTicks.toRunThisTickSet.isEmpty())
        levelTicks.toRunThisTickSet.remove(scheduledTick);
      levelTicks.alreadyRunThisTick.add(scheduledTick);
      biConsumer.accept(scheduledTick.pos(), (T)scheduledTick.type());

      if(rangeCheck(scheduledTick.pos(), pos, range))
      {
        addBlockOutline(scheduledTick.pos());
        if(currentMode == INDEX)
          executed_steps ++;
      }
      if(currentMode == PRIORITY)
      {
        ScheduledTick<T> nextTick = levelTicks.toRunThisTick.peek();
        if(nextTick == null || nextTick.priority() != scheduledTick.priority())
          executed_steps ++;
      }
    }
    return new Pair<Integer, Boolean>(executed_steps, exhausted = levelTicks.toRunThisTick.isEmpty());
  }

  // Accessor cast warnings
  // @SuppressWarnings("unchecked")
  // private Pair<Integer, Boolean> stepLithium(int count, BlockPos pos, int range)
  // {
  //   LithiumServerTickSchedulerAccessor<T> scheduler = (LithiumServerTickSchedulerAccessor<T>)(LithiumServerTickScheduler<T>)levelTicks;
  //   int executed_steps = 0;
  //   ArrayList<TickEntry<T>> ticks = scheduler.getExecutingTicks();
  //   int ticksSize = ticks.size();
  //   for(; lithium_scheduled_tick_step_index < ticksSize && executed_steps < count; lithium_scheduled_tick_step_index++)
  //   {
  //     TickEntry<T> tick = ticks.get(lithium_scheduled_tick_step_index);
  //     if(tick == null)
  //       continue;
  //     tick.consumed = true;
  //     scheduler.getTickConsumer().accept(tick);
  //     if(rangeCheck(tick.pos, pos, range))
  //     {
  //       addBlockOutline(tick.pos);
  //       if(currentMode == INDEX)
  //         executed_steps ++;
  //     }
  //
  //     if(currentMode == PRIORITY)
  //     {
  //       TickEntry<T> nextTick = ticks.get(lithium_scheduled_tick_step_index + 1);
  //       if(nextTick != null && nextTick.priority != tick.priority)
  //         executed_steps ++;
  //     }
  //   }
  //   return new Pair<Integer, Boolean>(executed_steps, exhausted = lithium_scheduled_tick_step_index == ticksSize);
  // }

  // private void endVanilla()
  // {
  //   levelTicks.alreadyTicked.clear();
  //   levelTicks.currentlyTicking.clear();
  // }
  private void endVanilla()
  {
    levelTicks.cleanupAfterTick();
  }

  // Accessor cast warnings
  // @SuppressWarnings("unchecked")
  // private void endLithium()
  // {
  //   ((LithiumServerTickSchedulerAccessor<T>)levelTicks).getExecutingTicks().clear();
  //   ((LithiumServerTickSchedulerAccessor<T>)levelTicks).getExecutingTicksSet().clear();
  // }
}
