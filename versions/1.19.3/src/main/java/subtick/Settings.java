package subtick;

import carpet.api.settings.Rule;

public class Settings
{
  @Rule(
    options = {"worldBorder", "weather", "time", "blockTick", "fluidTick", "raid", "chunk", "blockEvent", "entity", "blockEntity", "entityManagement"},
    strict = true,
    categories = "subtick"
  )
  public static String subtickDefaultPhase = "blockTick";

  @Rule(
    categories = "subtick"
  )
  public static String subtickTextFormat = "ig";

  @Rule(
    categories = "subtick"
  )
  public static String subtickNumberFormat = "iy";

  @Rule(
    categories = "subtick"
  )
  public static String subtickPhaseFormat = "it";

  @Rule(
    categories = "subtick"
  )
  public static String subtickDimensionFormat = "im";

  @Rule(
    categories = "subtick"
  )
  public static String subtickErrorFormat = "ir";

  @Rule(
    categories = "subtick"
  )
  public static int subtickHighlightColor = 0x00FF00;

  @Rule(
    categories = "subtick"
  )
  public static int subtickDefaultRange = 32;
}
