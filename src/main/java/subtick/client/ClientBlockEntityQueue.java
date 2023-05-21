package subtick.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

public class ClientBlockEntityQueue
{
  private static boolean stepping;
  private static HashSet<TickingBlockEntity> ticked_block_entities = new HashSet<>();

  private static void start(ClientLevel level)
  {
    if(!level.pendingBlockEntityTickers.isEmpty())
    {
      level.blockEntityTickers.addAll(level.pendingBlockEntityTickers);
      level.pendingBlockEntityTickers.clear();
    }
  }

  public static void step(ClientLevel level, List<BlockPos> poses)
  {
    if(!stepping)
    {
      start(level);
      stepping = true;
      level.tickingBlockEntities = true;
    }

    Iterator<TickingBlockEntity> iterator = level.blockEntityTickers.iterator();
    while(iterator.hasNext())
    {
      TickingBlockEntity be = iterator.next();
      if(be.getPos() == null)
        continue;
      for(BlockPos pos : poses)
        if(be.getPos().equals(pos))
        {
          if(be.isRemoved())
            iterator.remove();
          else
          {
            be.tick();
            ticked_block_entities.add(be);
          }
          return;
        }
    }
  }

  public static boolean end(ClientLevel level)
  {
    if(!stepping)
      return false;
    stepping = false;

    Iterator<TickingBlockEntity> iterator = level.blockEntityTickers.iterator();
    while(iterator.hasNext())
    {
      TickingBlockEntity be = iterator.next();
      if(!ticked_block_entities.contains(be))
      {
        if(be.isRemoved())
          iterator.remove();
        else
          be.tick();
      }
    }

    level.tickingBlockEntities = false;
    ticked_block_entities = new HashSet<>();
    return true;
  }
}
