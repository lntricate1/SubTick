package subtick.queues;

import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.apache.commons.lang3.tuple.Triple;

import subtick.QueueElement;
import subtick.TickPhase;

public class BlockEntityQueue extends TickingQueue
{
  private Iterator<TickingBlockEntity> block_entity_iterator;

  public BlockEntityQueue()
  {
    super(TickPhase.BLOCK_ENTITY, "blockEntity", "Block Entity", "Block Entities");
  }

  @Override
  public void start(ServerLevel level)
  {
    this.level = level;
    level.tickingBlockEntities = true;
    if(!level.pendingBlockEntityTickers.isEmpty())
    {
      level.blockEntityTickers.addAll(level.pendingBlockEntityTickers);
      level.pendingBlockEntityTickers.clear();
    }
    block_entity_iterator = level.blockEntityTickers.iterator();

    queue.clear();
    for(TickingBlockEntity be : level.blockEntityTickers)
      queue.add(new QueueElement(be));
  }

  @Override
  public Triple<Integer, Integer, Boolean> step(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    int success_steps = 0;

    while(success_steps < count && block_entity_iterator.hasNext())
    {
      TickingBlockEntity ticker = block_entity_iterator.next();
      BlockPos tpos = ticker.getPos();
      if(tpos == null)
      {
        queue.remove(new QueueElement(ticker));
        continue;
      }
      else if(rangeCheck(tpos, pos, range))
        success_steps ++;
      executed_steps ++;

      if(ticker.isRemoved())
        block_entity_iterator.remove();
      else
        ticker.tick();
    }
    return Triple.of(executed_steps, success_steps, exhausted = !block_entity_iterator.hasNext());
  }

  @Override
  public void end()
  {
    level.tickingBlockEntities = false;
  }
}
