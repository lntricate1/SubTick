package subtick.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import subtick.Settings;
import subtick.SubTick;
import subtick.TickPhase;
import subtick.TickingMode;

public class QueueCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("queueStep")
      .then(argument("phase", word())
        .suggests((c, b) -> suggest(TickPhase.getCommandKeys(), b))
        .then(argument("mode", word())
          .suggests((c, b) -> suggest(TickPhase.byCommandKey(getString(c, "phase")).getModeCommandKeys(), b))
          .then(argument("count", integer(1))
            .then(argument("range", integer(-1, 46340))
              .then(literal("force")
                .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), TickingMode.byCommandKey(getString(c, "phase"), getString(c, "mode")), getInteger(c, "count"), getInteger(c, "range"), true))
              )
              .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), TickingMode.byCommandKey(getString(c, "phase"), getString(c, "mode")), getInteger(c, "count"), getInteger(c, "range"), false))
            )
            .then(literal("force")
              .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), TickingMode.byCommandKey(getString(c, "phase"), getString(c, "mode")), getInteger(c, "count"), Settings.subtickDefaultRange, true))
            )
            .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), TickingMode.byCommandKey(getString(c, "phase"), getString(c, "mode")), getInteger(c, "count"), Settings.subtickDefaultRange, false))
          )
          .then(literal("force")
            .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), TickingMode.byCommandKey(getString(c, "phase"), getString(c, "mode")), 1, Settings.subtickDefaultRange, true))
          )
          .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), TickingMode.byCommandKey(getString(c, "phase"), getString(c, "mode")), 1, Settings.subtickDefaultRange, false))
        )
        .then(argument("count", integer(1))
          .then(argument("range", integer(-1, 46340))
            .then(literal("force")
              .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), getInteger(c, "count"), getInteger(c, "range"), true))
            )
            .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), getInteger(c, "count"), getInteger(c, "range"), false))
          )
          .then(literal("force")
            .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), getInteger(c, "count"), Settings.subtickDefaultRange, true))
          )
          .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), getInteger(c, "count"), Settings.subtickDefaultRange, false))
        )
        .then(literal("force")
          .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), 1, Settings.subtickDefaultRange, true))
        )
        .executes(c -> step(c, TickPhase.byCommandKey(getString(c, "phase")), 1, Settings.subtickDefaultRange, false))
      )
    );
  }

  private static int step(CommandContext<CommandSourceStack> c, TickPhase phase, int count, int range, boolean force) throws CommandSyntaxException
  {
    return step(c, phase, phase.getDefaultMode(), count, range, force);
  }

  private static int step(CommandContext<CommandSourceStack> c, TickPhase phase, TickingMode mode, int count, int range, boolean force) throws CommandSyntaxException
  {
    SubTick.getTickHandler(c).queues.schedule(c, phase, mode, count, new BlockPos(c.getSource().getPosition()), range, force);
    return Command.SINGLE_SUCCESS;
  }
}
