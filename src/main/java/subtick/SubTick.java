package subtick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ModInitializer;

import subtick.commands.SubTickCommand;
import subtick.commands.QueueCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public class SubTick implements CarpetExtension, ModInitializer
{
  @Override
  public void onInitialize()
  {
    CarpetServer.manageExtension(new SubTick());
  }

  @Override
  public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
    SubTickCommand.register(dispatcher);
    QueueCommand.register(dispatcher);
  }
}
