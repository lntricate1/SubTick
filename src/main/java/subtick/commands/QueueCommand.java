package subtick.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import subtick.TickHandlers;
import subtick.Settings;
import subtick.TickHandler;

public class QueueCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("queueStep")
      .then(argument("phase", word())
        .suggests((c, b) -> suggest(new String[]{"tileTick", "fluidTick", "blockEvent", "entity", "blockEntity"}, b))
        .then(argument("count", integer(1))
          .then(argument("range", integer(1))
            .then(literal("force")
              .executes((c) -> step(c, TickHandlers.getPhase(getString(c, "phase")), getInteger(c, "count"), getInteger(c, "range"), true))
            )
            .executes((c) -> step(c, TickHandlers.getPhase(getString(c, "phase")), getInteger(c, "count"), getInteger(c, "range"), false))
          )
          .then(literal("force")
            .executes((c) -> step(c, TickHandlers.getPhase(getString(c, "phase")), getInteger(c, "count"), Settings.subtickDefaultRange, true))
          )
          .executes((c) -> step(c, TickHandlers.getPhase(getString(c, "phase")), getInteger(c, "count"), Settings.subtickDefaultRange, false))
        )
        .then(literal("force")
          .executes((c) -> step(c, TickHandlers.getPhase(getString(c, "phase")), 1, Settings.subtickDefaultRange, true))
        )
        .executes((c) -> step(c, TickHandlers.getPhase(getString(c, "phase")), 1, Settings.subtickDefaultRange, false))
      )
    );
  }

  private static int step(CommandContext<CommandSourceStack> c, int phase, int count, int range, boolean force)
  {
    System.out.println(phase);
    TickHandler handler = TickHandlers.getHandler(c.getSource().getLevel().dimension());
    if(!force && !handler.queues.canStep(c, phase)) return 0;

    handler.queues.commandSource = c.getSource();
    handler.queues.scheduleQueueStep(phase, count, new BlockPos(c.getSource().getPosition()), range);
    return 0;
  }
}
