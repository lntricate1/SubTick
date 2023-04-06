package subtick.mixins;

import subtick.TickHandler;
import subtick.TickHandlers;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(Level.class)
public class LevelMixin
{
  @ModifyReturnValue(method = "getGameTime", at = @At("RETURN"))
  private long getTime(long original)
  {
    TickHandler handler = TickHandlers.getHandler(((Level)(Object)this).dimension());
    if(handler != null)
      return handler.time;
    else
      return original;
  }
}
