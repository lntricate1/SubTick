package subtick.commands;

import com.mojang.brigadier.Command;
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
import subtick.util.Translations;

public class TickCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("tick")
      .then(literal("when")
        .executes((c) -> when(c.getSource()))
      )
      .then(literal("freeze")
        .then(argument("phase", word())
          .suggests((c, b) -> suggest(TickPhase.commandSuggestions, b))
          .executes((c) -> toggleFreeze(c.getSource(), TickPhase.byCommandKey(getString(c, "phase"))))
        )
        // Carpet parity
        .then(literal("on")
          .then(argument("phase", word())
            .suggests((c, b) -> suggest(TickPhase.commandSuggestions, b))
            .executes((c) -> freeze(c.getSource(), TickPhase.byCommandKey(getString(c, "phase"))))
          )
          // .executes((c) -> freeze(c.getSource(), TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
        )
        // Carpet parity
        // .then(literal("off")
        //   .executes((c) -> unfreeze(c.getSource()))
        // )
        // Carpet parity
        // .then(literal("status")
        //   .executes((c) -> when(c.getSource()))
        // )
        // .executes((c) -> toggleFreeze(c.getSource(), TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
      )
      .then(literal("step")
        .then(argument("ticks", integer(0))
          .then(argument("phase", word())
            .suggests((c, b) -> suggest(TickPhase.commandSuggestions, b))
            .executes((c) -> step(c.getSource(), getInteger(c, "ticks"), TickPhase.byCommandKey(getString(c, "phase"))))
          )
          // .executes((c) -> step(c.getSource(), getInteger(c, "ticks"), TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
        )
        // .executes((c) -> step(c.getSource(), 1, TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
      )
    );
  }

  public static int when(CommandSourceStack c)
  {
    if(TickHandler.frozen())
      Translations.m(c, "tickCommand.when.frozen", TickHandler.currentPhase());
    else
      Translations.m(c, "tickCommand.when.unfrozen", TickHandler.currentPhase());
    return Command.SINGLE_SUCCESS;
  }

  public static int freeze(CommandSourceStack c, int phase)
  {
    if(TickHandler.frozen() || TickHandler.freezing())
    {
      Translations.m(c, "tickCommand.freeze.err");
      return 0;
    }
    else
    {
      TickPhase tickPhase = new TickPhase(c.getLevel(), phase);
      TickHandler.scheduleFreeze(c.getLevel(), tickPhase);
      Translations.m(c, "tickCommand.freeze.success", tickPhase);
      return Command.SINGLE_SUCCESS;
    }
  }

  public static int unfreeze(CommandSourceStack c)
  {
    if(TickHandler.frozen() || TickHandler.freezing())
    {
      TickHandler.scheduleUnfreeze(c.getLevel());
      Translations.m(c, "tickCommand.unfreeze.success");
      return Command.SINGLE_SUCCESS;
    }
    else
    {
      Translations.m(c, "tickCommand.unfreeze.err");
      return 0;
    }
  }

  public static int toggleFreeze(CommandSourceStack c, int phase)
  {
    if(TickHandler.frozen() || TickHandler.freezing())
      return unfreeze(c);
    else
      return freeze(c, phase);
  }

  public static int step(CommandSourceStack c, int ticks, int phase)
  {
    TickPhase tickPhase = new TickPhase(c.getLevel(), phase);
    if(!TickHandler.canStep(c, ticks, tickPhase)) return 0;

    if(ticks == 1)
      Translations.m(c, "tickCommand.step.success.single", tickPhase, 1);
    else
      Translations.m(c, "tickCommand.step.success.multiple", tickPhase, ticks);
    TickHandler.scheduleStep(c, ticks, tickPhase);
    return Command.SINGLE_SUCCESS;
  }
}
