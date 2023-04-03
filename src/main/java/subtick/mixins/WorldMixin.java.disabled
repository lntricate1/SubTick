package subtick.mixins;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import subtick.ClientTickHandler;

import net.minecraft.util.registry.RegistryKey;
import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(World.class)
public abstract class WorldMixin
{
  @Shadow @Final private boolean isClient;
  @Shadow public abstract RegistryKey<World> getRegistryKey();

  @Inject(method = "tickEntity(Ljava/util/function/Consumer;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
  private void tickEntity(Consumer<Entity> consumer, Entity e, CallbackInfo ci)
  {
    if(isClient && ClientTickHandler.frozen && !(e instanceof PlayerEntity)) ci.cancel();
  }
}
