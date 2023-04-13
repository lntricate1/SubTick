package subtick;

import carpet.settings.Rule;

public class Settings
{
  @Rule(
    desc = "The default tick phase to freeze at and step to",
    options = {"worldBorder", "weather", "time", "blockTick", "fluidTick", "raid", "chunk", "blockEvent", "entity", "blockEntity", "entityManagement"},
    strict = true,
    category = "subtick"
  )
  public static String subtickDefaultPhase = "blockTick";

  @Rule(
    desc = "Text format for normal text in subtick message feedback (Uses carpet format, search for \"format(components, ...)\" in https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md)",
    category = "subtick"
  )
  public static String subtickTextFormat = "ig";

  @Rule(
    desc = "Text format for numbers in subtick message feedback (Uses carpet format, search for \"format(components, ...)\" in https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md)",
    category = "subtick"
  )
  public static String subtickNumberFormat = "iy";

  @Rule(
    desc = "Text format for phases in subtick message feedback (Uses carpet format, search for \"format(components, ...)\" in https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md)",
    category = "subtick"
  )
  public static String subtickPhaseFormat = "it";

  @Rule(
    desc = "Text format for ticking modes in subtick message feedback (Uses carpet format, search for \"format(components, ...)\" in https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md)",
    category = "subtick"
    )
  public static String subtickModeFormat = "it";

  @Rule(
    desc = "Text format for queue steps in subtick message feedback (Uses carpet format, search for \"format(components, ...)\" in https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md)",
    category = "subtick"
    )
  public static String subtickStepFormat = "it";

  @Rule(
    desc = "Text format for dimensions in subtick message feedback (Uses carpet format, search for \"format(components, ...)\" in https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md)",
    category = "subtick"
  )
  public static String subtickDimensionFormat = "im";

  @Rule(
    desc = "Error text format for normal text in subtick message feedback (Uses carpet format, search for \"format(components, ...)\" in https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md)",
    category = "subtick"
  )
  public static String subtickErrorFormat = "ir";

  @Rule(
    desc = "Color for subtick queueStep highlighting",
    category = "subtick"
  )
  public static int subtickHighlightColor = 0x00FF00;

  @Rule(
    desc = "Default range for queueStep",
    category = "subtick"
  )
  public static int subtickDefaultRange = 32;
}
