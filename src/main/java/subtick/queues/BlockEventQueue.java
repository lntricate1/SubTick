package subtick.queues;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.level.BlockEventData;
import oshi.util.tuples.Pair;
import subtick.TickHandler;
import subtick.TickingMode;

public class BlockEventQueue extends TickingQueue
{
  public static final TickingMode INDEX = new TickingMode("index", "Index");
  public static final TickingMode DEPTH = new TickingMode("depth", "Depth");

  public BlockEventQueue(TickHandler handler)
  {
    super(handler);
  }

  @Override
  public void start(TickingMode mode)
  {}

  @Override
  public Pair<Integer, Boolean> step(TickingMode mode, int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    int batchSize = 0;
    int batchIndex = 0;
    while(executed_steps < count && level.blockEvents.size() != 0)
    {
      if (batchIndex++ == batchSize)
      {
        batchSize = level.blockEvents.size();
        batchIndex = 1;
      }
      BlockEventData blockEvent = level.blockEvents.removeFirst();
      if(level.doBlockEvent(blockEvent))
      {
        level.server.getPlayerList().broadcast(null, blockEvent.getPos().getX(), blockEvent.getPos().getY(), blockEvent.getPos().getZ(), 64.0D, level.dimension(), new ClientboundBlockEventPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getParamA(), blockEvent.getParamB()));

        if(rangeCheck(blockEvent.getPos(), pos, range))
        {
          addBlockOutline(blockEvent.getPos());
          if (mode == INDEX || batchIndex == batchSize)
            executed_steps ++;
        }
      }
    }
    return new Pair<Integer, Boolean>(executed_steps, exhausted = level.blockEvents.size() == 0);
  }

  @Override
  public void end(TickingMode mode)
  {}

  @Override
  public boolean cantStep(TickingMode mode)
  {
    return level.blockEvents.size() == 0;
  }

  @Override
  public String getName(TickingMode mode, int steps)
  {
    if (mode == INDEX)
    {
      return steps == 1 ? "block event" : "block events";
    }
    else
    {
      return steps == 1 ? "block event batch" : "block event batches";
    }
  }
}
