package subtick;

import carpet.settings.Rule;

public class Settings
{
  @Rule(
    desc = "The default tick phase to freeze at and step to",
    options = {"worldBorder", "weather", "time", "tileTick", "fluidTick", "raid", "chunk", "blockEvent", "entity", "blockEntity", "entityManagement"},
    strict = true,
    category = "subtick"
  )
  public static String subtickDefaultPhase = "tileTick";

  @Rule(
    desc = "Text format for normal text in subtick message feedback",
    category = "subtick"
  )
  public static String subtickTextFormat = "ig";

  @Rule(
    desc = "Text format for numbers in subtick message feedback",
    category = "subtick"
  )
  public static String subtickNumberFormat = "iy";

  @Rule(
    desc = "Text format for phases in subtick message feedback",
    category = "subtick"
  )
  public static String subtickPhaseFormat = "it";

  @Rule(
    desc = "Text format for dimensions in subtick message feedback",
    category = "subtick"
  )
  public static String subtickDimensionFormat = "im";
}
