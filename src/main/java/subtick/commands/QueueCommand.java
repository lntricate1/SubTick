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

import java.util.HashSet;
import java.util.Set;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import subtick.Queues;
import subtick.Settings;
import subtick.SubTick;

public class QueueCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("queueStep")
      .then(argument("queue", word()).suggests((c, b) -> suggest(Queues.getQueues(), b))
        .then(argument("mode", word()).suggests((c, b) -> suggest(new HashSet<String>()
          {{
            Set<String> modes = SubTick.getTickHandler(c).queues.byCommandKey(getString(c, "queue")).getModes();
            addAll(modes);
            if(modes.isEmpty())
              add("1");
          }}.toArray(new String[0]), b))
          .then(argument("count", integer(1)).suggests((c, b) -> suggest(new String[]{"1"}, b))
            .then(argument("range", integer(-1, 46340)).suggests((c, b) -> suggest(new String[]{"-1", "32"}, b))
              .then(literal("force")
                .executes((c) -> step(c, getString(c, "queue"), getString(c, "mode"), getInteger(c, "count"), getInteger(c, "range"), true))
              )
              .executes((c) -> step(c, getString(c, "queue"), getString(c, "mode"), getInteger(c, "count"), getInteger(c, "range"), false))
            )
            .then(literal("force")
              .executes((c) -> step(c, getString(c, "queue"), getString(c, "mode"), getInteger(c, "count"), Settings.subtickDefaultRange, true))
            )
            .executes((c) -> step(c, getString(c, "queue"), getString(c, "mode"), getInteger(c, "count"), Settings.subtickDefaultRange, false))
          )
          .then(literal("force")
            .executes((c) -> step(c, getString(c, "queue"), getString(c, "mode"), 1, Settings.subtickDefaultRange, true))
          )
          .executes((c) -> step(c, getString(c, "queue"), getString(c, "mode"), 1, Settings.subtickDefaultRange, false))
        )
        .then(argument("count", integer(1))
          .then(argument("range", integer(-1, 46340)).suggests((c, b) -> suggest(new String[]{"-1", "32"}, b))
            .then(literal("force")
              .executes((c) -> step(c, getString(c, "queue"), "", getInteger(c, "count"), getInteger(c, "range"), true))
            )
            .executes((c) -> step(c, getString(c, "queue"), "", getInteger(c, "count"), getInteger(c, "range"), false))
          )
          .then(literal("force")
            .executes((c) -> step(c, getString(c, "queue"), "", getInteger(c, "count"), Settings.subtickDefaultRange, true))
          )
          .executes((c) -> step(c, getString(c, "queue"), "", getInteger(c, "count"), Settings.subtickDefaultRange, false))
        )
        // .then(literal("force")
        //   .executes((c) -> step(c, getString(c, "queue"), "", 1, Settings.subtickDefaultRange, true))
        // )
        .executes((c) -> step(c, getString(c, "queue"), "", 1, Settings.subtickDefaultRange, false))
      )
    );
  }

  private static int step(CommandContext<CommandSourceStack> c, String commandKey, String modeKey, int count, int range, boolean force) throws CommandSyntaxException
  {
    //#if MC >= 11904
    //$$ SubTick.getTickHandler(c).queues.schedule(c, commandKey, modeKey, count, BlockPos.containing(c.getSource().getPosition()), range, force);
    //#else
    SubTick.getTickHandler(c).queues.schedule(c, commandKey, modeKey, count, new BlockPos(c.getSource().getPosition()), range, force);
    //#endif
    return Command.SINGLE_SUCCESS;
  }
}
