package subtick.queues;

import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import oshi.util.tuples.Pair;
import subtick.TickHandler;
import subtick.TickingMode;

public class BlockEntityQueue extends TickingQueue
{
  private Iterator<TickingBlockEntity> block_entity_iterator;

  public BlockEntityQueue(TickHandler handler)
  {
    super(handler);
  }

  @Override
  public void start(TickingMode mode)
  {
    level.tickingBlockEntities = true;
    if(!level.pendingBlockEntityTickers.isEmpty())
    {
      level.blockEntityTickers.addAll(level.pendingBlockEntityTickers);
      level.pendingBlockEntityTickers.clear();
    }
    block_entity_iterator = level.blockEntityTickers.iterator();
  }

  @Override
  public Pair<Integer, Boolean> step(TickingMode mode, int count, BlockPos pos, int range)
  {
    int executed_steps = 0;

    while(executed_steps < count && block_entity_iterator.hasNext())
    {
      TickingBlockEntity ticker = block_entity_iterator.next();
      if(rangeCheck(ticker.getPos(), pos, range))
      {
        addBlockOutline(ticker.getPos(), level);
        executed_steps ++;
      }

      if(ticker.isRemoved())
        block_entity_iterator.remove();
      else
        ticker.tick();
    }
    return new Pair<Integer, Boolean>(executed_steps, exhausted = !block_entity_iterator.hasNext());
  }

  @Override
  public void end(TickingMode mode)
  {
    level.tickingBlockEntities = false;
  }

  @Override
  public String getName(TickingMode mode, int steps)
  {
    return steps == 1 ? "block entity" : "block entities";
  }
}
