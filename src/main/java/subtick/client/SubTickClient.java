package subtick.client;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import subtick.SubTick;

public class SubTickClient implements ClientModInitializer, IInitializationHandler
{
  @Override
  public void onInitializeClient()
  {
    InitializationHandler.getInstance().registerInitializationHandler(this);
  }

  @Override
  public void registerModHandlers()
  {
    ConfigManager.getInstance().registerConfigHandler(SubTick.MOD_ID, new Configs());
  }
}
