package subtick.queues;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import oshi.util.tuples.Pair;
import subtick.TickHandler;
import subtick.TickingMode;
import subtick.network.ServerNetworkHandler;

public class BlockEntityQueue extends TickingQueue
{
  private Iterator<TickingBlockEntity> block_entity_iterator;
  public List<BlockPos> executed_poses = new ArrayList<BlockPos>();

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
        addBlockOutline(ticker.getPos());
        executed_poses.add(ticker.getPos());
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

  @Override
  public void sendHighlights(CommandSourceStack actor)
  {
    if(!getBlockHighlights().isEmpty())
      ServerNetworkHandler.sendBlockHighlights(getBlockHighlights(), level, actor);
    if(!executed_poses.isEmpty())
      ServerNetworkHandler.sendBlockEntityTicks(executed_poses, level, actor);
  }

  @Override
  public void emptyHighlights()
  {
    super.emptyHighlights();
    executed_poses.clear();
  }
}
