package subtick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import subtick.commands.TickCommand;
import subtick.commands.PhaseCommand;
import subtick.commands.QueueCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public class SubTick implements CarpetExtension, ModInitializer
{
  public static final String MOD_ID = "subtick";
  public static String MOD_NAME = "";
  public static String MOD_VERSION = "";
  @Override
  public void onInitialize()
  {
    CarpetServer.manageExtension(new SubTick());
  }

  @Override
  public void onGameStarted()
  {
    ModMetadata metadata = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(RuntimeException::new).getMetadata();
    MOD_NAME = metadata.getName();
    MOD_VERSION = metadata.getVersion().getFriendlyString();
    CarpetServer.settingsManager.parseSettingsClass(Settings.class);
  }

  @Override
  public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
    TickCommand.register(dispatcher);
    PhaseCommand.register(dispatcher);
    QueueCommand.register(dispatcher);
  }
}
