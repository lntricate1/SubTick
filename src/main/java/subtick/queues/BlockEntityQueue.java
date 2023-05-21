package subtick.queues;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.apache.commons.lang3.tuple.Pair;
import subtick.TickPhase;
import subtick.network.ServerNetworkHandler;

public class BlockEntityQueue extends TickingQueue
{
  private Iterator<TickingBlockEntity> block_entity_iterator;
  public List<BlockPos> executed_poses = new ArrayList<BlockPos>();

  public BlockEntityQueue(ServerLevel level)
  {
    super(level, TickPhase.BLOCK_ENTITY, "blockEntity", "Block Entity", "Block Entities");
  }

  @Override
  public void start()
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
  public Pair<Integer, Boolean> step(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;

    while(executed_steps < count && block_entity_iterator.hasNext())
    {
      TickingBlockEntity ticker = block_entity_iterator.next();
      BlockPos tpos = ticker.getPos();
      if(tpos != null && rangeCheck(tpos, pos, range))
      {
        addBlockOutline(tpos);
        executed_poses.add(tpos);
        executed_steps ++;
      }

      if(ticker.isRemoved())
        block_entity_iterator.remove();
      else
        ticker.tick();
    }
    return Pair.of(executed_steps, exhausted = !block_entity_iterator.hasNext());
  }

  @Override
  public void end()
  {
    level.tickingBlockEntities = false;
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
