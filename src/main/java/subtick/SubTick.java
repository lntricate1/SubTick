package subtick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.CarpetSettings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.Level;
import subtick.commands.TickCommand;
import subtick.interfaces.ILevel;
import subtick.queues.BlockEntityQueue;
import subtick.queues.BlockEventQueue;
import subtick.queues.EntityQueue;
import subtick.queues.ScheduledTickQueue;
import subtick.util.Translations;
import subtick.commands.PhaseCommand;
import subtick.commands.QueueCommand;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC >= 11903
//$$ import net.minecraft.commands.CommandBuildContext;
//#endif
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

public class SubTick implements CarpetExtension, ModInitializer
{
  public static final Logger LOGGER = LogManager.getLogger();

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
    //#if MC < 11901
    Translations.update(CarpetSettings.language.equalsIgnoreCase("none") ? "en_us" : CarpetSettings.language);
    //#endif

    Queues.registerQueue("blockTick", ScheduledTickQueue::block);
    Queues.registerQueue("fluidTick", ScheduledTickQueue::fluid);
    Queues.registerQueue("blockEvent", BlockEventQueue::new);
    Queues.registerQueue("entity", EntityQueue::new);
    Queues.registerQueue("blockEntity", BlockEntityQueue::new);
  }

  @Override
  //#if MC >= 11903
  //$$ public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
  //#else
  public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
  //#endif
    TickCommand.register(dispatcher);
    PhaseCommand.register(dispatcher);
    QueueCommand.register(dispatcher);
  }

  @Override
  public Map<String, String> canHasTranslations(String lang)
  {
    return Translations.getTranslationFromResourcePath(lang);
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

  public static TickHandler getTickHandler(CommandSourceStack source)
  {
    return getTickHandler(source.getLevel());
  }

  public static TickHandler getTickHandler(Level level)
  {
    return ((ILevel)level).getTickHandler();
  }
}
