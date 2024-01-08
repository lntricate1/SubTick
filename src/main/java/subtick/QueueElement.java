package subtick;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.material.Fluid;
//#if MC >= 11800
//$$ import net.minecraft.world.ticks.ScheduledTick;
//#else
import me.jellysquid.mods.lithium.common.world.scheduler.TickEntry;
import net.minecraft.world.level.TickNextTickData;
//#endif

public record QueueElement(String label, int x, int y, int z, int depth)
{
  @Override
  public boolean equals(Object o)
  {
    return o instanceof QueueElement element && element.x == x && element.y == y && element.z == z;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(x, y, z);
  }

  public BlockPos blockPos()
  {
    return new BlockPos(x, y, z);
  }

  public QueueElement(String label, BlockPos pos, int depth)
  {
    this(label, pos.getX(), pos.getY(), pos.getZ(), depth);
  }

  public QueueElement(BlockEventData be, int depth)
  {
    //#if MC >= 11800
    //$$ this(be.block().getName().getString(), be.pos(), depth);
    //#else
    this(be.getBlock().getName().getString(), be.getPos(), depth);
    //#endif
  }

  public QueueElement(TickingBlockEntity be)
  {
    this(be.getType(), be.getPos(), 0);
  }

  public QueueElement(Entity e)
  {
    this(e.getName().getString(), e.getId(), 0, 0, 0);
  }

  //#if MC >= 11800
  //$$ public QueueElement(ScheduledTick<?> t)
  //$$ {
  //$$   this(t.type() instanceof Block block ? block.getName().getString() : ((Fluid)t.type()).toString(), t.pos(), t.priority().getValue());
  //$$ }
  //#else
  public QueueElement(TickNextTickData<?> t)
  {
    this(t.getType() instanceof Block block ? block.getName().getString() : ((Fluid)t.getType()).toString(), t.pos, t.priority.getValue());
  }

  public QueueElement(TickEntry<?> t)
  {
    this(t.getType() instanceof Block block ? block.getName().getString() : ((Fluid)t.getType()).toString(), t.pos, t.priority.getValue());
  }
  //#endif
}
