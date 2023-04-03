package subtick.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import subtick.RenderHandler;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin
{
  @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V", ordinal = 1))
  private void onRenderWorldLastNormal(MatrixStack matrices, float delta, long time, boolean renderBlockOutline, Camera camera, GameRenderer renderer, LightmapTextureManager lightmapTextureManager, Matrix4f projMatrix, CallbackInfo ci)
  {
    RenderHandler.render();
  }
}
