package subtick.mixins.carpet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import carpet.commands.TickCommand;

@Mixin(TickCommand.class)
public class TickCommandMixin
{
  @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/arguments/IntegerArgumentType;integer(II)Lcom/mojang/brigadier/arguments/IntegerArgumentType;"), index = 0, remap = false)
  private static int minimumTicks(int original)
  {
    return 0;
  }
}
