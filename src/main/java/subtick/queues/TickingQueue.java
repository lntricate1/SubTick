package subtick.queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;
import subtick.TickPhase;
import subtick.TickingMode;
import subtick.network.ServerNetworkHandler;

public abstract class TickingQueue
{
  public static final DynamicCommandExceptionType INVALID_MODE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid mode '" + key + "'"));
  private List<AABB> block_highlights = new ArrayList<>();
  private List<Integer> entity_highlights = new ArrayList<>();
  public boolean exhausted;

  protected final ServerLevel level;
  protected final TickPhase phase;
  protected final String commandKey;
  protected final Map<String, TickingMode> modes;
  protected final TickingMode defaultMode;
  protected TickingMode currentMode;

  public TickingQueue(ServerLevel level, TickPhase phase, String commandKey, String nameSingle, String nameMultiple)
  {
    this(new HashMap<>(), new TickingMode(nameSingle, nameMultiple), level, phase, commandKey);
  }

  public TickingQueue(Map<String, TickingMode> modes, TickingMode defaultMode, ServerLevel level, TickPhase phase, String commandKey)
  {
    this.modes = modes;
    this.defaultMode = defaultMode;
    this.level = level;
    this.phase = phase;
    this.commandKey = commandKey;
  }

  public Set<String> getModes()
  {
    return modes.keySet();
  }

  public void setMode(String key) throws CommandSyntaxException
  {
    TickingMode newMode = key.equals("") ? defaultMode : modes.get(key);
    if(newMode == null)
      throw INVALID_MODE_EXCEPTION.create(key);
    currentMode = newMode;
  }

  public TickPhase getPhase()
  {
    return phase;
  }

  @Deprecated
  public String getName(int count)
  {
    return currentMode.getName(count);
  }

  public String getName()
  {
    return currentMode.getName();
  }

  public String getNamePlural()
  {
    return currentMode.getNamePlural();
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

  public boolean cantStep()
  {
    return exhausted;
  }

  public abstract void start();
  public abstract Pair<Integer, Boolean> step(int count, BlockPos pos, int range);
  public abstract void end();
}
