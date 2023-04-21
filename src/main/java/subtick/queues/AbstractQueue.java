package subtick.queues;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import oshi.util.tuples.Pair;
import subtick.TickPhase;
import subtick.network.ServerNetworkHandler;

public abstract class AbstractQueue
{
  private List<AABB> block_highlights = new ArrayList<>();
  private List<Integer> entity_highlights = new ArrayList<>();
  private final TickPhase phase;
  private final String commandKey, nameSingle, nameMultiple;
  public boolean exhausted;

  public AbstractQueue(TickPhase phase, String commandKey, String nameSingle, String nameMultiple)
  {
    this.phase = phase;
    this.commandKey = commandKey;
    this.nameSingle = nameSingle;
    this.nameMultiple = nameMultiple;
  }

  @Override
  public String toString() {
    return commandKey;
  }

  public TickPhase getPhase()
  {
    return phase;
  }

  public String getCommandKey()
  {
    return commandKey;
  }

  public String getName(int count)
  {
    return count == 1 ? nameSingle : nameMultiple;
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

  public void addBlockOutline(BlockPos pos, ServerLevel level)
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

  public void sendHighlights(ServerLevel level, CommandSourceStack actor)
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

  public void clearHighlights(ServerLevel level)
  {
    ServerNetworkHandler.clearBlockHighlights(level);
    ServerNetworkHandler.clearEntityHighlights(level);
  }

  public boolean cantStep(ServerLevel level)
  {
    return exhausted;
  }

  public abstract void start(ServerLevel level);
  public abstract Pair<Integer, Boolean> step(int count, ServerLevel level, BlockPos pos, int range);
  public abstract void end(ServerLevel level);
}
