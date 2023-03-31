package subtick.mixins.carpet;

import carpet.network.CarpetClient;
import carpet.network.ServerNetworkHandler;
import carpet.CarpetSettings;
import subtick.TickHandlers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;

@Mixin(ServerNetworkHandler.class)
public interface ServerNetworkHandlerAccessor
{
  @Accessor(value = "remoteCarpetPlayers", remap = false)
  public static Map<ServerPlayerEntity, String> getRemoteCarpetPlayers()
  {
    throw new AssertionError();
  }
  // @Shadow @Final private static Map<ServerPlayerEntity, String> remoteCarpetPlayers;
  //
  // @Inject(method = "updateFrozenStateToConnectedPlayers", at = @At("HEAD"), cancellable = true, remap = false)
  // private static void updateFrozenStateToConnectedPlayers(CallbackInfo ci)
  // {
  //   if(CarpetSettings.superSecretSetting) ci.cancel();
  //
  //   for(ServerPlayerEntity player : remoteCarpetPlayers.keySet())
  //   {
  //     boolean frozen = TickHandlers.getHandler(player.world.getRegistryKey()).frozen;
  //     System.out.println(TickHandlers.getHandler(player.world.getRegistryKey()).dimensionName + ": " + frozen);
  //
  //     NbtCompound tag = new NbtCompound();
  //     NbtCompound tickingState = new NbtCompound();
  //     tickingState.putBoolean("is_paused", frozen);
  //     tickingState.putBoolean("deepFreeze", frozen);
  //     tag.put("TickingState", tickingState);
  //
  //     PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
  //     packetBuf.writeVarInt(CarpetClient.DATA);
  //     packetBuf.writeNbt(tag);
  //
  //     player.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetClient.CARPET_CHANNEL, packetBuf));
  //   }
  //   ci.cancel();
  // }
}
