package subtick.mixins;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;

import subtick.TickPhase;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin
{
  @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
  private void onOverworldAdded(ChunkProgressListener chunkProgressListener, CallbackInfo ci, ServerLevelData serverLevelData, boolean bl,
    Registry<?> registry, WorldOptions worldOptions, long l, long m, List<?> list, LevelStem levelStem, ServerLevel serverLevel)
  {
    TickPhase.addDimension(serverLevel);
  }

  @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
  private void onDimensionAdded(ChunkProgressListener chunkProgressListener, CallbackInfo ci, ServerLevelData serverLevelData, boolean bl,
    Registry<?> registry, WorldOptions worldOptions, long l, long m, List<?> list, LevelStem levelStem, ServerLevel serverLevel,
    WorldBorder worldBorder, RandomSequences randomSequences, Iterator<?> var16, Map.Entry<?,?> entry, ResourceKey<?> resourceKey, ResourceKey<?> resourceKey2, DerivedLevelData derivedLevelData, ServerLevel serverLevel2)
  {
    TickPhase.addDimension(serverLevel2);
  }
}
