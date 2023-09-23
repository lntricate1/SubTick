package subtick;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

//#if MC >= 11901
//$$ import carpet.utils.Translations;
//#else
import subtick.util.Translations;
//#endif

public enum TickPhase
{
  UNKNOWN          (null),
  WORLD_BORDER     ("worldBorder"),
  WEATHER          ("weather"),
  TIME             ("time"),
  BLOCK_TICK       ("blockTick"),
  FLUID_TICK       ("fluidTick"),
  RAID             ("raid"),
  CHUNK            ("chunk"),
  BLOCK_EVENT      ("blockEvent"),
  ENTITY           ("entity"),
  BLOCK_ENTITY     ("blockEntity"),
  ENTITY_MANAGEMENT("entityManagement");

  private static final TickPhase[] BY_ID;
  private static final Map<String, TickPhase> BY_COMMAND_KEY;

  public static final DynamicCommandExceptionType INVALID_TICK_PHASE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid tick phase '" + key + "'"));

  static
  {
    TickPhase[] values = SubTick.getTickPhaseOrder();
    BY_ID = new TickPhase[values.length];
    BY_COMMAND_KEY = new HashMap<>();

    int id = 0;

    for(TickPhase phase : values)
    {
      if(phase == null || phase == UNKNOWN)
        throw new IllegalStateException("invalid tick phase " + phase + " provided in tick phase order!");
      if(phase.id != -1)
        throw new IllegalStateException("tick phase " + phase + " appears multiple times in tick phase order!");
      phase.id = id++;
      BY_ID[phase.id] = phase;
      BY_COMMAND_KEY.put(phase.commandKey, phase);
    }
  }

  private final String commandKey;

  private int id = -1;

  private TickPhase(String commandKey)
  {
    this.commandKey = commandKey;
  }

  @Override
  public String toString() {
    return commandKey == null ? "UNKNOWN" : commandKey + "[" + id + "]";
  }

  public int getId()
  {
    return id;
  }

  public String getCommandKey()
  {
    return commandKey;
  }

  public String getName()
  {
    return Translations.tr("subtick.tickPhase." + commandKey);
  }

  public boolean exists()
  {
    return id >= 0;
  }

  public boolean isFirst()
  {
    return id == 0;
  }

  public boolean isPriorTo(TickPhase other)
  {
    return id < other.id;
  }

  public boolean isPosteriorTo(TickPhase other)
  {
    return id > other.id;
  }

  public TickPhase next()
  {
    return next(1);
  }

  public TickPhase next(int count)
  {
    return BY_ID[(id + count) % BY_ID.length];
  }

  public static TickPhase byId(int id)
  {
    return id >= 0 && id < BY_ID.length ? BY_ID[id] : UNKNOWN;
  }

  public static TickPhase byCommandKey(String key) throws CommandSyntaxException
  {
    TickPhase phase = BY_COMMAND_KEY.get(key);
    if(phase == null || !phase.exists())
      throw INVALID_TICK_PHASE_EXCEPTION.create(key);
    return phase;
  }

  public static Collection<String> getCommandKeys()
  {
    return BY_COMMAND_KEY.keySet();
  }
}
