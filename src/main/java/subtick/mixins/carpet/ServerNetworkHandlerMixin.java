package subtick.mixins.carpet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.network.ServerNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import subtick.TickHandlers;

@Mixin(ServerNetworkHandler.class)
public class ServerNetworkHandlerMixin
{
  @Inject(method = "onHello", at = @At("TAIL"))
  private static void onHello(ServerPlayerEntity player, PacketByteBuf data, CallbackInfo ci)
  {
    TickHandlers.getHandler(player.world.getRegistryKey()).updateFrozenStateToConnectedPlayer(player);
  }
}
