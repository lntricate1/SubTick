package subtick.mixins.carpet;

import java.util.Map;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.network.ClientNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import subtick.RenderHandler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import subtick.interfaces.IEntity;

@Mixin(ClientNetworkHandler.class)
public class ClientNetworkHandlerMixin
{
  @Shadow private static Map<String, BiConsumer<ClientPlayerEntity, NbtElement>> dataHandlers;

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void addDataHandlers(CallbackInfo ci)
  {
    dataHandlers.put("BlockHighlighting", (p, t) ->
    {
      RenderHandler.clear();
      for(int i = 0, count = ((NbtList)t).size(); i < count; i ++)
      {
        NbtCompound nbt = ((NbtList)t).getCompound(i);

        RenderHandler.addCuboid(
          nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"),
          nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"),
          nbt.getInt("color"));
      }
    });

    dataHandlers.put("EntityHighlighting", (p, t) ->
    {
      MinecraftClient client = MinecraftClient.getInstance();
      ClientWorld world = client.world;
      world.entityList.forEach((entity) -> {((IEntity)entity).setCGlowing(false);});

      int[] ids = ((NbtIntArray)t).getIntArray();

      for(int i = 0; i < ids.length; i ++)
        ((IEntity)world.getEntityById(ids[i])).setCGlowing(true);
      client.close();
    });
  }
}
