package subtick.mixins.carpet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.network.ServerNetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import subtick.SubTick;

@Mixin(ServerNetworkHandler.class)
public class ServerNetworkHandlerMixin
{
  @Inject(method = "onHello", at = @At("TAIL"))
  private static void onHello(ServerPlayer player, FriendlyByteBuf data, CallbackInfo ci)
  {
    subtick.network.ServerNetworkHandler.updateFrozenStateToConnectedPlayer(player, SubTick.getTickHandler(player.level()).frozen);
  }
}
