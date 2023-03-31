package subtick;

import java.util.Map;
import java.util.HashMap;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

public class TickHandlers
{
  private static Map<RegistryKey<World>, TickHandler> tickHandlers = new HashMap<RegistryKey<World>, TickHandler>();

  public static final int
    WORLD_BORDER = 0,
    WEATHER = 1,
    TIME = 2,
    TILE_TICK = 3,
    FLUID_TICK = 4,
    RAID = 5,
    CHUNK = 6,
    BLOCK_EVENT = 7,
    ENTITY = 8,
    BLOCK_ENTITY = 9,
    ENTITY_MANAGEMENT = 10,
    TOTAL_PHASES = 11;

  public static final String[] tickPhaseNames = new String[]
  {
    "World Border",
    "Weather",
    "Time Increment",
    "Tile Tick",
    "Fluid Tick",
    "Raid",
    "Chunk",
    "Block Event",
    "Entity",
    "Block Entity",
    "Entity Management"
  };

  public static final String[] tickPhaseNamesPlural = new String[]
  {
    "",
    "",
    "",
    "Tile Ticks",
    "Fluid Ticks",
    "",
    "",
    "Block Events",
    "Entities",
    "Block Entities",
    ""
  };

  public static final String[] tickPhaseArgumentNames = new String[]
  {
    "worldBorder",
    "weather",
    "time",
    "tileTick",
    "fluidTick",
    "raid",
    "chunk",
    "blockEvent",
    "entity",
    "blockEntity",
    "entityManagement"
  };

  public static int getPhase(String str)
  {
    for(int i = 0; i < tickPhaseArgumentNames.length; i++)
      if(str.equals(tickPhaseArgumentNames[i])) return i;

    return -1;
  }

  public static String getPhase(int phase)
  {
    return Settings.subtickPhaseFormat + " " + tickPhaseNames[phase];
  }

  public static String getPhase(int phase, int count)
  {
    return Settings.subtickPhaseFormat + " " + (count == 1 ? tickPhaseNames : tickPhaseNamesPlural)[phase];
  }

  public static void addWorld(RegistryKey<World> key, ServerWorld world)
  {
    tickHandlers.put(key, new TickHandler(key.getValue().getPath(), world));
  }

  public static TickHandler getHandler(RegistryKey<World> key)
  {
    return tickHandlers.get(key);
  }
}
