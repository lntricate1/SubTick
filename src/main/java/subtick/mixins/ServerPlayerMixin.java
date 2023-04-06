package subtick.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import subtick.TickHandlers;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin
{
  // Needed to account for end -> overworld portal
  @Inject(method = "changeDimension", at = @At("RETURN"))
  private void changeDimension(ServerLevel destination, CallbackInfoReturnable<Entity> cir)
  {
    TickHandlers.getHandler(destination.dimension()).updateFrozenStateToConnectedPlayer((ServerPlayer)(Object)this);
  }

  // Needed to account for teleports
  @Inject(method = "triggerDimensionChangeTriggers", at = @At("RETURN"))
  private void changeDimension2(ServerLevel origin, CallbackInfo ci)
  {
    TickHandlers.getHandler(((ServerPlayer)(Object)this).level.dimension()).updateFrozenStateToConnectedPlayer((ServerPlayer)(Object)this);
  }
}
