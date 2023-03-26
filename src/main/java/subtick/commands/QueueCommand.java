package subtick.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import net.minecraft.server.world.ServerWorld;

import carpet.utils.Messenger;

import subtick.TickHandlers;
import subtick.TickHandler;

public class QueueCommand
{
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
  {
    dispatcher.register(
      literal("queueStep")
      .then(literal("blockEvent")
        .then(argument("count", integer(1))
          .executes((c) -> blockEventStep(c, getInteger(c, "count")))
        )
        .executes((c) -> blockEventStep(c, 1))
      )
      .then(literal("entity")
        .then(argument("count", integer(1))
          .executes((c) -> entityStep(c, getInteger(c, "count")))
        )
        .executes((c) -> entityStep(c, 1))
      )
      .then(literal("blockEntity")
        .then(argument("count", integer(1))
          .executes((c) -> blockEntityStep(c, getInteger(c, "count")))
        )
        .executes((c) -> blockEntityStep(c, 1))
      )
    );
  }

  private static int blockEventStep(CommandContext<ServerCommandSource> c, int count)
  {
    ServerWorld world = c.getSource().getWorld();
    TickHandler handler = TickHandlers.getHandler(world.getRegistryKey());
    if(handler.stepping)
    {
      Messenger.m(c.getSource(), "ig Dimension " + handler.dimensionName + " is already tick stepping. Try again later.");
      return 1;
    }

    if(handler.scheduled_block_event_step)
    {
      Messenger.m(c.getSource(), "ig Dimension " + handler.dimensionName + " is already scheduled for Block Event stepping. Try again later.");
      return 1;
    }

    if(handler.current_phase > TickHandlers.BLOCK_EVENT || world.syncedBlockEventQueue.isEmpty())
    {
      Messenger.m(c.getSource(), "ig No more Block Events for this tick in dimension " + handler.dimensionName);
      return 1;
    }

    if(count == 1)
      Messenger.m(c.getSource(), "ig Stepping 1 Block Event in dimension " + handler.dimensionName);
    else
      Messenger.m(c.getSource(), "ig Stepping " + count + " Block Events in dimension " + handler.dimensionName);
    if(handler.current_phase < TickHandlers.BLOCK_EVENT)
    {
      handler.step(0, TickHandlers.BLOCK_EVENT);
      handler.scheduleBlockEventStep(count);
      return 0;
    }
    handler.stepBlockEvents(count);
    return 0;
  }

  private static int entityStep(CommandContext<ServerCommandSource> c, int count)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(handler.stepping)
    {
      Messenger.m(c.getSource(), "ig Dimension " + handler.dimensionName + " is already tick stepping. Try again later.");
      return 1;
    }

    if(handler.scheduled_entity_step)
    {
      Messenger.m(c.getSource(), "ig Dimension " + handler.dimensionName + " is already scheduled for Entity stepping. Try again later.");
      return 1;
    }

    if(handler.current_phase > TickHandlers.ENTITY || (handler.entity_iterator != null && !handler.entity_iterator.hasNext()))
    {
      Messenger.m(c.getSource(), "ig No more Entities for this tick in dimension " + handler.dimensionName);
      return 1;
    }

    if(count == 1)
      Messenger.m(c.getSource(), "ig Stepping 1 Entity in dimension " + handler.dimensionName);
    else
      Messenger.m(c.getSource(), "ig Stepping " + count + " Entities in dimension " + handler.dimensionName);
    if(handler.current_phase < TickHandlers.ENTITY)
    {
      handler.step(0, TickHandlers.ENTITY);
      handler.scheduleEntityStep(count);
      return 0;
    }
    handler.stepEntities(count);
    return 0;
  }

  private static int blockEntityStep(CommandContext<ServerCommandSource> c, int count)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(handler.stepping)
    {
      Messenger.m(c.getSource(), "ig Dimension " + handler.dimensionName + " is already tick stepping. Try again later.");
      return 1;
    }

    if(handler.scheduled_block_entity_step)
    {
      Messenger.m(c.getSource(), "ig Dimension " + handler.dimensionName + " is already scheduled for Block Entity stepping. Try again later.");
      return 1;
    }

    if(handler.current_phase > TickHandlers.BLOCK_ENTITY || (handler.world.blockEntityTickers.isEmpty() && handler.world.pendingBlockEntityTickers.isEmpty()) || (handler.block_entity_iterator != null && !handler.block_entity_iterator.hasNext()))
    {
      Messenger.m(c.getSource(), "ig No more Block Entities for this tick in dimension " + handler.dimensionName);
      return 1;
    }

    if(count == 1)
      Messenger.m(c.getSource(), "ig Stepping 1 Block Entity in dimension " + handler.dimensionName);
    else
      Messenger.m(c.getSource(), "ig Stepping " + count + " Block Entities in dimension " + handler.dimensionName);
    if(handler.current_phase < TickHandlers.BLOCK_ENTITY)
    {
      handler.step(0, TickHandlers.BLOCK_ENTITY);
      handler.scheduleBlockEntityStep(count);
      return 0;
    }
    handler.stepBlockEntities(count);
    return 0;
  }
}
