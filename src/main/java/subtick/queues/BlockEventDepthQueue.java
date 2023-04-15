package subtick.queues;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import oshi.util.tuples.Pair;
import subtick.TickPhase;

public class BlockEventDepthQueue extends AbstractQueue
{
  public BlockEventDepthQueue()
  {
    super(TickPhase.BLOCK_EVENT, "blockEventDepth", "Block Event Depth", "Block Event Depths");
  }

  @Override
  public void start(ServerLevel level)
  {}

  // @Override
  // public Pair<Integer, Boolean> step(int count, ServerLevel level, BlockPos pos, int range)
  // {
  //   BlockEventQueue queue = new BlockEventQueue();
  //   int executed_steps = 0;
  //   while(executed_steps < count && level.blockEvents.size() != 0)
  //   {
  //     Pair<Integer, Boolean> pair = queue.step(level.blockEvents.size(), level, pos, range);
  //     if(pair.getA() > 0)
  //       executed_steps ++;
  //   }
  //   return new Pair<Integer, Boolean>(executed_steps, exhausted = level.blockEvents.size() == 0);
  // }

  @Override
  public Pair<Integer, Boolean> step(int count, ServerLevel level, BlockPos pos, int range)
  {
    int executed_steps = 0;
    while(executed_steps < count && level.blockEvents.size() != 0)
    {
      int size = level.blockEvents.size();
      boolean stepped = false;
      for(int i = 0; i < size; i ++)
      {
        BlockEventData blockEvent = level.blockEvents.removeFirst();
        if(level.doBlockEvent(blockEvent))
        {
          level.server.getPlayerList().broadcast(null, blockEvent.getPos().getX(), blockEvent.getPos().getY(), blockEvent.getPos().getZ(), 64.0D, level.dimension(), new ClientboundBlockEventPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getParamA(), blockEvent.getParamB()));

          if(rangeCheck(blockEvent.getPos(), pos, range))
          {
            addBlockOutline(blockEvent.getPos(), level);
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
  public void end(ServerLevel level)
  {}
}
