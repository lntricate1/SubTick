package subtick.mixins;

import subtick.TickHandler;
import subtick.interfaces.ILevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(Level.class)
public class LevelMixin implements ILevel
{
  @Override
  public TickHandler getTickHandler() {
    return null;
  }

  @ModifyReturnValue(method = "getGameTime", at = @At("RETURN"))
  private long getTime(long original)
  {
    TickHandler handler = getTickHandler();
    if(handler != null)
      return handler.time;
    else
      return original;
  }
}
