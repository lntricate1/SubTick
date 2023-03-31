package subtick.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.CommandSource.suggestMatching;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import net.minecraft.server.world.ServerWorld;

import carpet.utils.Messenger;

import subtick.TickHandlers;
import subtick.TickHandler;
import subtick.Settings;

public class QueueCommand
{
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
  {
    dispatcher.register(
      literal("queueStep")
      .then(argument("phase", word())
        .suggests((c, b) -> suggestMatching(new String[]{"blockEvent", "entity", "blockEntity"}, b))
        .then(argument("count", integer(1))
          .executes((c) -> step(c, TickHandlers.getPhase(getString(c, "phase")), getInteger(c, "count")))
        )
        .executes((c) -> step(c, TickHandlers.getPhase(getString(c, "phase")), 1))
      )
    );
  }

  private static String t(String str)
  {
    return Settings.subtickTextFormat + " " + str;
  }

  private static String n(String str)
  {
    return Settings.subtickNumberFormat + " " + str;
  }

  private static int step(CommandContext<ServerCommandSource> c, int phase, int count)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(!handler.canStep(c) || !handler.canQueueStep(c, phase)) return 1;

    Messenger.m(c.getSource(), t("Stepping dimension "), handler.getDimension(), n(" " + count + " "), TickHandlers.getPhase(phase, count));
    handler.scheduleQueueStep(phase, count);
    return 0;
  }
}
