package subtick.mixins;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;

import subtick.TickPhase;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin
{
  @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
  private void onOverworldAdded(ChunkProgressListener chunkProgressListener, CallbackInfo ci,
    ServerLevelData serverLevelData, WorldGenSettings worldGenSettings, boolean bl, long l, long m, List<?> list,
    Registry<?> registry, ChunkGenerator chunkGenerator, LevelStem levelStem, Holder<?> holder, ServerLevel serverLevel)
  {
    TickPhase.addDimension(serverLevel);
  }

  @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
  private void onDimensionAdded(ChunkProgressListener chunkProgressListener, CallbackInfo ci,
    ServerLevelData serverLevelData, WorldGenSettings worldGenSettings, boolean bl, long l, long m, List<?> list,
    Registry<?> registry, ChunkGenerator chunkGenerator, LevelStem levelStem, Holder<?> holder, ServerLevel serverLevel,
    WorldBorder worldBorder, Iterator<?> var17, Map.Entry<?,?> entry, ResourceKey<?> resourceKey, ResourceKey<?> resourceKey2,
    Holder<?> holder2, ChunkGenerator chunkGenerator2, DerivedLevelData derivedLevelData, ServerLevel serverLevel2)
  {
    TickPhase.addDimension(serverLevel2);
  }
}
