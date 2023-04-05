package subtick.mixins.lithium;

import java.util.ArrayList;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import me.jellysquid.mods.lithium.common.world.scheduler.LithiumServerTickScheduler;
import me.jellysquid.mods.lithium.common.world.scheduler.TickEntry;
import net.minecraft.world.ScheduledTick;

@Mixin(LithiumServerTickScheduler.class)
public interface LithiumServerTickSchedulerAccessor<T>
{
  @Accessor(value = "executingTicks", remap = false)
  public ArrayList<TickEntry<T>> getExecutingTicks();

  @Accessor(value = "executingTicksSet", remap = false)
  public ObjectOpenHashSet<TickEntry<T>> getExecutingTicksSet();

  @Accessor(value = "tickConsumer", remap = false)
  public Consumer<ScheduledTick<T>> getTickConsumer();
}
