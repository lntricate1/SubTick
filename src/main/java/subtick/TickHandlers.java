package subtick;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class TickHandlers
{
  private static Map<ResourceKey<Level>, TickHandler> tickHandlers = new HashMap<ResourceKey<Level>, TickHandler>();

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

  public static String t(String str)
  {
    return Settings.subtickTextFormat + " " + str;
  }

  public static String n(String str)
  {
    return Settings.subtickNumberFormat + " " + str;
  }

  public static String n(int x)
  {
    return Settings.subtickNumberFormat + " " + x;
  }

  public static String err(String str)
  {
    return Settings.subtickErrorFormat + " " + str;
  }

  public static void addLevel(ResourceKey<Level> key, ServerLevel level)
  {
    tickHandlers.put(key, new TickHandler(key.location().getPath(), level));
  }

  public static TickHandler getHandler(ResourceKey<Level> key)
  {
    return tickHandlers.get(key);
  }
}
