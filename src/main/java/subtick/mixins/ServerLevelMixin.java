package subtick.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;

import subtick.TickHandlers;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

// Level constructor
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import java.util.function.Supplier;
import net.minecraft.util.profiling.ProfilerFiller;

// ServerLevel constructor
import net.minecraft.server.MinecraftServer;
import java.util.concurrent.Executor;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.chunk.ChunkGenerator;
import java.util.List;
import net.minecraft.world.level.CustomSpawner;

// world border
import net.minecraft.world.level.border.WorldBorder;
// weather
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.network.protocol.Packet;
// tile tick
import net.minecraft.world.level.ServerTickList;
// raid
import net.minecraft.world.entity.raid.Raids;
// chunk
import net.minecraft.server.level.ServerChunkCache;
import java.util.function.BooleanSupplier;
import com.google.common.collect.Lists;
import java.util.Optional;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ChunkHolder;
// entity
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import java.util.function.Consumer;
// entity management
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level
{
  public ServerLevelMixin(WritableLevelData properties, ResourceKey<Level> registryRef, final DimensionType dimensionType, Supplier<ProfilerFiller> profiler, boolean isClient, boolean debugWorld, long seed)
  {
    super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
  }

  @Inject(method = "tickTime", at = @At("HEAD"))
  private void tickTime(CallbackInfo ci)
  {
    TickHandlers.getHandler(dimension()).tickTime();
  }

  private boolean tickingWeather = false;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void registerTickHandler(MinecraftServer server, Executor workerExecutor, LevelStorageAccess session, ServerLevelData properties, ResourceKey<Level> worldKey, DimensionType dimensionType, ChunkProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean debugWorld, long seed, List<CustomSpawner> CustomSpawners, boolean shouldTickTime, CallbackInfo ci)
  {
    TickHandlers.addLevel(worldKey, (ServerLevel)(Object)this);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;tick()V"))
  private boolean worldBorder(WorldBorder self)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.WORLD_BORDER);
  }

  // BEGIN WEATHER --------------------------------------------------------------------------------------------------------------------------
  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;hasSkyLight()Z"))
  private boolean weather1(boolean original)
  {
    if(TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.WEATHER))
    {
      tickingWeather = true;
      return original;
    }
    return false;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/resources/ResourceKey;)V"))
  private boolean weather2(PlayerList self, Packet<?> packet, ResourceKey<Level> key)
  {
    return tickingWeather;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
  private boolean weather3(PlayerList self, Packet<?> packet)
  {
    return tickingWeather;
  }

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"))
  private boolean weather4(boolean original)
  {
    return tickingWeather && original;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateSkyBrightness()V"))
  private boolean weather5(ServerLevel self)
  {
    if(tickingWeather)
    {
      tickingWeather = false;
      return true;
    }
    return false;
  }
  // END WEATHER

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"))
  private boolean time(ServerLevel self)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.TIME);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerTickList;tick()V", ordinal = 0))
  private boolean blockTick(ServerTickList<Block> self)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.TILE_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerTickList;tick()V", ordinal = 1))
  private boolean fluidTick(ServerTickList<Fluid> self)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.FLUID_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"))
  private boolean blockTick(Raids self)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.RAID);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;tick(Ljava/util/function/BooleanSupplier;)V"))
  private boolean chunk(ServerChunkCache self, BooleanSupplier hasTimeLeft)
  {
    if(TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.CHUNK))
      return true;

    // Send chunk updates and entity updates to clients
    for(ChunkHolder holder : Lists.newArrayList(self.chunkMap.getChunks()))
    {
      Optional<LevelChunk> optional = holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left();
      if(optional.isPresent())
        holder.broadcastChanges(optional.get());
    }
    self.chunkMap.tick();
    return false;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
  private boolean blockEvent(ServerLevel self)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.BLOCK_EVENT);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
  private boolean entity(EntityTickList self, Consumer<Entity> action)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.ENTITY);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickBlockEntities()V"))
  private boolean blockEntity(ServerLevel self)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.BLOCK_ENTITY);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;tick()V"))
  private boolean entityManagement(PersistentEntitySectionManager<Entity> self)
  {
    return TickHandlers.getHandler(dimension()).shouldTick(TickHandlers.ENTITY_MANAGEMENT);
  }
}
