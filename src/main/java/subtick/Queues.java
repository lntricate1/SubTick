package subtick;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import carpet.CarpetSettings;
import carpet.utils.Messenger;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;

// highlights
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

// packets
import net.minecraft.server.network.ServerPlayerEntity;
import subtick.mixins.carpet.ServerNetworkHandlerAccessor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;

// tile tick step
import net.minecraft.server.world.ServerTickScheduler;
import net.minecraft.world.ScheduledTick;
// block event step
import net.minecraft.server.world.BlockEvent;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
// entity step
import net.minecraft.entity.Entity;

import static subtick.TickHandlers.t;
import static subtick.TickHandlers.n;

public class Queues
{
  private final TickHandler handler;
  public ServerCommandSource commandSource;

  public int stepping = -1;
  public int scheduled = -1;
  private int scheduled_count = 0;
  private boolean exhausted;
  private BlockPos pos = new BlockPos(0, 0, 0);
  private int range = -1;

  // Queues
  public Iterator<Entity> entity_iterator = null;
  public Iterator<BlockEntityTickInvoker> block_entity_iterator = null;

  public Queues(TickHandler handler)
  {
    this.handler = handler;
  }

  public void scheduleQueueStep(int phase, int count, BlockPos pos, int range)
  {
    if(phase < handler.current_phase || isExhausted())
      handler.step(1, phase);
    else
      handler.step(0, phase);
    scheduled = phase;
    scheduled_count = count;
    this.pos = pos;
    this.range = range == -1 ? -1 : range*range;
  }

  public void executeScheduledSteps()
  {
    if(scheduled != -1)
    {
      switch(scheduled)
      {
        case TickHandlers.TILE_TICK:
          stepTileTicks(handler.world.blockTickScheduler, scheduled_count);
          break;
        case TickHandlers.FLUID_TICK:
          stepTileTicks(handler.world.fluidTickScheduler, scheduled_count);
          break;
        case TickHandlers.BLOCK_EVENT:
          stepBlockEvents(scheduled_count);
          break;
        case TickHandlers.ENTITY:
          stepEntities(scheduled_count);
          break;
        case TickHandlers.BLOCK_ENTITY:
          stepBlockEntities(scheduled_count);
          break;
      }
      scheduled = -1;
    }
  }

  private static int squaredDistance(BlockPos a, BlockPos b)
  {
    int x = a.getX() - b.getX();
    int y = a.getY() - b.getY();
    int z = a.getZ() - b.getZ();
    return x*x + y*y + z*z;
  }

  private static void addOutlines(BlockPos pos, List<Box> boxes, ServerWorld world)
  {
    List<Box> outlineBoxes = world.getBlockState(pos).getOutlineShape(world, pos).getBoundingBoxes();
    if(outlineBoxes.isEmpty())
      boxes.add(new Box(pos));
    else
    {
      outlineBoxes.replaceAll((box) -> box.offset(pos));
      boxes.addAll(outlineBoxes);
    }
  }

  public <T> void stepTileTicks(ServerTickScheduler<T> scheduler, int count)
  {
    if(stepping == -1)
    {
      stepping = handler.current_phase;
      Iterator<ScheduledTick<T>> iterator = scheduler.scheduledTickActionsInOrder.iterator();

      for(int i = 0; i < 65536 && iterator.hasNext();)
      {
        ScheduledTick<T> tick = (ScheduledTick<T>)iterator.next();
        if(tick.time > handler.world.getTime())
          break;
        if(handler.world.method_37117(tick.pos))
        {
          iterator.remove();
          scheduler.scheduledTickActions.remove(tick);
          scheduler.currentTickActions.add(tick);
          i ++;
        }
      }
    }

    ArrayList<Box> boxes = new ArrayList<>();
    int i = 0;
    while(i < count)
    {
      ScheduledTick<T> tick = (ScheduledTick<T>)scheduler.currentTickActions.poll();
      if(tick == null)
      {
        exhausted = true;
        break;
      }

      if(handler.world.method_37117(tick.pos))
      {
        scheduler.consumedTickActions.add(tick);
        scheduler.tickConsumer.accept(tick);
      }
      else
        scheduler.schedule(tick.pos, tick.getObject(), 0);

      if(range == -1 || squaredDistance(tick.pos, pos) <= range)
      {
        addOutlines(tick.pos, boxes, handler.world);
        i ++;
      }
    }

    sendBlockHighlights(boxes, handler.world);
    if(range != -1)
      sendFeedback(i);
  }

  private void stepBlockEvents(int count)
  {
    stepping = TickHandlers.BLOCK_EVENT;
    ArrayList<Box> boxes = new ArrayList<>();
    int i = 0;
    while(i < count && handler.world.syncedBlockEventQueue.size() != 0)
    {
      BlockEvent blockEvent = (BlockEvent)handler.world.syncedBlockEventQueue.removeFirst();
      if(handler.world.processBlockEvent(blockEvent))
      {
        handler.world.server.getPlayerManager().sendToAround((PlayerEntity)null, (double)blockEvent.getPos().getX(), (double)blockEvent.getPos().getY(), (double)blockEvent.getPos().getZ(), 64.0D, handler.world.getRegistryKey(), new BlockEventS2CPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getType(), blockEvent.getData()));

        if(range == -1 || squaredDistance(blockEvent.getPos(), pos) <= range)
        {
          addOutlines(blockEvent.getPos(), boxes, handler.world);
          i ++;
        }
      }
    }

    sendBlockHighlights(boxes, handler.world);
    if(range != -1)
      sendFeedback(i);
  }

  private void stepEntities(int count)
  {
    if(stepping == -1)
    {
      handler.world.entityList.iterating = handler.world.entityList.entities;
      entity_iterator = handler.world.entityList.entities.values().iterator();
      stepping = TickHandlers.ENTITY;
    }

    ArrayList<Integer> ids = new ArrayList<>();
    int i = 0;
    while(i < count && entity_iterator.hasNext())
    {
      Entity entity = (Entity)entity_iterator.next();
      if(entity.isRemoved()) continue;

      if(handler.world.shouldCancelSpawn(entity))
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

        handler.world.tickEntity(handler.world::tickEntity, entity);

      }
      if(range == -1 || squaredDistance(entity.getBlockPos(), pos) <= range)
      {
        ids.add(entity.getId());
        i ++;
      }
    }
    exhausted = !entity_iterator.hasNext();

    if(!ids.isEmpty())
      sendEntityHighlights(ids, handler.world);
    if(range != -1)
      sendFeedback(i);
  }

  private void stepBlockEntities(int count)
  {
    stepping = TickHandlers.BLOCK_ENTITY;
    if(!handler.world.iteratingTickingBlockEntities)
    {
      handler.world.iteratingTickingBlockEntities = true;
      if(!handler.world.pendingBlockEntityTickers.isEmpty())
      {
        handler.world.blockEntityTickers.addAll(handler.world.pendingBlockEntityTickers);
        handler.world.pendingBlockEntityTickers.clear();
      }

      block_entity_iterator = handler.world.blockEntityTickers.iterator();
    }

    ArrayList<Box> boxes = new ArrayList<>();
    int i = 0;
    while(i < count && block_entity_iterator.hasNext())
    {
      BlockEntityTickInvoker blockEntityTickInvoker = (BlockEntityTickInvoker)block_entity_iterator.next();
      if(range == -1 || squaredDistance(blockEntityTickInvoker.getPos(), pos) <= range)
      {
        addOutlines(blockEntityTickInvoker.getPos(), boxes, handler.world);
        i ++;
      }

      if(blockEntityTickInvoker.isRemoved())
        block_entity_iterator.remove();
      else
        blockEntityTickInvoker.tick();
    }
    exhausted = !block_entity_iterator.hasNext();

    sendBlockHighlights(boxes, handler.world);
    if(range != -1)
      sendFeedback(i);
  }

  public void sendFeedback(int count)
  {
    if(count == 0)
      Messenger.m(commandSource, handler.getDimension(), t(" "), TickHandlers.getPhase(stepping), t(" queue exhausted"));
    else if(count != scheduled_count || isExhausted())
      Messenger.m(commandSource, handler.getDimension(), t(" stepped"), n(" " + count + " "), TickHandlers.getPhase(stepping, count), t(" (queue exhausted)"));
    else
      Messenger.m(commandSource, handler.getDimension(), t(" stepped"), n(" " + count + " "), TickHandlers.getPhase(stepping, count));
  }

  public void finishQueueStep()
  {
    finishStepTileTicks();
    finishStepFluidTicks();
    finishStepBlockEvents();
    finishStepEntities();
    finishStepBlockEntities();
    exhausted = false;
  }

  private void finishStepTileTicks()
  {
    if(stepping == TickHandlers.TILE_TICK)
    {
      range = -1;
      stepTileTicks(handler.world.blockTickScheduler, 2147483647);
      handler.world.blockTickScheduler.consumedTickActions.clear();
      handler.world.blockTickScheduler.currentTickActions.clear();
      stepping = -1;
      clearBlockHighlights(handler.world);
    }
  }

  private void finishStepFluidTicks()
  {
    if(stepping == TickHandlers.FLUID_TICK)
    {
      range = -1;
      stepTileTicks(handler.world.fluidTickScheduler, 2147483647);
      handler.world.fluidTickScheduler.consumedTickActions.clear();
      handler.world.fluidTickScheduler.currentTickActions.clear();
      stepping = -1;
      clearBlockHighlights(handler.world);
    }
  }

  private void finishStepBlockEvents()
  {
    if(stepping == TickHandlers.BLOCK_EVENT)
    {
      stepping = -1;
      clearBlockHighlights(handler.world);
    }
  }

  private void finishStepEntities()
  {
    if(stepping == TickHandlers.ENTITY)
    {
      range = -1;
      stepEntities(2147483647);
      handler.world.entityList.iterating = null;
      stepping = -1;
      handler.advancePhase();
      clearEntityHighlights(handler.world);
    }
  }

  private void finishStepBlockEntities()
  {
    if(stepping == TickHandlers.BLOCK_ENTITY)
    {
      range = -1;
      stepBlockEntities(2147483647);
      handler.world.iteratingTickingBlockEntities = false;
      stepping = -1;
      handler.advancePhase();
      clearBlockHighlights(handler.world);
    }
  }

  private static void sendBlockHighlights(List<Box> boxes, ServerWorld world)
  {
    if(CarpetSettings.superSecretSetting || boxes.isEmpty()) return;

    NbtCompound tag = new NbtCompound();
    NbtList list = new NbtList();
    for(Box box : boxes)
    {
      NbtCompound nbt = new NbtCompound();
      nbt.putDouble("x", box.minX);
      nbt.putDouble("y", box.minY);
      nbt.putDouble("z", box.minZ);
      nbt.putDouble("X", box.maxX);
      nbt.putDouble("Y", box.maxY);
      nbt.putDouble("Z", box.maxZ);
      nbt.putDouble("color", Settings.subtickHighlightColor);
      list.add(nbt);
    }
    tag.put("BlockHighlighting", list);

    for(ServerPlayerEntity player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.world != world) continue;
      TickHandler.sendNbt(player, tag);
    }
  }

  private static void sendEntityHighlights(List<Integer> ids, ServerWorld world)
  {
    if(CarpetSettings.superSecretSetting) return;

    NbtCompound tag = new NbtCompound();
    tag.put("EntityHighlighting", new NbtIntArray(ids));

    // System.out.println("SENDING PACKET: " + tag.toString());

    for(ServerPlayerEntity player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.world != world) continue;
      TickHandler.sendNbt(player, tag);
    }
  }

  private static void clearBlockHighlights(ServerWorld world)
  {
    NbtCompound tag = new NbtCompound();
    tag.put("BlockHighlighting", new NbtList());

    for(ServerPlayerEntity player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.world != world) continue;
      TickHandler.sendNbt(player, tag);
    }
  }

  private static void clearEntityHighlights(ServerWorld world)
  {
    if(CarpetSettings.superSecretSetting) return;

    NbtCompound tag = new NbtCompound();
    NbtCompound nbt = new NbtCompound();
    nbt.putInt("color", 0);
    nbt.put("ids", new NbtIntArray(new int[]{}));
    tag.put("EntityHighlighting", nbt);

    for(ServerPlayerEntity player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.world != world) continue;
      TickHandler.sendNbt(player, tag);
    }
  }

  private boolean isExhausted()
  {
    if(stepping == -1) return false;

    boolean noMore = false;
    switch(stepping)
    {
      case TickHandlers.TILE_TICK:
      case TickHandlers.FLUID_TICK:
      case TickHandlers.ENTITY:
      case TickHandlers.BLOCK_ENTITY:
        noMore = exhausted;
        break;
      case TickHandlers.BLOCK_EVENT:
        noMore = handler.world.syncedBlockEventQueue.isEmpty();
        break;
    }
    return noMore;
  }

  public boolean canStep(CommandContext<ServerCommandSource> c, int queue)
  {
    if(!handler.canStep(c, 0, queue)) return false;

    if(handler.current_phase > queue)
    {
      Messenger.m(c.getSource(), handler.getDimension(), t(" cannot queueStep because "), TickHandlers.getPhase(queue), t(" phase already happened"));
      return false;
    }
    if(handler.current_phase < queue)
      return true;

    if(isExhausted())
    {
      Messenger.m(c.getSource(), handler.getDimension(), t(" exhausted "), TickHandlers.getPhase(queue), t(" queue"));
      return false;
    }

    return true;
  }
}
