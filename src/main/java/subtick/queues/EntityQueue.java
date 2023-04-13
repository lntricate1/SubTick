package subtick.queues;

import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import oshi.util.tuples.Pair;
import subtick.TickHandler;
import subtick.TickingMode;

public class EntityQueue extends TickingQueue
{
  private Iterator<Entity> entity_iterator;

  public EntityQueue(TickHandler handler)
  {
    super(handler);
  }

  @Override
  public void start(TickingMode mode)
  {
    level.entityTickList.iterated = level.entityTickList.active;
    entity_iterator = level.entityTickList.active.values().iterator();
  }

  @Override
  public Pair<Integer, Boolean> step(TickingMode mode, int count, BlockPos pos, int range)
  {
    int executed_steps = 0;
    while(executed_steps < count && entity_iterator.hasNext())
    {
      Entity entity = entity_iterator.next();
      if(entity.isRemoved()) continue;

      if(level.shouldDiscardEntity(entity))
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

        level.guardEntityTick(level::tickNonPassenger, entity);
      }
      if(rangeCheck(entity.blockPosition(), pos, range))
      {
        addEntityHighlight(entity.getId());
        executed_steps ++;
      }
    }
    return new Pair<Integer, Boolean>(executed_steps, exhausted = !entity_iterator.hasNext());
  }

  @Override
  public void end(TickingMode mode)
  {
    level.entityTickList.iterated = null;
  }

  @Override
  public String getName(TickingMode mode, int steps)
  {
    return steps == 1 ? "entity" : "entities";
  }
}
