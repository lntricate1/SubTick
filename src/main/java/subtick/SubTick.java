package subtick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.Messenger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import subtick.commands.TickCommand;
import subtick.interfaces.ILevel;
import subtick.queues.AbstractQueue;
import subtick.commands.PhaseCommand;
import subtick.commands.QueueCommand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

public class SubTick implements CarpetExtension, ModInitializer
{
  public static final String MOD_ID = "subtick";
  public static String MOD_NAME = "";
  public static String MOD_VERSION = "";
  public static final boolean hasLithium = FabricLoader.getInstance().isModLoaded("lithium");

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
  public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
    TickCommand.register(dispatcher);
    PhaseCommand.register(dispatcher);
    QueueCommand.register(dispatcher);
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

  public static String p(TickPhase phase)
  {
    return Settings.subtickPhaseFormat + " " + phase.getName();
  }

  public static String p(AbstractQueue queue, int count)
  {
    return Settings.subtickPhaseFormat + " " + queue.getName(count);
  }

  public static BaseComponent d(Level level)
  {
    ResourceLocation location = level.dimension().location();
    return Messenger.c(
      Settings.subtickDimensionFormat + " " + location.getPath().substring(0, 1).toUpperCase() + location.getPath().substring(1),
      "^" + Settings.subtickDimensionFormat + " " + location.toString());
  }

  public static String err(String str)
  {
    return Settings.subtickErrorFormat + " " + str;
  }

  // The order can be different in different versions of the game...
  public static TickPhase[] getTickPhaseOrder()
  {
    return new TickPhase[]
    {
      TickPhase.WORLD_BORDER,
      TickPhase.WEATHER,
      TickPhase.TIME,
      TickPhase.BLOCK_TICK,
      TickPhase.FLUID_TICK,
      TickPhase.RAID,
      TickPhase.CHUNK,
      TickPhase.BLOCK_EVENT,
      TickPhase.ENTITY,
      TickPhase.BLOCK_ENTITY,
      TickPhase.ENTITY_MANAGEMENT
    };
  }

  public static TickHandler getTickHandler(CommandContext<CommandSourceStack> c)
  {
    return getTickHandler(c.getSource().getLevel());
  }

  public static TickHandler getTickHandler(Level level)
  {
    return ((ILevel)level).getTickHandler();
  }
}
