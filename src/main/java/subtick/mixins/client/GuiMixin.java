package subtick.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Gui;
//#if MC >= 12000
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import subtick.client.HudRenderer;

@Mixin(Gui.class)
public class GuiMixin
{
  //#if MC >= 12000
  //$$ @Inject(method = "render", at = @At("RETURN"))
  //$$ private void renderHud(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci)
  //$$ {
  //$$   HudRenderer.render(guiGraphics);
  //$$ }
  //#else
  @Inject(method = "render", at = @At(value = "INVOKE", shift = Shift.AFTER, target = "Lnet/minecraft/client/gui/Gui;renderEffects(Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
  private void renderHud(PoseStack poseStack, float partialTick, CallbackInfo ci)
  {
    HudRenderer.render(poseStack);
  }
  //#endif
}
