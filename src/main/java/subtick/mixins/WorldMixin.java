package subtick.mixins;

import subtick.TickHandler;
import subtick.TickHandlers;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(World.class)
public class WorldMixin
{
  @ModifyReturnValue(method = "getTime", at = @At("RETURN"))
  private long getTime(long original)
  {
    TickHandler handler = TickHandlers.getHandler(((World)(Object)this).getRegistryKey());
    if(handler != null)
      return handler.time;
    else
      return original;
  }
}
