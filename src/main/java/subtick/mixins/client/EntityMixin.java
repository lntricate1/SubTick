package subtick.mixins.client;

import net.minecraft.world.entity.Entity;
import subtick.client.Configs;
import subtick.client.IEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements IEntity
{
  private boolean cGlowing = false;

  @Override
  public void setCGlowing(boolean value)
  {
    cGlowing = value;
  }

  @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
  private void isGlowing(CallbackInfoReturnable<Boolean> cir)
  {
    if(cGlowing)
      cir.setReturnValue(true);
  }

  @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
  private void changeTeamColor(CallbackInfoReturnable<Integer> cir)
  {
    if(cGlowing)
      cir.setReturnValue(Configs.STEPPING_BG.getColor().intValue);
  }
}
