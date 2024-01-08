package subtick.queues;

//#if MC >= 11800
//$$ import java.util.Collection;
//$$ import net.minecraft.world.level.ChunkPos;
//#endif
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import org.apache.commons.lang3.tuple.Triple;

import subtick.QueueElement;
import subtick.TickPhase;
import subtick.TickingMode;
import subtick.network.ServerNetworkHandler;

public class BlockEventQueue extends TickingQueue
{
  private static final TickingMode INDEX = new TickingMode("Block Event", "Block Events");
  private static final TickingMode DEPTH = new TickingMode("Block Event Depth", "Block Event Depths");
  private int depth;
  private int remainingSteps;

  public BlockEventQueue()
  {
    super(Map.of("index", INDEX, "depth", DEPTH), INDEX, TickPhase.BLOCK_EVENT, "blockEvent");
  }

  public void updateQueue(ServerLevel level, BlockEventData be)
  {
    queue.add(new QueueElement(be, depth));
    ServerNetworkHandler.sendQueue(queue, level);
    exhausted = false;
  }

  @Override
  public void start(ServerLevel level)
  {
    this.level = level;
    //#if MC >= 11800
    //$$ level.blockEventsToReschedule.clear();
    //#endif
    queue.clear();
    for(BlockEventData be : level.blockEvents)
      queue.add(new QueueElement(be, 0));
    depth = 0;
    remainingSteps = 0;
  }

  @Override
  public Triple<Integer, Integer, Boolean> step(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    int success_steps = 0;
    while(success_steps < count && !level.blockEvents.isEmpty())
    {
      if(remainingSteps < 1)
      {
        depth ++;
        remainingSteps = level.blockEvents.size();
      }
      int size = currentMode == INDEX ? 1 : remainingSteps;
      remainingSteps -= size;
      boolean stepped = false;
      for(int i = 0; i < size; i ++)
      {
        BlockEventData blockEvent = level.blockEvents.removeFirst();
        //#if MC >= 11800
        //$$ if(level.shouldTickBlocksAt(ChunkPos.asLong(blockEvent.pos())))
        //$$ {
        //#endif
          if(!level.doBlockEvent(blockEvent))
          {
            queue.remove(new QueueElement(blockEvent, depth-1));
            continue;
          }

          //#if MC >= 11800
          //$$ level.server.getPlayerList().broadcast(null, blockEvent.pos().getX(), blockEvent.pos().getY(), blockEvent.pos().getZ(), 64.0D, level.dimension(), new ClientboundBlockEventPacket(blockEvent.pos(), blockEvent.block(), blockEvent.paramA(), blockEvent.paramB()));
          //#else
          level.server.getPlayerList().broadcast(null, blockEvent.getPos().getX(), blockEvent.getPos().getY(), blockEvent.getPos().getZ(), 64.0D, level.dimension(), new ClientboundBlockEventPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getParamA(), blockEvent.getParamB()));
          //#endif

          //#if MC >= 11800
          //$$ if(rangeCheck(blockEvent.pos(), pos, range))
          //#else
          if(rangeCheck(blockEvent.getPos(), pos, range))
          //#endif
            stepped = true;
          executed_steps ++;
        //#if MC >= 11800
        //$$ }
        //$$ level.blockEventsToReschedule.add(blockEvent);
        //#endif
      }
      if(stepped)
        success_steps ++;
    }
    return Triple.of(executed_steps, success_steps, exhausted = level.blockEvents.size() == 0);
  }

  // RSMM compat
  @Override
  public void end()
  {
    //#if MC >= 11800
    //$$ level.blockEvents.addAll((Collection<BlockEventData>)level.blockEventsToReschedule);
    //#endif
  }

  @Override
  public boolean cantStep()
  {
    return level.blockEvents.isEmpty();
  }
}
