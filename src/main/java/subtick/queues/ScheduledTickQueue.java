package subtick.queues;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.lang3.tuple.Triple;

import subtick.QueueElement;
import subtick.TickPhase;
import subtick.TickingMode;

//#if MC >= 11800
//$$ import java.util.function.BiConsumer;
//$$ import net.minecraft.world.ticks.LevelTicks;
//$$ import net.minecraft.world.ticks.ScheduledTick;
//#else
import java.util.ArrayList;
import java.util.Iterator;

import me.jellysquid.mods.lithium.common.world.scheduler.LithiumServerTickScheduler;
import me.jellysquid.mods.lithium.common.world.scheduler.TickEntry;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;

import subtick.SubTick;
import subtick.mixins.lithium.LithiumServerTickSchedulerAccessor;
//#endif

public class ScheduledTickQueue<T> extends TickingQueue
{
  private final TickingMode INDEX;
  private final TickingMode PRIORITY;

  //#if MC >= 11800
  //$$ private LevelTicks<T> levelTicks;
  //$$ private BiConsumer<BlockPos, T> biConsumer;
  //#else
  private ServerTickList<T> tickList;
  private int lithium_scheduled_tick_step_index = 0;
  //#endif

  public static ScheduledTickQueue<Block> block()
  {
    return new ScheduledTickQueue<Block>(
      new TickingMode("Block Tick", "Block Ticks"),
      new TickingMode("Block Tick Priority", "Block Tick Priorities"),
      TickPhase.BLOCK_TICK, "blockTick");
  }

  public static ScheduledTickQueue<Fluid> fluid()
  {
    return new ScheduledTickQueue<Fluid>(
      new TickingMode("Fluid Tick", "Fluid Ticks"),
      TickPhase.FLUID_TICK, "fluidTick");
  }

  public ScheduledTickQueue(TickingMode INDEX, TickingMode PRIORITY, int phase, String commandKey)
  {
    super(Map.of("index", INDEX, "priority", PRIORITY), INDEX, phase, commandKey);
    this.INDEX = INDEX;
    this.PRIORITY = PRIORITY;
  }

  public ScheduledTickQueue(TickingMode INDEX, int phase, String commandKey)
  {
    super(new HashMap<>(), INDEX, phase, commandKey);
    this.INDEX = INDEX;
    this.PRIORITY = null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void start(ServerLevel level)
  {
    this.level = level;

    //#if MC >= 11800
    //$$ levelTicks = (LevelTicks<T>)(phase == TickPhase.BLOCK_TICK ? level.blockTicks : level.fluidTicks);
    //$$ biConsumer = (BiConsumer<BlockPos, T>)(phase == TickPhase.BLOCK_TICK ?
    //$$   (BiConsumer<BlockPos, Block>)level::tickBlock :
    //$$   (BiConsumer<BlockPos, Fluid>)level::tickFluid);
    //$$
    //$$ levelTicks.collectTicks(level.getGameTime(), 65536, levelTicks.profiler.get());
    //$$ queue.clear();
    //$$ for(ScheduledTick<T> scheduledTick : levelTicks.toRunThisTick)
    //$$   queue.add(new QueueElement(scheduledTick));
    //#else
    tickList = (ServerTickList<T>) (phase == TickPhase.BLOCK_TICK ? level.blockTicks : level.liquidTicks);
    if(SubTick.hasLithium)
      startLithium();
    else
      startVanilla();
    //#endif
  }

  @Override
  public Triple<Integer, Integer, Boolean> step(int count, BlockPos pos, int range)
  {
  //#if MC >= 11800
  //$$ int executed_steps = 0;
  //$$ int success_steps = 0;
  //$$ while(success_steps < count && !levelTicks.toRunThisTick.isEmpty())
  //$$ {
  //$$   ScheduledTick<T> tick = levelTicks.toRunThisTick.poll();
  //$$ 
  //$$   if(!levelTicks.toRunThisTickSet.isEmpty())
  //$$     levelTicks.toRunThisTickSet.remove(tick);
  //$$   levelTicks.alreadyRunThisTick.add(tick);
  //$$   biConsumer.accept(tick.pos(), (T)tick.type());
  //$$ 
  //$$   if(currentMode == INDEX)
  //$$   {
  //$$     if(rangeCheck(tick.pos(), pos, range))
  //$$       success_steps ++;
  //$$   }
  //$$   else
  //$$   {
  //$$     ScheduledTick<T> nextTick = levelTicks.toRunThisTick.peek();
  //$$     if(nextTick == null || nextTick.priority() != tick.priority())
  //$$       success_steps ++;
  //$$   }
  //$$   executed_steps ++;
  //$$ }
  //$$ return Triple.of(executed_steps, success_steps, exhausted = levelTicks.toRunThisTick.isEmpty());
  //#else
    if(SubTick.hasLithium)
      return stepLithium(count, pos, range);
    return stepVanilla(count, pos, range);
  //#endif
  }

  @Override
  public void end()
  {
    //#if MC >= 11800
    //$$ levelTicks.cleanupAfterTick();
    //#else
    if(SubTick.hasLithium)
      endLithium();
    else
      endVanilla();
    //#endif
  }

  //#if MC < 11800
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

    queue.clear();
    for(TickNextTickData<T> tick : tickList.currentlyTicking)
      queue.add(new QueueElement(tick));
  }

  // Accessor cast warnings
  @SuppressWarnings("unchecked")
  private void startLithium()
  {
    ((LithiumServerTickScheduler<T>)tickList).selectTicks(level.getGameTime());
    lithium_scheduled_tick_step_index = 0;

    queue.clear();
    for(TickEntry<T> tick : ((LithiumServerTickSchedulerAccessor<T>)(LithiumServerTickScheduler<T>)tickList).getExecutingTicks())
      queue.add(new QueueElement(tick));
  }

  public Triple<Integer, Integer, Boolean> stepVanilla(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    int success_steps = 0;
    while(success_steps < count && !tickList.currentlyTicking.isEmpty())
    {
      TickNextTickData<T> tick = tickList.currentlyTicking.poll();

      if(level.isPositionTickingWithEntitiesLoaded(tick.pos))
      {
        tickList.alreadyTicked.add(tick);
        tickList.ticker.accept(tick);
      }
      else
        tickList.scheduleTick(tick.pos, tick.getType(), 0);

      if(currentMode == INDEX)
      {
        if(rangeCheck(tick.pos, pos, range))
          success_steps ++;
      }
      else
      {
        TickNextTickData<T> nextTick = tickList.currentlyTicking.peek();
        if(nextTick == null || nextTick.priority != tick.priority)
          success_steps ++;
      }
      executed_steps ++;
    }
    return Triple.of(executed_steps, success_steps, exhausted = tickList.currentlyTicking.isEmpty());
  }

  // Accessor cast warnings
  @SuppressWarnings("unchecked")
  private Triple<Integer, Integer, Boolean> stepLithium(int count, BlockPos pos, int range)
  {
    LithiumServerTickSchedulerAccessor<T> scheduler = (LithiumServerTickSchedulerAccessor<T>)(LithiumServerTickScheduler<T>)tickList;
    int executed_steps = 0;
    int success_steps = 0;
    ArrayList<TickEntry<T>> ticks = scheduler.getExecutingTicks();
    int ticksSize = ticks.size();
    while(lithium_scheduled_tick_step_index < ticksSize && success_steps < count)
    {
      TickEntry<T> tick = ticks.get(lithium_scheduled_tick_step_index);
      if(tick == null)
        continue;
      tick.consumed = true;
      scheduler.getTickConsumer().accept(tick);
      lithium_scheduled_tick_step_index ++;
      if(currentMode == INDEX)
      {
        if(rangeCheck(tick.pos, pos, range))
          success_steps ++;
      }
      else if(lithium_scheduled_tick_step_index == ticksSize || ticks.get(lithium_scheduled_tick_step_index).priority != tick.priority)
          success_steps ++;
      executed_steps ++;
    }
    return Triple.of(executed_steps, success_steps, exhausted = lithium_scheduled_tick_step_index == ticksSize);
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
  //#endif
}
