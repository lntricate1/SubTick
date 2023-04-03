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

import carpet.utils.Messenger;

import subtick.TickHandlers;
import subtick.TickHandler;
import subtick.Settings;
import static subtick.TickHandlers.t;
import static subtick.TickHandlers.n;

public class SubTickCommand
{
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
  {
    dispatcher.register(
      literal("tick")
      .then(literal("when")
        .executes((c) -> when(c))
      )
      .then(literal("freeze")
        .then(argument("phase", word())
          .suggests((c, b) -> suggestMatching(TickHandlers.tickPhaseArgumentNames, b))
          .executes((c) -> toggleFreeze(c, TickHandlers.getPhase(getString(c, "phase"))))
        )
        .then(literal("on")
          .then(argument("phase", word())
            .suggests((c, b) -> suggestMatching(TickHandlers.tickPhaseArgumentNames, b))
            .executes((c) -> freeze(c, TickHandlers.getPhase(getString(c, "phase"))))
          )
          .executes((c) -> freeze(c, TickHandlers.getPhase(Settings.subtickDefaultPhase)))
        )
        .then(literal("off")
          .executes((c) -> unFreeze(c))
        )
        .then(literal("status")
          .executes((c) -> status(c))
        )
        .then(literal("deep")
          .executes((c) -> {Messenger.m(c.getSource(), t("This feature doesn't do anything because SubTick is installed.")); return 1;})
        )
        .executes((c) -> toggleFreeze(c, TickHandlers.getPhase(Settings.subtickDefaultPhase)))
      )
      .then(literal("step")
        .then(argument("ticks", integer(0))
          .then(argument("phase", word())
            .suggests((c, b) -> suggestMatching(TickHandlers.tickPhaseArgumentNames, b))
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.getPhase(getString(c, "phase"))))
          )
          .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.getPhase(Settings.subtickDefaultPhase)))
        )
        .executes((c) -> step(c, 1, TickHandlers.getPhase(Settings.subtickDefaultPhase)))
      )
    );
  }

  private static int when(CommandContext<ServerCommandSource> c)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    Messenger.m(c.getSource(), t("Dimension "), handler.getDimension(), t(" is in "), handler.getPhase(), t(" phase"));
    return 0;
  }

  private static int status(CommandContext<ServerCommandSource> c)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    Messenger.m(c.getSource(), t("Dimension "), handler.getDimension(), t(" is " + (handler.frozen ? "frozen" : "unfrozen") + " in "), handler.getPhase(), t(" phase"));
    return 0;
  }

  private static int freeze(CommandContext<ServerCommandSource> c, int phase)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(handler.frozen || handler.freezing)
    {
      Messenger.m(c.getSource(), t("Dimension "), handler.getDimension(), t(" is already frozen"));
      return 0;
    }
    Messenger.m(c.getSource(), t("Freezing dimension "), handler.getDimension(), t(" in "), TickHandlers.getPhase(phase), t(" phase"));
    handler.freeze(phase);
    return 0;
  }

  private static int unFreeze(CommandContext<ServerCommandSource> c)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(!(handler.frozen || handler.freezing))
    {
      Messenger.m(c.getSource(), t("Dimension "), handler.getDimension(), t(" is already unfrozen"));
      return 0;
    }
    Messenger.m(c.getSource(), t("Unfreezing dimension "), handler.getDimension());
    handler.unfreeze();
    return 0;
  }

  private static int toggleFreeze(CommandContext<ServerCommandSource> c, int phase)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(handler.frozen || handler.freezing)
    {
      unFreeze(c);
    }
    else
    {
      freeze(c, phase);
    }
    return 0;
  }

  private static int step(CommandContext<ServerCommandSource> c, int ticks, int phase)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(!handler.canStep(c)) return 1;

    if(ticks == 0 && phase <= handler.current_phase)
    {
      Messenger.m(c.getSource(), TickHandlers.getPhase(phase), t(" phase already stepped to for this tick in dimension "), handler.getDimension(), t(". Change [ticks] to more than 0 to step to a new tick"));
      return 1;
    }

    Messenger.m(c.getSource(), t("Stepping dimension "), handler.getDimension(), n(" " + ticks), t(" tick" + (ticks == 1 ? "" : "s") + ", ending at "), TickHandlers.getPhase(phase), t(" phase"));
    handler.step(ticks, phase);
    return 0;
  }
}
