package subtick.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import subtick.TickHandlers;
import subtick.TickHandler;

public class PhaseCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("phaseStep")
      .then(argument("phase", word())
        .suggests((c, b) -> suggest(TickHandlers.tickPhaseArgumentNames, b))
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

  private static int stepCount(CommandContext<CommandSourceStack> c, int count)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getLevel().dimension());
    int phase = handler.current_phase + count;
    int ticks = phase / TickHandlers.TOTAL_PHASES;
    phase %= TickHandlers.TOTAL_PHASES;
    return TickCommand.step(c, ticks, phase);
  }

  private static int stepToPhase(CommandContext<CommandSourceStack> c, int phase, boolean force)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getLevel().dimension());
    if(phase <= handler.current_phase && force)
      return TickCommand.step(c, 1, phase);
    else
      return TickCommand.step(c, 0, phase);
  }
}
