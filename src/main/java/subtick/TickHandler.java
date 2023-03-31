package subtick;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import carpet.utils.Messenger;

import carpet.network.ServerNetworkHandler;
import subtick.mixins.carpet.ServerNetworkHandlerAccessor;
import carpet.CarpetSettings;
import carpet.network.CarpetClient;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;

// entity step
import it.unimi.dsi.fastutil.objects.ObjectIterator;
// block entity step
import java.util.Iterator;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

public class TickHandler
{
  public final ServerWorld world;
  public final String dimensionName;

  // Freeze
  public boolean frozen = false;
  public boolean freezing = false;
  private boolean unfreezing = false;
  private int target_phase = 0;
  // Step
  public boolean stepping = false;
  private boolean in_first_stepped_phase = false;
  private int remaining_ticks = 0;
  public int current_phase = 0;

  // Queue stuff
  public int queue_stepping = -1;
  public int scheduled_queue_step = -1;
  private int scheduled_queue_step_count = 0;
  // Queues
  public ObjectIterator entity_iterator = null;
  public Iterator block_entity_iterator = null;

  public TickHandler(String dimensionName, ServerWorld world)
  {
    this.dimensionName = dimensionName;
    this.world = world;
  }

  public boolean shouldTick(int phase)
  {
    // Freezing
    if(freezing && phase == target_phase)
    {
      freezing = false;
      frozen = true;
      current_phase = phase;
      updateFrozenStateToConnectedPlayers();
      return false;
    }

    // Normal ticking
    if(!frozen)
    {
      current_phase = phase;
      return true;
    }
    // Everything below this is frozen logic

    // Unfreezing
    if(unfreezing && phase == current_phase)
    {
      unfreezing = false;
      frozen = false;
      updateFrozenStateToConnectedPlayers();
      return true;
    }

    // Return false if frozen and not stepping
    if(!stepping || phase != current_phase) return false;
    // Continues only if stepping and in current_phase

    // Stepping
    if(phase == 0 && !in_first_stepped_phase) --remaining_ticks;
    in_first_stepped_phase = false;
    if(remaining_ticks < 1 && phase == target_phase)
    {
      stepping = false;
      if(scheduled_queue_step != -1)
      {
        switch(scheduled_queue_step)
        {
          case TickHandlers.BLOCK_EVENT:
            stepBlockEvents(scheduled_queue_step_count);
            break;
          case TickHandlers.ENTITY:
            stepEntities(scheduled_queue_step_count);
            break;
          case TickHandlers.BLOCK_ENTITY:
            stepBlockEntities(scheduled_queue_step_count);
            break;
        }
        scheduled_queue_step = -1;
      }
      return false;
    }
    advancePhase();
    return true;
  }

  private void advancePhase()
  {
    current_phase = (current_phase + 1) % TickHandlers.TOTAL_PHASES;
  }

  public void step(int ticks, int phase)
  {
    stepping = true;
    in_first_stepped_phase = true;
    remaining_ticks = ticks;
    target_phase = phase;
    if(ticks != 0 || phase != current_phase)
      finishQueueStep();
  }

  public void finishQueueStep()
  {
    finishStepEntities();
    finishStepBlockEntities();
  }

  public void freeze(int phase)
  {
    freezing = true;
    target_phase = phase;
  }

  public void unfreeze()
  {
    if(freezing) freezing = false;
    else
    {
      unfreezing = true;
      stepping = false;
      finishQueueStep();
    }
  }

  public void scheduleQueueStep(int phase, int count)
  {
    step(0, phase);
    scheduled_queue_step = phase;
    scheduled_queue_step_count = count;
  }

  public void stepBlockEvents(int count)
  {
    for(int i = 0; i < count && world.syncedBlockEventQueue.size() != 0; i ++)
    {
      BlockEvent blockEvent = (BlockEvent)world.syncedBlockEventQueue.removeFirst();
      if(world.processBlockEvent(blockEvent))
        world.server.getPlayerManager().sendToAround((PlayerEntity)null, (double)blockEvent.getPos().getX(), (double)blockEvent.getPos().getY(), (double)blockEvent.getPos().getZ(), 64.0D, world.getRegistryKey(), new BlockEventS2CPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getType(), blockEvent.getData()));
    }
  }

  public void stepEntities(int count)
  {
    if(queue_stepping == -1)
    {
      world.entityList.iterating = world.entityList.entities;
      entity_iterator = world.entityList.entities.values().iterator();
      queue_stepping = TickHandlers.ENTITY;
    }

    for(int i = 0; i < count && entity_iterator.hasNext(); i++)
    {
      Entity entity = (Entity)entity_iterator.next();
      if(entity.isRemoved()) continue;

      if(world.shouldCancelSpawn(entity))
        entity.discard();
      else
      {
        entity.checkDespawn();
        Entity entity2 = entity.getVehicle();
        if(entity2 != null)
        {
          if(!entity2.isRemoved() && entity2.hasPassenger(entity))
            continue;

          entity.stopRiding();
        }

        world.tickEntity(world::tickEntity, entity);
      }
    }
  }

  public void stepBlockEntities(int count)
  {
    queue_stepping = TickHandlers.BLOCK_ENTITY;
    if(!world.iteratingTickingBlockEntities)
    {
      world.iteratingTickingBlockEntities = true;
      if(!world.pendingBlockEntityTickers.isEmpty())
      {
        world.blockEntityTickers.addAll(world.pendingBlockEntityTickers);
        world.pendingBlockEntityTickers.clear();
      }

      block_entity_iterator = world.blockEntityTickers.iterator();
    }

    for(int i = 0; i < count && block_entity_iterator.hasNext(); i++)
    {
      BlockEntityTickInvoker blockEntityTickInvoker = (BlockEntityTickInvoker)block_entity_iterator.next();
      if(blockEntityTickInvoker.isRemoved())
        block_entity_iterator.remove();
      else
        blockEntityTickInvoker.tick();

      System.out.println(blockEntityTickInvoker.getPos());
    }
  }

  private void finishStepEntities()
  {
    if(queue_stepping == TickHandlers.ENTITY)
    {
      stepEntities(2147483647);
      world.entityList.iterating = null;
      queue_stepping = -1;
      advancePhase();
    }
  }

  private void finishStepBlockEntities()
  {
    if(queue_stepping == TickHandlers.BLOCK_ENTITY)
    {
      stepBlockEntities(2147483647);
      world.iteratingTickingBlockEntities = false;
      queue_stepping = -1;
      advancePhase();
    }
  }

  public boolean canStep(CommandContext<ServerCommandSource> c)
  {
    if(!frozen)
    {
      Messenger.m(c.getSource(), "ig Cannot step because dimension ", getDimension(), "ig  is not frozen.");
      return false;
    }

    if(stepping)
    {
      Messenger.m(c.getSource(), "ig Cannot step because dimension ", getDimension(), "ig  is already tick stepping.");
      return false;
    }

    if(scheduled_queue_step != -1)
    {
      Messenger.m(c.getSource(), "ig Cannot step because dimension ", getDimension(), "ig  is already scheduled to step through ", "it " + TickHandlers.tickPhaseNames[scheduled_queue_step], "ig  queue.");
    }

    return true;
  }

  public boolean canQueueStep(CommandContext<ServerCommandSource> c, int queue)
  {
    if(!canStep(c)) return false;

    if(current_phase > queue)
    {
      Messenger.m(c.getSource(), "ig Cannot queueStep because ", TickHandlers.getPhase(queue), "ig  phase already happened for dimension ", getDimension());
      return false;
    }

    if(queue_stepping != -1)
    {
      boolean noMore = false;
      switch(queue_stepping)
      {
        case TickHandlers.BLOCK_EVENT:
          noMore = world.syncedBlockEventQueue.isEmpty();
          break;
        case TickHandlers.ENTITY:
          noMore = !entity_iterator.hasNext();
          break;
        case TickHandlers.BLOCK_ENTITY:
          noMore = !block_entity_iterator.hasNext();
          break;
      }

      if(noMore)
      {
        Messenger.m(c.getSource(), "ig No more elements in queue ", TickHandlers.getPhase(queue), "ig  for dimension ", getDimension());
        return false;
      }
    }

    return true;
  }

  private void updateFrozenStateToConnectedPlayers()
  {
    if(CarpetSettings.superSecretSetting) return;

    for(ServerPlayerEntity player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.world != world) continue;

      NbtCompound tag = new NbtCompound();
      NbtCompound tickingState = new NbtCompound();
      tickingState.putBoolean("is_paused", frozen);
      tickingState.putBoolean("deepFreeze", frozen);
      tag.put("TickingState", tickingState);

      PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
      packetBuf.writeVarInt(CarpetClient.DATA);
      packetBuf.writeNbt(tag);

      player.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetClient.CARPET_CHANNEL, packetBuf));
    }
  }

  public String getPhase()
  {
    return Settings.subtickPhaseFormat + " " + TickHandlers.tickPhaseNames[current_phase];
  }

  public String getDimension()
  {
    return Settings.subtickDimensionFormat + " " + dimensionName;
  }
}
