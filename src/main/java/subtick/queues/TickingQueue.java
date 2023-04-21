package subtick.queues;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import oshi.util.tuples.Pair;
import subtick.TickHandler;
import subtick.TickingMode;
import subtick.network.ServerNetworkHandler;

public abstract class TickingQueue
{
  private List<AABB> block_highlights = new ArrayList<>();
  private List<Integer> entity_highlights = new ArrayList<>();
  protected final ServerLevel level;
  protected final TickHandler handler;
  public boolean exhausted;

  public TickingQueue(TickHandler handler)
  {
    this.level = handler.level;
    this.handler = handler;
  }

  public static boolean rangeCheck(BlockPos a, BlockPos b, long range)
  {
    if(range == -2) return false;
    if(range == -1) return true;

    long x = a.getX() - b.getX();
    long y = a.getY() - b.getY();
    long z = a.getZ() - b.getZ();
    return x*x + y*y + z*z <= range*range;
  }

  public void addBlockOutline(BlockPos pos)
  {
    List<AABB> outlineAabbs = level.getBlockState(pos).getShape(level, pos).toAabbs();
    if(outlineAabbs.isEmpty())
      block_highlights.add(new AABB(pos));
    else
    {
      outlineAabbs.replaceAll((box) -> box.move(pos));
      block_highlights.addAll(outlineAabbs);
    }
  }

  public void addBlockHighlight(AABB aabb)
  {
    block_highlights.add(aabb);
  }

  public void addEntityHighlight(int id)
  {
    entity_highlights.add(id);
  }

  public List<AABB> getBlockHighlights()
  {
    return block_highlights;
  }

  public List<Integer> getEntityHighlights()
  {
    return entity_highlights;
  }

  public void sendHighlights(CommandSourceStack actor)
  {
    if(!block_highlights.isEmpty())
      ServerNetworkHandler.sendBlockHighlights(block_highlights, level, actor);
    if(!entity_highlights.isEmpty())
      ServerNetworkHandler.sendEntityHighlights(entity_highlights, level, actor);
  }

  public void emptyHighlights()
  {
    block_highlights.clear();
    entity_highlights.clear();
  }

  public void clearHighlights()
  {
    ServerNetworkHandler.clearBlockHighlights(level);
    ServerNetworkHandler.clearEntityHighlights(level);
  }

  public boolean cantStep(TickingMode mode)
  {
    return exhausted;
  }

  public abstract void start(TickingMode mode);
  public abstract Pair<Integer, Boolean> step(TickingMode mode, int count, BlockPos pos, int range);
  public abstract void end(TickingMode mode);
  public abstract String getName(TickingMode mode, int steps);
}
