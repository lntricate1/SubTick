package subtick.mixins.client;

import java.util.Map;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.network.ClientNetworkHandler;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.Tag;

@Mixin(ClientNetworkHandler.class)
public class ClientNetworkHandlerMixin
{
  @Shadow(remap = false) private static Map<String, BiConsumer<AbstractClientPlayer, Tag>> dataHandlers;

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void addDataHandlers(CallbackInfo ci)
  {
    subtick.client.ClientNetworkHandler.addDataHandlers(dataHandlers);
  }
}
