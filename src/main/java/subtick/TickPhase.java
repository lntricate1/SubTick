package subtick;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import subtick.util.Translations;

public record TickPhase(int dim, int phase)
{
  public static final DynamicCommandExceptionType INVALID_TICK_PHASE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid tick phase '" + key + "'"));
  public static TickPhase INVALID = new TickPhase(-1, -1);
  private static final List<String> dims = new ArrayList<>();

  // Tick phase order, reorder this if it changes across mc versions
  private static final List<String> commandKeys = List.of(
    "worldBorder",
    "weather",
    "time",
    "blockTick",
    "fluidTick",
    "raid",
    "chunk",
    "blockEvent",
    "entity",
    "blockEntity",
    "entityManagement");
  public static final String[] commandSuggestions = commandKeys.toArray(new String[]{});

  // This one doesn't need to be changed if you change the order above
  public static final int
    WORLD_BORDER = commandKeys.indexOf("worldBorder"),
    WEATHER = commandKeys.indexOf("weather"),
    TIME = commandKeys.indexOf("time"),
    BLOCK_TICK = commandKeys.indexOf("blockTick"),
    FLUID_TICK = commandKeys.indexOf("fluidTick"),
    RAID = commandKeys.indexOf("raid"),
    CHUNK = commandKeys.indexOf("chunk"),
    BLOCK_EVENT = commandKeys.indexOf("blockEvent"),
    ENTITY = commandKeys.indexOf("entity"),
    BLOCK_ENTITY = commandKeys.indexOf("blockEntity"),
    ENTITY_MANAGEMENT = commandKeys.indexOf("entityManagement");
  private static final int lastPhase = 10;
  public static final int totalPhases = 11;

  public static List<String> getDimensions()
  {
    return dims;
  }

  public TickPhase(ServerLevel level, int phase)
  {
    this(dims.indexOf(level.dimension().location().getPath()), phase);
  }

  public TickPhase(CompoundTag tag)
  {
    this(tag.getInt("dim"), tag.getInt("phase"));
  }

  /*
   * Gets the next tick phase, changing dimension as necessary
   */
  public TickPhase next(ServerLevel level)
  {
    if(phase == lastPhase)
      return new TickPhase(dim + 1 == dims.size() ? 0 : dim + 1, 0);

    if(phase == BLOCK_EVENT && dimensionUnloaded(level))
      return new TickPhase(dim, phase + 3);

    return new TickPhase(dim, phase == lastPhase ? 0 : phase + 1);
  }

  /*
   * Gets the next tick phase, but only in the current dimension
   */
  public TickPhase next(int i)
  {
    return new TickPhase(dim, (phase + i)%totalPhases);
  }

  public boolean isLast()
  {
    return phase == lastPhase && dim == dims.size() - 1;
  }

  public boolean isPriorTo(TickPhase phase2)
  {
    return dim < phase2.dim || phase < phase2.phase;
  }

  public String getPath()
  {
    return dims.get(dim);
  }

  public String getPhaseName()
  {
    return Translations.tr("subtick.tickPhase." + commandKeys.get(phase));
  }

  public static String getPhaseName(int phase)
  {
    return Translations.tr("subtick.tickPhase." + commandKeys.get(phase));
  }

  public static int byCommandKey(String key) throws CommandSyntaxException
  {
    int phase = commandKeys.indexOf(key);
    if(phase == -1)
      throw INVALID_TICK_PHASE_EXCEPTION.create(key);
    return phase;
  }

  public static void addDimension(ServerLevel level)
  {
    dims.add(level.dimension().location().getPath());
  }

  private boolean dimensionUnloaded(ServerLevel level)
  {
    return level.players().isEmpty() && level.getForcedChunks().isEmpty() && level.emptyTime >= 300;
  }
}
