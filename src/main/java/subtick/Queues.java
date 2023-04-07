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

// tile tick step
import subtick.mixins.lithium.LithiumServerTickSchedulerAccessor;
import subtick.network.ServerNetworkHandler;
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
import static subtick.SubTick.err;

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
  private int executed_steps = 0;
  private ArrayList<AABB> block_highlights = new ArrayList<>();
  private ArrayList<Integer> entity_highlights = new ArrayList<>();

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
    if(scheduled == TickPhase.UNKNOWN) return;

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
    stepping = scheduled;
    if(block_highlights.size() > 0)
      ServerNetworkHandler.sendBlockHighlights(block_highlights, handler.level, commandSource);
    if(entity_highlights.size() > 0)
      ServerNetworkHandler.sendEntityHighlights(entity_highlights, handler.level, commandSource);
    block_highlights = new ArrayList<>();
    entity_highlights = new ArrayList<>();

    sendFeedback();
    scheduled = TickPhase.UNKNOWN;
  }

  private static int squaredDistance(BlockPos a, BlockPos b)
  {
    int x = a.getX() - b.getX();
    int y = a.getY() - b.getY();
    int z = a.getZ() - b.getZ();
    return x*x + y*y + z*z;
  }

  private void addOutlines(BlockPos pos, ServerLevel level)
  {
    List<AABB> outlineAabbs = level.getBlockState(pos).getShape(level, pos).toAabbs();
    if(outlineAabbs.isEmpty())
      block_highlights.add(new AABB(pos));
    else
    {
      outlineAabbs.replaceAll((box) -> box.move(pos));
      block_highlights.addAll(outlineAabbs);
    }
  }

  public <T> void stepScheduledTicksLithium(LithiumServerTickScheduler<T> scheduler, int count)
  {
    if(stepping == TickPhase.UNKNOWN)
    {
      scheduler.selectTicks(handler.time);
      lithium_tile_tick_step_index = 0;
    }

    ArrayList<TickEntry<T>> ticks = ((LithiumServerTickSchedulerAccessor<T>)scheduler).getExecutingTicks();
    int ticksSize = ticks.size();
    for(; lithium_tile_tick_step_index < ticksSize && executed_steps < count; lithium_tile_tick_step_index++)
    {
      TickEntry<T> tick = ticks.get(lithium_tile_tick_step_index);
      if(tick == null)
        continue;
      tick.consumed = true;
      ((LithiumServerTickSchedulerAccessor<T>)scheduler).getTickConsumer().accept(tick);
      if(range == -1 || squaredDistance(tick.pos, pos) <= range)
      {
        addOutlines(tick.pos, handler.level);
        executed_steps ++;
      }
    }
    exhausted = lithium_tile_tick_step_index == ticksSize;
  }

  public <T> void stepScheduledTicks(ServerTickList<T> tickList, int count)
  {
    if(stepping == TickPhase.UNKNOWN)
    {
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

    while(executed_steps < count)
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
        addOutlines(tick.pos, handler.level);
        executed_steps ++;
      }
    }
  }

  private void stepBlockEvents(int count)
  {

    while(executed_steps < count && handler.level.blockEvents.size() != 0)
    {
      BlockEventData blockEvent = handler.level.blockEvents.removeFirst();
      if(handler.level.doBlockEvent(blockEvent))
      {
        handler.level.server.getPlayerList().broadcast(null, blockEvent.getPos().getX(), blockEvent.getPos().getY(), blockEvent.getPos().getZ(), 64.0D, handler.level.dimension(), new ClientboundBlockEventPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getParamA(), blockEvent.getParamB()));

        if(range == -1 || squaredDistance(blockEvent.getPos(), pos) <= range)
        {
          addOutlines(blockEvent.getPos(), handler.level);
          executed_steps ++;
        }
      }
    }
  }

  private void stepEntities(int count)
  {
    if(stepping == TickPhase.UNKNOWN)
    {
      handler.level.entityTickList.iterated = handler.level.entityTickList.active;
      entity_iterator = handler.level.entityTickList.active.values().iterator();
    }

    while(executed_steps < count && entity_iterator.hasNext())
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
        entity_highlights.add(entity.getId());
        executed_steps ++;
      }
    }
    exhausted = !entity_iterator.hasNext();
  }

  private void stepBlockEntities(int count)
  {
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

    while(executed_steps < count && block_entity_iterator.hasNext())
    {
      TickingBlockEntity ticker = block_entity_iterator.next();
      if(range == -1 || squaredDistance(ticker.getPos(), pos) <= range)
      {
        addOutlines(ticker.getPos(), handler.level);
        executed_steps ++;
      }

      if(ticker.isRemoved())
        block_entity_iterator.remove();
      else
        ticker.tick();
    }
    exhausted = !block_entity_iterator.hasNext();
  }

  public void sendFeedback()
  {
    if(executed_steps == 0)
      Messenger.m(commandSource, d(handler.level), err(" "), p(stepping), err(" queue exhausted"));
    else if(executed_steps != scheduled_count || isExhausted())
      Messenger.m(commandSource, d(handler.level), t(" stepped"), n(" " + executed_steps + " "), p(stepping, executed_steps), t(" (queue exhausted)"));
    else
      Messenger.m(commandSource, d(handler.level), t(" stepped"), n(" " + executed_steps + " "), p(stepping, executed_steps));

    executed_steps = 0;
  }

  public void finishQueueStep()
  {
    if(stepping == TickPhase.UNKNOWN)
      return;

    range = -1;
    switch(stepping)
    {
      case BLOCK_TICK:
        finishStepTileTicks();
        break;
      case FLUID_TICK:
        finishStepFluidTicks();
        break;
      case ENTITY:
        finishStepEntities();
        break;
      case BLOCK_ENTITY:
        finishStepBlockEntities();
        break;
    }
    block_highlights = new ArrayList<>();
    entity_highlights = new ArrayList<>();
    executed_steps = 0;
    exhausted = false;
    stepping = TickPhase.UNKNOWN;
  }

  private void finishStepTileTicks()
  {
    if(handler.level.blockTicks instanceof LithiumServerTickScheduler)
    {
      stepScheduledTicksLithium((LithiumServerTickScheduler<Block>)handler.level.blockTicks, 2147483647);
      ((LithiumServerTickSchedulerAccessor<?>)handler.level.blockTicks).getExecutingTicks().clear();
      ((LithiumServerTickSchedulerAccessor<?>)handler.level.blockTicks).getExecutingTicksSet().clear();
    }
    else
    {
      stepScheduledTicks(handler.level.blockTicks, 2147483647);
      handler.level.blockTicks.alreadyTicked.clear();
      handler.level.blockTicks.currentlyTicking.clear();
    }

    ServerNetworkHandler.clearBlockHighlights(handler.level);
    handler.advancePhase();
  }

  private void finishStepFluidTicks()
  {
    stepScheduledTicks(handler.level.liquidTicks, 2147483647);
    if(handler.level.liquidTicks instanceof LithiumServerTickScheduler)
    {
      stepScheduledTicksLithium((LithiumServerTickScheduler<Fluid>)handler.level.liquidTicks, 2147483647);
      ((LithiumServerTickSchedulerAccessor<?>)handler.level.liquidTicks).getExecutingTicks().clear();
      ((LithiumServerTickSchedulerAccessor<?>)handler.level.liquidTicks).getExecutingTicksSet().clear();
    }
    else
    {
      stepScheduledTicks(handler.level.liquidTicks, 2147483647);

      handler.level.liquidTicks.alreadyTicked.clear();
      handler.level.liquidTicks.currentlyTicking.clear();
    }

    ServerNetworkHandler.clearBlockHighlights(handler.level);
    handler.advancePhase();
  }

  private void finishStepEntities()
  {
    stepEntities(2147483647);
    handler.level.entityTickList.iterated = null;

    ServerNetworkHandler.clearEntityHighlights(handler.level);
    handler.advancePhase();
  }

  private void finishStepBlockEntities()
  {
    stepBlockEntities(2147483647);
    handler.level.tickingBlockEntities = false;

    ServerNetworkHandler.clearBlockHighlights(handler.level);
    handler.advancePhase();
  }

  public boolean isExhausted()
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

      Messenger.m(c.getSource(), d(handler.level), err(" cannot queueStep because "), p(queue), err(" phase already happened"));
      return false;
    }
    if(handler.current_phase.isPriorTo(queue))
      return true;

    if(isExhausted())
    {
      Messenger.m(c.getSource(), d(handler.level), err(" exhausted "), p(queue), err(" queue"));
      return false;
    }

    return true;
  }
}
