package subtick;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;

// entity step
import it.unimi.dsi.fastutil.objects.ObjectIterator;
// block entity step
import java.util.Iterator;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

public class TickHandler
{
  public final ServerWorld world;

  public final String dimensionName;
  public boolean frozen = false;
  private boolean freezing = false;
  private boolean unfreezing = false;
  public boolean stepping = false;
  private boolean in_first_stepped_phase = false;
  private int remaining_ticks = 0;
  public int current_phase = 0;
  private int target_phase = 7;

  public boolean scheduled_block_event_step = false;
  private int scheduled_block_event_step_count = 0;

  public ObjectIterator entity_iterator = null;
  public boolean scheduled_entity_step = false;
  private int scheduled_entity_step_count = 0;

  public Iterator block_entity_iterator = null;
  public boolean scheduled_block_entity_step = false;
  private int scheduled_block_entity_step_count = 0;

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
      return true;
    }

    // Return false if frozen and not stepping
    if(!stepping || phase != current_phase) return false;

    // Stepping
    if(phase == 0 && !in_first_stepped_phase) --remaining_ticks;
    if(remaining_ticks < 1 && phase == target_phase)
    {
      stepping = false;
      if(scheduled_block_event_step)
      {
        stepBlockEvents(scheduled_block_event_step_count);
        scheduled_block_event_step = false;
      }
      if(scheduled_entity_step)
      {
        stepEntities(scheduled_entity_step_count);
        scheduled_entity_step = false;
      }
      if(scheduled_block_entity_step)
      {
        stepBlockEntities(scheduled_block_entity_step_count);
        scheduled_block_entity_step = false;
      }
      return false;
    }
    in_first_stepped_phase = false;
    current_phase = (current_phase + 1) % TickHandlers.TOTAL_PHASES;
    return true;
  }

  public void step(int ticks, int phase)
  {
    stepping = true;
    in_first_stepped_phase = true;
    remaining_ticks = ticks;
    target_phase = phase;
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
      finishStepEntities();
      finishStepBlockEntities();
    }
  }

  public void scheduleBlockEventStep(int count)
  {
    scheduled_block_event_step = true;
    scheduled_block_event_step_count = count;
  }

  public void scheduleEntityStep(int count)
  {
    scheduled_entity_step = true;
    scheduled_entity_step_count = count;
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
    if(entity_iterator == null)
      entity_iterator = world.entityList.entities.values().iterator();

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

  public void finishStepEntities()
  {
    if(entity_iterator != null)
    {
      stepEntities(2147483647);
      entity_iterator = null;
      current_phase ++;
    }
  }

  public void scheduleBlockEntityStep(int count)
  {
    scheduled_block_entity_step = true;
    scheduled_block_entity_step_count = count;
  }

  public void stepBlockEntities(int count)
  {
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

  public void finishStepBlockEntities()
  {
    if(block_entity_iterator != null)
    {
      stepBlockEntities(2147483647);
      world.iteratingTickingBlockEntities = false;
      block_entity_iterator = null;
      current_phase ++;
    }
  }
}
