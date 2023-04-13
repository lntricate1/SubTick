package subtick;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import subtick.queues.TickingQueue;
import subtick.queues.BlockEntityQueue;
import subtick.queues.BlockEventQueue;
import subtick.queues.EntityQueue;
import subtick.queues.ScheduledTickQueue;

public enum TickPhase
{
  UNKNOWN          (null              , null),
  WORLD_BORDER     ("worldBorder"     , "World Border"),
  WEATHER          ("weather"         , "Weather"),
  TIME             ("time"            , "Time"),
  BLOCK_TICK       ("blockTick"       , "Block Tick"       , ScheduledTickQueue::block),
  FLUID_TICK       ("fluidTick"       , "Fluid Tick"       , ScheduledTickQueue::fluid),
  RAID             ("raid"            , "Raid"),
  CHUNK            ("chunk"           , "Chunk"),
  BLOCK_EVENT      ("blockEvent"      , "Block Event"      , BlockEventQueue::new, BlockEventQueue.INDEX, BlockEventQueue.DEPTH),
  ENTITY           ("entity"          , "Entity"           , EntityQueue::new),
  BLOCK_ENTITY     ("blockEntity"     , "Block Entity"     , BlockEntityQueue::new),
  ENTITY_MANAGEMENT("entityManagement", "Entity Management");

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
  private final String name;
  private final Function<TickHandler, TickingQueue> queueFactory;
  private final TickingMode defaultMode;
  private final Map<String, TickingMode> modesByCommandKey;

  private int id = -1;

  private TickPhase(String commandKey, String name)
  {
    this(commandKey, name, null, new TickingMode[0]);
  }

  private TickPhase(String commandKey, String name, Function<TickHandler, TickingQueue> queueFactory)
  {
    this(commandKey, name, queueFactory, TickingMode.DEFAULT);
  }

  private TickPhase(String commandKey, String name, Function<TickHandler, TickingQueue> queueFactory, TickingMode... modes)
  {
    if (queueFactory == null)
    {
      if (modes.length > 0)
        throw new IllegalArgumentException("tick phase has ticking modes but no ticking queue!");
    }
    else
    {
      if (modes.length == 0)
        throw new IllegalArgumentException("tick phase has a ticking queue but no ticking modes!");
      for (TickingMode mode : modes)
        if (mode == null)
          throw new IllegalArgumentException("null is not a valid ticking mode!");
    }
    this.commandKey = commandKey;
    this.name = name;
    this.queueFactory = queueFactory;
    this.defaultMode = (modes.length > 0) ? modes[0] : null;
    this.modesByCommandKey = new HashMap<>();
    for (TickingMode mode : modes)
    {
      if (this.modesByCommandKey.containsKey(mode.getCommandKey()))
        throw new IllegalArgumentException(mode.getCommandKey() + " is not a unique ticking mode for tick phase " + this);
      this.modesByCommandKey.put(mode.getCommandKey(), mode);
    }
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
    return name;
  }

  public TickingQueue newQueue(TickHandler handler)
  {
    return queueFactory == null ? null : queueFactory.apply(handler);
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

  public TickingMode getDefaultMode()
  {
    return defaultMode;
  }

  public TickingMode getMode(String key) throws CommandSyntaxException
  {
    return modesByCommandKey.get(key);
  }

  public Collection<String> getModeCommandKeys()
  {
    return modesByCommandKey.keySet();
  }
}
