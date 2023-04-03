package subtick.mixins;

import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.WrapWithCondition;

import subtick.ClientTickHandler;

import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;

@Mixin(targets = "net/minecraft/world/chunk/WorldChunk$DirectBlockEntityTickInvoker")
public class DirectBlockEntityTickInvokerMixin
{
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;)V"))
  private boolean tick(BlockEntityTicker self, World world, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity)
  {
    return !(world.isClient && ClientTickHandler.frozen);
  }
}
