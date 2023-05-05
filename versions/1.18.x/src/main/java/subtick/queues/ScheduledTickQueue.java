package subtick.queues;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.apache.commons.lang3.tuple.Pair;
import subtick.TickPhase;
import subtick.TickingMode;

public class ScheduledTickQueue<T> extends TickingQueue
{
  private final TickingMode INDEX;
  private final TickingMode PRIORITY;

  private final LevelTicks<T> levelTicks;
  private final BiConsumer<BlockPos, T> biConsumer;

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
    levelTicks.collectTicks(level.getGameTime(), 65536, levelTicks.profiler.get());
  }

  @Override
  public Pair<Integer, Boolean> step(int count, BlockPos pos, int range)
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
    return Pair.of(executed_steps, exhausted = levelTicks.toRunThisTick.isEmpty());
  }

  @Override
  public void end()
  {
    levelTicks.cleanupAfterTick();
  }
}
