package subtick.queues;

import java.util.Collection;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import oshi.util.tuples.Pair;
import subtick.TickPhase;
import subtick.TickingMode;

public class BlockEventQueue extends TickingQueue
{
  private static TickingMode INDEX = new TickingMode("Block Event", "Block Events");
  private static TickingMode DEPTH = new TickingMode("Block Event Depth", "Block Event Depths");

  public BlockEventQueue(ServerLevel level)
  {
    super(Map.of("index", INDEX, "depth", DEPTH), INDEX, level, TickPhase.BLOCK_EVENT, "blockEvent");
  }

  @Override
  public void start()
  {
    level.blockEventsToReschedule.clear();
  }

  @Override
  public Pair<Integer, Boolean> step(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    while(executed_steps < count && !level.blockEvents.isEmpty())
    {
      int size = currentMode == INDEX ? 1 : level.blockEvents.size();
      boolean stepped = false;
      for(int i = 0; i < size; i ++)
      {
        BlockEventData blockEvent = level.blockEvents.removeFirst();
        if(level.shouldTickBlocksAt(ChunkPos.asLong(blockEvent.pos())))
        {
          if(!level.doBlockEvent(blockEvent))
            continue;

          level.server.getPlayerList().broadcast(null, blockEvent.pos().getX(), blockEvent.pos().getY(), blockEvent.pos().getZ(), 64.0D, level.dimension(), new ClientboundBlockEventPacket(blockEvent.pos(), blockEvent.block(), blockEvent.paramA(), blockEvent.paramB()));

          if(rangeCheck(blockEvent.pos(), pos, range))
          {
            addBlockOutline(blockEvent.pos());
            stepped = true;
          }
          continue;
        }
        level.blockEventsToReschedule.add(blockEvent);
      }
      if(stepped)
        executed_steps ++;
    }
    return new Pair<Integer, Boolean>(executed_steps, exhausted = level.blockEvents.size() == 0);
  }

  @Override
  public void end()
  {
    level.blockEvents.addAll((Collection<BlockEventData>)level.blockEventsToReschedule);
  }

  @Override
  public boolean cantStep()
  {
    return level.blockEvents.isEmpty();
  }
}
