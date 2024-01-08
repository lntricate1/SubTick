package subtick.queues;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.lang3.tuple.Triple;

import subtick.QueueElement;
import subtick.TickingMode;
import subtick.network.ServerNetworkHandler;

public abstract class TickingQueue
{
  public static final DynamicCommandExceptionType INVALID_MODE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid mode '" + key + "'"));
  public static final DynamicCommandExceptionType INVALID_QUEUE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid queue '" + key + "'"));
  protected final ObjectLinkedOpenHashSet<QueueElement> queue = new ObjectLinkedOpenHashSet<>();
  public boolean exhausted;

  protected final int phase;
  protected final String commandKey;
  protected final Map<String, TickingMode> modes;
  protected final TickingMode defaultMode;
  protected TickingMode currentMode;
  protected ServerLevel level;

  public static final TickingQueue BLOCK_TICK = ScheduledTickQueue.block();
  public static final TickingQueue FLUID_TICK = ScheduledTickQueue.fluid();
  public static final TickingQueue BLOCK_EVENT = new BlockEventQueue();
  public static final TickingQueue ENTITY = new EntityQueue();
  public static final TickingQueue BLOCK_ENTITY = new BlockEntityQueue();

  private static final ImmutableMap<String, TickingQueue> BY_COMMAND_KEY = ImmutableMap.of(
    "blockTick", BLOCK_TICK,
    "fluidTick", FLUID_TICK,
    "blockEvent", BLOCK_EVENT,
    "entity", ENTITY,
    "blockEntity", BLOCK_ENTITY);

  public static String[] commandKeys = new String[]{
    BLOCK_TICK.commandKey, FLUID_TICK.commandKey, BLOCK_EVENT.commandKey, ENTITY.commandKey, BLOCK_ENTITY.commandKey};

  public TickingQueue(int phase, String commandKey, String nameSingle, String nameMultiple)
  {
    this(new HashMap<>(), new TickingMode(nameSingle, nameMultiple), phase, commandKey);
  }

  public TickingQueue(Map<String, TickingMode> modes, TickingMode defaultMode, int phase, String commandKey)
  {
    this.modes = modes;
    this.defaultMode = defaultMode;
    this.phase = phase;
    this.commandKey = commandKey;
  }

  public static TickingQueue byCommandKey(String commandKey) throws CommandSyntaxException
  {
    TickingQueue queue = BY_COMMAND_KEY.get(commandKey);
    if(queue == null)
      throw INVALID_QUEUE_EXCEPTION.create(commandKey);
    return queue;
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

  public int getPhase()
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

  protected static boolean rangeCheck(BlockPos a, BlockPos b, long range)
  {
    if(range == -2) return false;
    if(range == -1) return true;

    long x = a.getX() - b.getX();
    long y = a.getY() - b.getY();
    long z = a.getZ() - b.getZ();
    return x*x + y*y + z*z <= range*range;
  }

  public void sendQueues(CommandSourceStack actor, int count)
  {
    ServerNetworkHandler.sendQueueStep(queue, count, actor.getLevel(), actor);
  }

  public boolean cantStep()
  {
    return exhausted;
  }

  public void start(ServerLevel level)
  {
    this.level = level;
  }

  // Actual count, feedback, exhausted
  public abstract Triple<Integer, Integer, Boolean> step(int count, BlockPos pos, int range);
  public void end(){}
}
