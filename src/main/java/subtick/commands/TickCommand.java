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
// import subtick.Settings;
import subtick.SubTick;

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
          .suggests((c, b) -> suggest(TickPhase.getCommandKeys(), b))
          .executes((c) -> toggleFreeze(c.getSource(), TickPhase.byCommandKey(getString(c, "phase"))))
        )
        // Carpet parity
        .then(literal("on")
          .then(argument("phase", word())
            .suggests((c, b) -> suggest(TickPhase.getCommandKeys(), b))
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
            .suggests((c, b) -> suggest(TickPhase.getCommandKeys(), b))
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
    TickHandler handler = SubTick.getTickHandler(c);
    if(handler.frozen)
      Translations.m(c, "tickCommand.when.frozen", handler);
    else
      Translations.m(c, "tickCommand.when.unfrozen", handler);
    return Command.SINGLE_SUCCESS;
  }

  public static int freeze(CommandSourceStack c, TickPhase phase)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    if(handler.frozen || handler.freezing)
    {
      Translations.m(c, "tickCommand.freeze.err", handler.level);
      return 0;
    }
    else
    {
      handler.freeze(phase);
      Translations.m(c, "tickCommand.freeze.success", handler.level, phase);
      return Command.SINGLE_SUCCESS;
    }
  }

  public static int unfreeze(CommandSourceStack c)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    if(handler.frozen || handler.freezing)
    {
      handler.unfreeze();
      Translations.m(c, "tickCommand.unfreeze.success", handler.level);
      return Command.SINGLE_SUCCESS;
    }
    else
    {
      Translations.m(c, "tickCommand.unfreeze.err", handler.level);
      return 0;
    }
  }

  public static int toggleFreeze(CommandSourceStack c, TickPhase phase)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    if(handler.frozen || handler.freezing)
      return unfreeze(c);
    else
      return freeze(c, phase);
  }

  public static int step(CommandSourceStack c, int ticks, TickPhase phase)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    if(!handler.canStep(c, ticks, phase)) return 0;

    if(ticks == 1)
      Translations.m(c, "tickCommand.step.success.single", handler.level, phase, 1);
    else
      Translations.m(c, "tickCommand.step.success.multiple", handler.level, phase, ticks);
    handler.step(ticks, phase);
    return Command.SINGLE_SUCCESS;
  }
}
