package subtick.queues;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
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
  {}

  @Override
  public Pair<Integer, Boolean> step(int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    while(executed_steps < count && level.blockEvents.size() != 0)
    {
      int size = currentMode == INDEX ? 1 : level.blockEvents.size();
      boolean stepped = false;
      for(int i = 0; i < size; i ++)
      {
        BlockEventData blockEvent = level.blockEvents.removeFirst();
        if(level.doBlockEvent(blockEvent))
        {
          level.server.getPlayerList().broadcast(null, blockEvent.getPos().getX(), blockEvent.getPos().getY(), blockEvent.getPos().getZ(), 64.0D, level.dimension(), new ClientboundBlockEventPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getParamA(), blockEvent.getParamB()));

          if(rangeCheck(blockEvent.getPos(), pos, range))
          {
            addBlockOutline(blockEvent.getPos());
            stepped = true;
          }
        }
      }
      if(stepped)
        executed_steps ++;
    }
    return new Pair<Integer, Boolean>(executed_steps, exhausted = level.blockEvents.size() == 0);
  }

  @Override
  public void end()
  {}

  @Override
  public boolean cantStep()
  {
    return level.blockEvents.size() == 0;
  }
}
