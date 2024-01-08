package subtick.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;

import subtick.Queues;
import subtick.TickHandler;
import subtick.TickPhase;
import net.minecraft.server.level.ServerLevel;

// world border
import net.minecraft.world.level.border.WorldBorder;
// tile tick
import net.minecraft.world.ticks.LevelTicks;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
// raid
import net.minecraft.world.entity.raid.Raids;
// chunk
import net.minecraft.server.level.ServerChunkCache;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ChunkHolder;
import com.google.common.collect.Lists;
import java.util.Optional;
import net.minecraft.world.level.chunk.LevelChunk;
// entity
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import java.util.function.Consumer;
// entity management
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

@Mixin(ServerLevel.class)
public class ServerLevelMixin
{
  @Inject(method = "blockEvent", at = @At("TAIL"))
  private void blockEvent(BlockPos blockPos, Block block, int i, int j, CallbackInfo ci)
  {
    if(TickHandler.frozen())
      Queues.onScheduleBlockEvent((ServerLevel)(Object)this, new BlockEventData(blockPos, block, i, j));
  }

  private boolean tickingWeather = false;

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;tick()V"))
  private boolean worldBorder(WorldBorder self)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.WORLD_BORDER);
  }

  // BEGIN WEATHER --------------------------------------------------------------------------------------------------------------------------
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;advanceWeatherCycle()V"))
  private boolean weather1(ServerLevel self)
  {
    if(TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.WEATHER))
    {
      tickingWeather = true;
      return true;
    }
    tickingWeather = false;
    return false;
  }

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"))
  private boolean weather2(boolean original)
  {
    return tickingWeather && original;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateSkyBrightness()V"))
  private boolean weather3(ServerLevel self)
  {
    return tickingWeather;
  }
  // END WEATHER --------------------------------------------------------------------------------------------------------------------------

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"))
  private boolean time(ServerLevel self)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.TIME);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;tick(JILjava/util/function/BiConsumer;)V", ordinal = 0))
  private boolean blockTick(LevelTicks<Block> self, long l, int i, BiConsumer<BlockPos, Block> biConsumer)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;tick(JILjava/util/function/BiConsumer;)V", ordinal = 1))
  private boolean fluidTick(LevelTicks<Fluid> self, long l, int i, BiConsumer<BlockPos, Fluid> biConsumer)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.FLUID_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"))
  private boolean blockTick(Raids self)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.RAID);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;tick(Ljava/util/function/BooleanSupplier;Z)V"))
  private boolean chunk(ServerChunkCache self, BooleanSupplier hasTimeLeft, boolean bool)
  {
    if(TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.CHUNK))
      return true;

    // Send chunk updates and entity updates to clients
    for(ChunkHolder holder : Lists.newArrayList(self.chunkMap.getChunks()))
    {
      Optional<LevelChunk> optional = holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left();
      if(optional.isPresent())
        holder.broadcastChanges(optional.get());
    }
    return false;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
  private boolean blockEvent(ServerLevel self)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_EVENT);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
  private boolean entity(EntityTickList self, Consumer<Entity> action)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.ENTITY);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickBlockEntities()V"))
  private boolean blockEntity(ServerLevel self)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.BLOCK_ENTITY);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;tick()V"))
  private boolean entityManagement(PersistentEntitySectionManager<Entity> self)
  {
    return TickHandler.shouldTick((ServerLevel)(Object)this, TickPhase.ENTITY_MANAGEMENT);
  }
}
