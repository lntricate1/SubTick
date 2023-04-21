package subtick.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import subtick.client.ClientTickHandler;

@Mixin(ClientLevel.class)
public class ClientLevelMixin
{
  @Inject(method = "tick", at = @At("TAIL"))
  private void onTick(CallbackInfo ci)
  {
    ClientTickHandler.onTick((ClientLevel)(Object)this);
  }

  @WrapWithCondition(method = "tickEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickBlockEntities()V"))
  private boolean tickBlockEntities(ClientLevel level)
  {
    return !ClientTickHandler.skip_block_entities && ClientTickHandler.shouldTick();
  }

  @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
  private void tickNonPassenger(Entity entity, CallbackInfo ci)
  {
    if(!ClientTickHandler.shouldTick() && !(entity instanceof Player))
      ci.cancel();
  }
}
