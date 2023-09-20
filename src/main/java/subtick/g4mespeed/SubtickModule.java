package subtick.g4mespeed;

import com.g4mesoft.core.GSIModule;
import com.g4mesoft.core.GSIModuleManager;
import com.g4mesoft.module.tps.GSTpsModule;

public class SubtickModule implements GSIModule
{
  public static final String KEY_CATEGORY = "subtick";

  @Override public void init(GSIModuleManager manager)
  {
    manager.runOnServer(serverManager ->
    {
      serverManager.getModule(GSTpsModule.class).sPrettySand.set(GSTpsModule.PRETTY_SAND_DISABLED);
      serverManager.getModule(GSTpsModule.class).sPrettySand.setEnabledInGui(false);
    });
  }
}
