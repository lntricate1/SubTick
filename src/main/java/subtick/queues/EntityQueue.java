package subtick.queues;

import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.tuple.Pair;
import subtick.TickPhase;

public class EntityQueue extends TickingQueue
{
  private Iterator<Entity> entity_iterator;

  public EntityQueue(ServerLevel level)
  {
    super(level, TickPhase.ENTITY, "entity", "Entity", "Entities");
  }

  @Override
  public void start()
  {
    level.entityTickList.iterated = level.entityTickList.active;
    entity_iterator = level.entityTickList.active.values().iterator();
  }

  @Override
  public Pair<Integer, Boolean> step(int count, BlockPos pos, int range)
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
    return Pair.of(executed_steps, exhausted = !entity_iterator.hasNext());
  }

  @Override
  public void end()
  {
    level.entityTickList.iterated = null;
  }
}
