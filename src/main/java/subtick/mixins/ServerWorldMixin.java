package subtick.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;

import subtick.TickHandlers;

import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

// World constructor
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;
import java.util.function.Supplier;
import net.minecraft.util.profiler.Profiler;

// ServerWorld constructor
import net.minecraft.server.MinecraftServer;
import java.util.concurrent.Executor;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import java.util.List;
import net.minecraft.world.gen.Spawner;

// world border
import net.minecraft.world.border.WorldBorder;
// weather
import net.minecraft.server.PlayerManager;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.network.Packet;
// tile tick
import net.minecraft.server.world.ServerTickScheduler;
// raid
import net.minecraft.village.raid.RaidManager;
// chunk
import net.minecraft.server.world.ServerChunkManager;
import java.util.function.BooleanSupplier;
import com.google.common.collect.Lists;
import java.util.Optional;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.server.world.ChunkHolder;
// entity
import net.minecraft.world.EntityList;
import java.util.function.Consumer;
// entity management
import net.minecraft.server.world.ServerEntityManager;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World
{
  public ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, final DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed)
  {
    super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
  }

  @Inject(method = "tickTime", at = @At("HEAD"))
  private void tickTime(CallbackInfo ci)
  {
    TickHandlers.getHandler(getRegistryKey()).tickTime();
  }

  private boolean tickingWeather = false;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void registerTickHandler(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> worldKey, DimensionType dimensionType, WorldGenerationProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean debugWorld, long seed, List<Spawner> spawners, boolean shouldTickTime, CallbackInfo ci)
  {
    TickHandlers.addWorld(worldKey, (ServerWorld)(Object)this);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;tick()V"))
  private boolean worldBorder(WorldBorder self)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.WORLD_BORDER);
  }

  // BEGIN WEATHER --------------------------------------------------------------------------------------------------------------------------
  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/DimensionType;hasSkyLight()Z"))
  private boolean weather1(boolean original)
  {
    if(TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.WEATHER))
    {
      tickingWeather = true;
      return original;
    }
    return false;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToDimension(Lnet/minecraft/network/Packet;Lnet/minecraft/util/registry/RegistryKey;)V"))
  private boolean weather2(PlayerManager self, Packet packet, RegistryKey key)
  {
    return tickingWeather;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/Packet;)V"))
  private boolean weather3(PlayerManager self, Packet packet)
  {
    return tickingWeather;
  }

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/SleepManager;canSkipNight(I)Z"))
  private boolean weather4(boolean original)
  {
    return tickingWeather && original;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;calculateAmbientDarkness()V"))
  private boolean weather5(ServerWorld self)
  {
    if(tickingWeather)
    {
      tickingWeather = false;
      return true;
    }
    return false;
  }
  // END WEATHER

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;tickTime()V"))
  private boolean time(ServerWorld self)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.TIME);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerTickScheduler;tick()V", ordinal = 0))
  private boolean blockTick(ServerTickScheduler<Block> self)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.TILE_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerTickScheduler;tick()V", ordinal = 1))
  private boolean fluidTick(ServerTickScheduler<Fluid> self)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.FLUID_TICK);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/village/raid/RaidManager;tick()V"))
  private boolean blockTick(RaidManager self)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.RAID);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;tick(Ljava/util/function/BooleanSupplier;)V"))
  private boolean chunk(ServerChunkManager self, BooleanSupplier bs)
  {
    if(TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.CHUNK))
      return true;

    // Send chunk updates and entity updates to clients
    for(ChunkHolder holder : Lists.newArrayList(self.threadedAnvilChunkStorage.entryIterator()))
    {
      Optional<WorldChunk> optional = holder.getTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left();
      if(optional.isPresent())
        holder.flushUpdates(optional.get());
    }
    self.threadedAnvilChunkStorage.tickEntityMovement();
    return false;
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;processSyncedBlockEvents()V"))
  private boolean blockEvent(ServerWorld self)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.BLOCK_EVENT);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/EntityList;forEach(Ljava/util/function/Consumer;)V"))
  private boolean entity(EntityList self, Consumer c)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.ENTITY);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;tickBlockEntities()V"))
  private boolean blockEntity(ServerWorld self)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.BLOCK_ENTITY);
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerEntityManager;tick()V"))
  private boolean entityManagement(ServerEntityManager self)
  {
    return TickHandlers.getHandler(getRegistryKey()).shouldTick(TickHandlers.ENTITY_MANAGEMENT);
  }
}
