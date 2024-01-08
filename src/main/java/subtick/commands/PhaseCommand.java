package subtick.commands;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import subtick.TickHandler;
import subtick.TickPhase;

public class PhaseCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("phaseStep")
      .then(argument("count", integer(1))
        .executes((c) -> stepCount(c.getSource(), getInteger(c, "count")))
      )
      .then(argument("phase", word())
        .suggests((c, b) -> suggest(TickPhase.commandSuggestions, b))
        .then(literal("force")
          .executes((c) -> stepToPhase(c.getSource(), TickPhase.byCommandKey(getString(c, "phase")), true))
        )
        .executes((c) -> stepToPhase(c.getSource(), TickPhase.byCommandKey(getString(c, "phase")), false))
      )
      .executes((c) -> stepCount(c.getSource(), 1))
    );
  }

  private static int stepCount(CommandSourceStack c, int count)
  {
    int currentPhase = TickHandler.currentPhase().phase();
    int phase = currentPhase + count;
    int ticks = phase/TickPhase.totalPhases;
    return TickCommand.step(c, phase < currentPhase ? ticks + 1 : ticks, phase % TickPhase.totalPhases);
  }

  private static int stepToPhase(CommandSourceStack c, int phase, boolean force)
  {
    if(phase < TickHandler.currentPhase().phase() && force)
      return TickCommand.step(c, 1, phase);
    else
      return TickCommand.step(c, 0, phase);
  }
}
