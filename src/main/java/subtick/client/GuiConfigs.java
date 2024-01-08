package subtick.client;

import java.util.List;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import subtick.SubTick;

public class GuiConfigs extends GuiConfigsBase
{
  public GuiConfigs()
  {
    super(10, 50, SubTick.MOD_ID, null, "subtick.gui.title.configs");
  }

  @Override
  public List<ConfigOptionWrapper> getConfigs()
  {
    return ConfigOptionWrapper.createFor(Configs.OPTIONS);
  }
}
