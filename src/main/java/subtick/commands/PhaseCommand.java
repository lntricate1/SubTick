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

import subtick.TickHandlers;
import subtick.TickHandler;

public class PhaseCommand
{
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
  {
    dispatcher.register(
      literal("phaseStep")
      .then(argument("phase", word())
        .suggests((c, b) -> suggestMatching(TickHandlers.tickPhaseArgumentNames, b))
        .then(literal("force")
          .executes((c) -> stepToPhase(c, TickHandlers.getPhase(getString(c, "phase")), true))
        )
        .executes((c) -> stepToPhase(c, TickHandlers.getPhase(getString(c, "phase")), false))
      )
      .then(argument("count", integer(1))
        .executes((c) -> stepCount(c, getInteger(c, "count")))
      )
      .executes((c) -> stepCount(c, 1))
    );
  }

  private static int stepCount(CommandContext<ServerCommandSource> c, int count)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    int phase = handler.current_phase + count;
    int ticks = phase / TickHandlers.TOTAL_PHASES;
    phase %= TickHandlers.TOTAL_PHASES;
    return TickCommand.step(c, ticks, phase);
  }

  private static int stepToPhase(CommandContext<ServerCommandSource> c, int phase, boolean force)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(phase <= handler.current_phase && force)
      return TickCommand.step(c, 1, phase);
    else
      return TickCommand.step(c, 0, phase);
  }
}
