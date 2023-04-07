package subtick.commands;

import com.mojang.brigadier.Command;
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

import carpet.utils.Messenger;

import subtick.TickHandler;
import subtick.TickPhase;
import subtick.Settings;
import subtick.SubTick;

import static subtick.SubTick.t;
import static subtick.SubTick.n;
import static subtick.SubTick.p;
import static subtick.SubTick.d;
import static subtick.SubTick.err;

public class TickCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("tick")
      .then(literal("when")
        .executes((c) -> when(c))
      )
      .then(literal("freeze")
        .then(argument("phase", word())
          .suggests((c, b) -> suggest(TickPhase.getCommandKeys(), b))
          .executes((c) -> toggleFreeze(c, TickPhase.byCommandKey(getString(c, "phase"))))
        )
        .then(literal("on")
          .then(argument("phase", word())
            .suggests((c, b) -> suggest(TickPhase.getCommandKeys(), b))
            .executes((c) -> freeze(c, TickPhase.byCommandKey(getString(c, "phase"))))
          )
          .executes((c) -> freeze(c, TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
        )
        .then(literal("off")
          .executes((c) -> unFreeze(c))
        )
        .then(literal("status")
          .executes((c) -> when(c))
        )
        .then(literal("deep")
          .executes((c) -> {Messenger.m(c.getSource(), err("This feature doesn't do anything because SubTick is installed.")); return 1;})
        )
        .executes((c) -> toggleFreeze(c, TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
      )
      .then(literal("step")
        .then(argument("ticks", integer(0))
          .then(argument("phase", word())
            .suggests((c, b) -> suggest(TickPhase.getCommandKeys(), b))
            .executes((c) -> step(c, getInteger(c, "ticks"), TickPhase.byCommandKey(getString(c, "phase"))))
          )
          .executes((c) -> step(c, getInteger(c, "ticks"), TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
        )
        .executes((c) -> step(c, 1, TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
      )
    );
  }

  private static int when(CommandContext<CommandSourceStack> c)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    Messenger.m(c.getSource(), d(handler.level), t(" is " + (handler.frozen ? "frozen" : "unfrozen") + " in "), p(handler.current_phase), t(" phase"));
    return Command.SINGLE_SUCCESS;
  }

  private static int freeze(CommandContext<CommandSourceStack> c, TickPhase phase)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    if(handler.frozen || handler.freezing)
    {
      Messenger.m(c.getSource(), d(handler.level), err(" is already frozen"));
    }
    else
    {
      Messenger.m(c.getSource(), d(handler.level), t(" freezing at "), p(phase), t(" phase"));
      handler.freeze(phase);
    }
    return Command.SINGLE_SUCCESS;
  }

  private static int unFreeze(CommandContext<CommandSourceStack> c)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    if(handler.frozen || handler.freezing)
    {
      Messenger.m(c.getSource(), d(handler.level), t(" unfreezing"));
      handler.unfreeze();
      return 0;
    }
    Messenger.m(c.getSource(), d(handler.level), err(" is not frozen"));
    return Command.SINGLE_SUCCESS;
  }

  private static int toggleFreeze(CommandContext<CommandSourceStack> c, TickPhase phase)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    if(handler.frozen || handler.freezing)
    {
      unFreeze(c);
    }
    else
    {
      freeze(c, phase);
    }
    return Command.SINGLE_SUCCESS;
  }

  public static int step(CommandContext<CommandSourceStack> c, int ticks, TickPhase phase)
  {
    TickHandler handler = SubTick.getTickHandler(c);
    if(!handler.canStep(c, ticks, phase)) return 0;

    Messenger.m(c.getSource(), d(handler.level), t(" stepping "), n(ticks), t(" tick" + (ticks == 1 ? "" : "s") + ", ending at "), p(phase), t(" phase"));
    handler.step(ticks, phase);
    return Command.SINGLE_SUCCESS;
  }
}
