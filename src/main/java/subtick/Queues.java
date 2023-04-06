package subtick;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import carpet.utils.Messenger;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

// highlights
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

// packets
import subtick.mixins.carpet.ServerNetworkHandlerAccessor;
import carpet.CarpetSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;

// tile tick step
import subtick.mixins.lithium.LithiumServerTickSchedulerAccessor;
import me.jellysquid.mods.lithium.common.world.scheduler.LithiumServerTickScheduler;
import me.jellysquid.mods.lithium.common.world.scheduler.TickEntry;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
// block event step
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
// entity step
import net.minecraft.world.entity.Entity;

import static subtick.SubTick.t;
import static subtick.SubTick.n;
import static subtick.SubTick.p;
import static subtick.SubTick.d;

public class Queues
{
  private final TickHandler handler;
  public CommandSourceStack commandSource;

  public TickPhase stepping = TickPhase.UNKNOWN;
  public TickPhase scheduled = TickPhase.UNKNOWN;
  private int scheduled_count = 0;
  private boolean exhausted;
  private BlockPos pos = new BlockPos(0, 0, 0);
  private int range = -1;

  // Queues
  private int lithium_tile_tick_step_index = 0;
  private Iterator<Entity> entity_iterator = null;
  private Iterator<TickingBlockEntity> block_entity_iterator = null;

  public Queues(TickHandler handler)
  {
    this.handler = handler;
  }

  public void scheduleQueueStep(TickPhase phase, int count, BlockPos pos, int range)
  {
    if(phase.isPriorTo(handler.current_phase) || isExhausted())
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
    if(scheduled != TickPhase.UNKNOWN)
    {
      switch(scheduled)
      {
        case BLOCK_TICK:
          if(handler.level.blockTicks instanceof LithiumServerTickScheduler)
            stepScheduledTicksLithium((LithiumServerTickScheduler<Block>)handler.level.blockTicks, scheduled_count);
          else
            stepScheduledTicks(handler.level.blockTicks, scheduled_count);
          break;
        case FLUID_TICK:
          if(handler.level.liquidTicks instanceof LithiumServerTickScheduler)
            stepScheduledTicksLithium((LithiumServerTickScheduler<Fluid>)handler.level.liquidTicks, scheduled_count);
          else
            stepScheduledTicks(handler.level.liquidTicks, scheduled_count);
          break;
        case BLOCK_EVENT:
          stepBlockEvents(scheduled_count);
          break;
        case ENTITY:
          stepEntities(scheduled_count);
          break;
        case BLOCK_ENTITY:
          stepBlockEntities(scheduled_count);
          break;
      }
      scheduled = TickPhase.UNKNOWN;
    }
  }

  private static int squaredDistance(BlockPos a, BlockPos b)
  {
    int x = a.getX() - b.getX();
    int y = a.getY() - b.getY();
    int z = a.getZ() - b.getZ();
    return x*x + y*y + z*z;
  }

  private static void addOutlines(BlockPos pos, List<AABB> aabbs, ServerLevel level)
  {
    List<AABB> outlineAabbs = level.getBlockState(pos).getShape(level, pos).toAabbs();
    if(outlineAabbs.isEmpty())
      aabbs.add(new AABB(pos));
    else
    {
      outlineAabbs.replaceAll((box) -> box.move(pos));
      aabbs.addAll(outlineAabbs);
    }
  }

  public <T> void stepScheduledTicksLithium(LithiumServerTickScheduler<T> scheduler, int count)
  {
    if(stepping == TickPhase.UNKNOWN)
    {
      stepping = handler.current_phase;
      scheduler.selectTicks(handler.time);
      lithium_tile_tick_step_index = 0;
    }

    ArrayList<TickEntry<T>> ticks = ((LithiumServerTickSchedulerAccessor<T>)scheduler).getExecutingTicks();
    ArrayList<AABB> aabbs = new ArrayList<>();
    int i = 0;
    int ticksSize = ticks.size();
    for(; lithium_tile_tick_step_index < ticksSize && i < count; lithium_tile_tick_step_index++)
    {
      TickEntry<T> tick = ticks.get(lithium_tile_tick_step_index);
      if(tick == null)
        continue;
      tick.consumed = true;
      ((LithiumServerTickSchedulerAccessor<T>)scheduler).getTickConsumer().accept(tick);
      if(range == -1 || squaredDistance(tick.pos, pos) <= range)
      {
        addOutlines(tick.pos, aabbs, handler.level);
        i ++;
      }
    }
    exhausted = lithium_tile_tick_step_index == ticksSize;
    sendBlockHighlights(aabbs, handler.level);
    if(range != -1)
      sendFeedback(i);
  }

  public <T> void stepScheduledTicks(ServerTickList<T> tickList, int count)
  {
    if(stepping == TickPhase.UNKNOWN)
    {
      stepping = handler.current_phase;
      Iterator<TickNextTickData<T>> iterator = tickList.tickNextTickList.iterator();

      for(int i = 0; i < 65536 && iterator.hasNext();)
      {
        TickNextTickData<T> tick = iterator.next();
        if(tick.triggerTick > handler.level.getGameTime())
          break;
        if(handler.level.isPositionTickingWithEntitiesLoaded(tick.pos))
        {
          iterator.remove();
          tickList.tickNextTickSet.remove(tick);
          tickList.currentlyTicking.add(tick);
          i ++;
        }
      }
    }

    ArrayList<AABB> aabbs = new ArrayList<>();
    int i = 0;
    while(i < count)
    {
      TickNextTickData<T> tick = tickList.currentlyTicking.poll();
      if(tick == null)
      {
        exhausted = true;
        break;
      }

      if(handler.level.isPositionTickingWithEntitiesLoaded(tick.pos))
      {
        tickList.alreadyTicked.add(tick);
        tickList.ticker.accept(tick);
      }
      else
        tickList.scheduleTick(tick.pos, tick.getType(), 0);

      if(range == -1 || squaredDistance(tick.pos, pos) <= range)
      {
        addOutlines(tick.pos, aabbs, handler.level);
        i ++;
      }
    }

    sendBlockHighlights(aabbs, handler.level);
    if(range != -1)
      sendFeedback(i);
  }

  private void stepBlockEvents(int count)
  {
    stepping = TickPhase.BLOCK_EVENT;
    ArrayList<AABB> aabbs = new ArrayList<>();
    int i = 0;
    while(i < count && handler.level.blockEvents.size() != 0)
    {
      BlockEventData blockEvent = handler.level.blockEvents.removeFirst();
      if(handler.level.doBlockEvent(blockEvent))
      {
        handler.level.server.getPlayerList().broadcast(null, blockEvent.getPos().getX(), blockEvent.getPos().getY(), blockEvent.getPos().getZ(), 64.0D, handler.level.dimension(), new ClientboundBlockEventPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getParamA(), blockEvent.getParamB()));

        if(range == -1 || squaredDistance(blockEvent.getPos(), pos) <= range)
        {
          addOutlines(blockEvent.getPos(), aabbs, handler.level);
          i ++;
        }
      }
    }

    sendBlockHighlights(aabbs, handler.level);
    if(range != -1)
      sendFeedback(i);
  }

  private void stepEntities(int count)
  {
    if(stepping == TickPhase.UNKNOWN)
    {
      handler.level.entityTickList.iterated = handler.level.entityTickList.active;
      entity_iterator = handler.level.entityTickList.active.values().iterator();
      stepping = TickPhase.ENTITY;
    }

    ArrayList<Integer> ids = new ArrayList<>();
    int i = 0;
    while(i < count && entity_iterator.hasNext())
    {
      Entity entity = entity_iterator.next();
      if(entity.isRemoved()) continue;

      if(handler.level.shouldDiscardEntity(entity))
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

        handler.level.guardEntityTick(handler.level::tickNonPassenger, entity);

      }
      if(range == -1 || squaredDistance(entity.getOnPos(), pos) <= range)
      {
        ids.add(entity.getId());
        i ++;
      }
    }
    exhausted = !entity_iterator.hasNext();

    if(!ids.isEmpty())
      sendEntityHighlights(ids, handler.level);
    if(range != -1)
      sendFeedback(i);
  }

  private void stepBlockEntities(int count)
  {
    stepping = TickPhase.BLOCK_ENTITY;
    if(!handler.level.tickingBlockEntities)
    {
      handler.level.tickingBlockEntities = true;
      if(!handler.level.pendingBlockEntityTickers.isEmpty())
      {
        handler.level.blockEntityTickers.addAll(handler.level.pendingBlockEntityTickers);
        handler.level.pendingBlockEntityTickers.clear();
      }

      block_entity_iterator = handler.level.blockEntityTickers.iterator();
    }

    ArrayList<AABB> aabbs = new ArrayList<>();
    int i = 0;
    while(i < count && block_entity_iterator.hasNext())
    {
      TickingBlockEntity ticker = block_entity_iterator.next();
      if(range == -1 || squaredDistance(ticker.getPos(), pos) <= range)
      {
        addOutlines(ticker.getPos(), aabbs, handler.level);
        i ++;
      }

      if(ticker.isRemoved())
        block_entity_iterator.remove();
      else
        ticker.tick();
    }
    exhausted = !block_entity_iterator.hasNext();

    sendBlockHighlights(aabbs, handler.level);
    if(range != -1)
      sendFeedback(i);
  }

  public void sendFeedback(int count)
  {
    if(count == 0)
      Messenger.m(commandSource, d(handler.level), t(" "), p(stepping), t(" queue exhausted"));
    else if(count != scheduled_count || isExhausted())
      Messenger.m(commandSource, d(handler.level), t(" stepped"), n(" " + count + " "), p(stepping, count), t(" (queue exhausted)"));
    else
      Messenger.m(commandSource, d(handler.level), t(" stepped"), n(" " + count + " "), p(stepping, count));
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
    if(stepping == TickPhase.BLOCK_TICK)
    {
      range = -1;
      stepScheduledTicks(handler.level.blockTicks, 2147483647);
      if(handler.level.blockTicks instanceof LithiumServerTickScheduler)
      {
        ((LithiumServerTickSchedulerAccessor<?>)handler.level.blockTicks).getExecutingTicks().clear();
        ((LithiumServerTickSchedulerAccessor<?>)handler.level.blockTicks).getExecutingTicksSet().clear();
      }
      else
      {
        handler.level.blockTicks.alreadyTicked.clear();
        handler.level.blockTicks.currentlyTicking.clear();
      }
      stepping = TickPhase.UNKNOWN;
      clearBlockHighlights(handler.level);
    }
  }

  private void finishStepFluidTicks()
  {
    if(stepping == TickPhase.FLUID_TICK)
    {
      range = -1;
      stepScheduledTicks(handler.level.liquidTicks, 2147483647);
      if(handler.level.liquidTicks instanceof LithiumServerTickScheduler)
      {
        ((LithiumServerTickSchedulerAccessor<?>)handler.level.liquidTicks).getExecutingTicks().clear();
        ((LithiumServerTickSchedulerAccessor<?>)handler.level.liquidTicks).getExecutingTicksSet().clear();
      }
      else
      {
        handler.level.liquidTicks.alreadyTicked.clear();
        handler.level.liquidTicks.currentlyTicking.clear();
      }
      stepping = TickPhase.UNKNOWN;
      clearBlockHighlights(handler.level);
    }
  }

  private void finishStepBlockEvents()
  {
    if(stepping == TickPhase.BLOCK_EVENT)
    {
      stepping = TickPhase.UNKNOWN;
      clearBlockHighlights(handler.level);
    }
  }

  private void finishStepEntities()
  {
    if(stepping == TickPhase.ENTITY)
    {
      range = -1;
      stepEntities(2147483647);
      handler.level.entityTickList.iterated = null;
      stepping = TickPhase.UNKNOWN;
      handler.advancePhase();
      clearEntityHighlights(handler.level);
    }
  }

  private void finishStepBlockEntities()
  {
    if(stepping == TickPhase.BLOCK_ENTITY)
    {
      range = -1;
      stepBlockEntities(2147483647);
      handler.level.tickingBlockEntities = false;
      stepping = TickPhase.UNKNOWN;
      handler.advancePhase();
      clearBlockHighlights(handler.level);
    }
  }

  private static void sendBlockHighlights(List<AABB> aabbs, ServerLevel level)
  {
    if(CarpetSettings.superSecretSetting || aabbs.isEmpty()) return;

    CompoundTag tag = new CompoundTag();
    ListTag list = new ListTag();
    for(AABB aabb : aabbs)
    {
      CompoundTag nbt = new CompoundTag();
      nbt.putDouble("x", aabb.minX);
      nbt.putDouble("y", aabb.minY);
      nbt.putDouble("z", aabb.minZ);
      nbt.putDouble("X", aabb.maxX);
      nbt.putDouble("Y", aabb.maxY);
      nbt.putDouble("Z", aabb.maxZ);
      nbt.putDouble("color", Settings.subtickHighlightColor);
      list.add(nbt);
    }
    tag.put("BlockHighlighting", list);

    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.level != level) continue;
      TickHandler.sendNbt(player, tag);
    }
  }

  private static void sendEntityHighlights(List<Integer> ids, ServerLevel level)
  {
    if(CarpetSettings.superSecretSetting) return;

    CompoundTag tag = new CompoundTag();
    tag.put("EntityHighlighting", new IntArrayTag(ids));

    // System.out.println("SENDING PACKET: " + tag.toString());

    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.level != level) continue;
      TickHandler.sendNbt(player, tag);
    }
  }

  private static void clearBlockHighlights(ServerLevel level)
  {
    CompoundTag tag = new CompoundTag();
    tag.put("BlockHighlighting", new ListTag());

    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.level != level) continue;
      TickHandler.sendNbt(player, tag);
    }
  }

  private static void clearEntityHighlights(ServerLevel level)
  {
    if(CarpetSettings.superSecretSetting) return;

    CompoundTag tag = new CompoundTag();
    CompoundTag nbt = new CompoundTag();
    nbt.putInt("color", 0);
    nbt.put("ids", new IntArrayTag(new int[]{}));
    tag.put("EntityHighlighting", nbt);

    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.level != level) continue;
      TickHandler.sendNbt(player, tag);
    }
  }

  private boolean isExhausted()
  {
    if(stepping == TickPhase.UNKNOWN) return false;

    boolean noMore = false;
    switch(stepping)
    {
      case BLOCK_TICK:
      case FLUID_TICK:
      case ENTITY:
      case BLOCK_ENTITY:
        noMore = exhausted;
        break;
      case BLOCK_EVENT:
        noMore = handler.level.blockEvents.isEmpty();
        break;
    }
    return noMore;
  }

  public boolean canStep(CommandContext<CommandSourceStack> c, TickPhase queue)
  {
    if(!handler.canStep(c, 0, queue)) return false;

    if(handler.current_phase.isPosteriorTo(queue))
    {
      Messenger.m(c.getSource(), d(handler.level), t(" cannot queueStep because "), p(queue), t(" phase already happened"));
      return false;
    }
    if(handler.current_phase.isPriorTo(queue))
      return true;

    if(isExhausted())
    {
      Messenger.m(c.getSource(), d(handler.level), t(" exhausted "), p(queue), t(" queue"));
      return false;
    }

    return true;
  }
}
