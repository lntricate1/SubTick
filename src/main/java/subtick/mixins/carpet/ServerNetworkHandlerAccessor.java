package subtick.mixins.carpet;

import carpet.network.ServerNetworkHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerNetworkHandler.class)
public interface ServerNetworkHandlerAccessor
{
  @Accessor(value = "remoteCarpetPlayers", remap = false)
  public static Map<ServerPlayer, String> getRemoteCarpetPlayers()
  {
    throw new AssertionError();
  }
}
