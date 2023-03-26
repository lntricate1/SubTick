package subtick.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import carpet.utils.Messenger;

import subtick.TickHandlers;
import subtick.TickHandler;
import subtick.Settings;

public class SubTickCommand
{
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
  {
    dispatcher.register(
      literal("subtick")
      .then(literal("when")
        .executes((c) -> when(c))
      )
      .then(literal("freeze")
        .then(literal("worldBorder")
          .executes((c) -> toggleFreeze(c, TickHandlers.WORLD_BORDER))
        )
        .then(literal("weather")
          .executes((c) -> toggleFreeze(c, TickHandlers.WEATHER))
        )
        .then(literal("time")
          .executes((c) -> toggleFreeze(c, TickHandlers.TIME))
        )
        .then(literal("tileTick")
          .executes((c) -> toggleFreeze(c, TickHandlers.TILE_TICK))
        )
        .then(literal("fluidTick")
          .executes((c) -> toggleFreeze(c, TickHandlers.FLUID_TICK))
        )
        .then(literal("raid")
          .executes((c) -> toggleFreeze(c, TickHandlers.RAID))
        )
        .then(literal("chunk")
          .executes((c) -> toggleFreeze(c, TickHandlers.CHUNK))
        )
        .then(literal("blockEvent")
          .executes((c) -> toggleFreeze(c, TickHandlers.BLOCK_EVENT))
        )
        .then(literal("entity")
          .executes((c) -> toggleFreeze(c, TickHandlers.ENTITY))
        )
        .then(literal("blockEntity")
          .executes((c) -> toggleFreeze(c, TickHandlers.BLOCK_ENTITY))
        )
        .then(literal("entityManagement")
          .executes((c) -> toggleFreeze(c, TickHandlers.ENTITY_MANAGEMENT))
        )
        .executes((c) -> toggleFreeze(c, Settings.defaultPhase))
      )
      .then(literal("step")
        .then(argument("ticks", integer(0))
          .then(literal("worldBorder")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.WORLD_BORDER))
          )
          .then(literal("weather")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.WEATHER))
          )
          .then(literal("tileTick")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.TILE_TICK))
          )
          .then(literal("fluidTick")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.FLUID_TICK))
          )
          .then(literal("raid")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.RAID))
          )
          .then(literal("chunk")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.CHUNK))
          )
          .then(literal("blockEvent")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.BLOCK_EVENT))
          )
          .then(literal("entity")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.ENTITY))
          )
          .then(literal("blockEntity")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.BLOCK_ENTITY))
          )
          .then(literal("entityManagement")
            .executes((c) -> step(c, getInteger(c, "ticks"), TickHandlers.ENTITY_MANAGEMENT))
          )
          .executes((c) -> step(c, getInteger(c, "ticks"), Settings.defaultPhase))
        )
        .executes((c) -> step(c, 1, Settings.defaultPhase))
      )
    );
  }

  private static int when(CommandContext<ServerCommandSource> c)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    Messenger.m(c.getSource(), "ig Dimension " + handler.dimensionName + " is in " + TickHandlers.tickPhaseNames[handler.current_phase] + " phase");
    return 0;
  }

  private static int toggleFreeze(CommandContext<ServerCommandSource> c, int phase)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(handler.frozen)
    {
      Messenger.m(c.getSource(), "ig Unfreezing dimension " + handler.dimensionName);
      handler.unfreeze();
    }
    else
    {
      Messenger.m(c.getSource(), "ig Freezing dimension " + handler.dimensionName + " in " + TickHandlers.tickPhaseNames[phase] + " phase");
      handler.freeze(phase);
    }
    return 0;
  }

  private static int step(CommandContext<ServerCommandSource> c, int ticks, int phase)
  {
    TickHandler handler = TickHandlers.getHandler(c.getSource().getWorld().getRegistryKey());
    if(handler.stepping)
    {
      Messenger.m(c.getSource(), "ig Dimension " + handler.dimensionName + " is already tick stepping. Try again later.");
      return 1;
    }
    if(ticks == 0 && phase <= handler.current_phase)
    {
      Messenger.m(c.getSource(), "ig " + TickHandlers.tickPhaseNames[phase] + " phase already stepped to for this tick in dimension " + handler.dimensionName + ". Change [ticks] to more than 0 to step to a new tick.");
      return 1;
    }
    if(ticks == 1)
      Messenger.m(c.getSource(), "ig Stepping dimension " + handler.dimensionName + " 1 tick, ending in " + TickHandlers.tickPhaseNames[phase] + " phase");
    else
      Messenger.m(c.getSource(), "ig Stepping dimension " + handler.dimensionName + " " + ticks + " ticks, ending in " + TickHandlers.tickPhaseNames[phase] + " phase");

    handler.step(ticks, phase);
    return 0;
  }
}
