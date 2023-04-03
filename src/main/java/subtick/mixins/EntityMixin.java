package subtick.mixins;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import subtick.interfaces.IEntity;

@Mixin(Entity.class)
public class EntityMixin implements IEntity
{
  private boolean cGlowing = false;
  private int color = 0;

  public void setCGlowing(boolean value, int color)
  {
    cGlowing = value;
    this.color = color;
  }

  @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
  private void isGlowing(CallbackInfoReturnable<Boolean> cir)
  {
    if(cGlowing)
      cir.setReturnValue(true);
  }

  @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
  private void changeTeamColor(CallbackInfoReturnable<Integer> cir)
  {
    if(cGlowing)
      cir.setReturnValue(subtick.Settings.subtickHighlightColor);
  }
}
