package subtick.mixins.client;

import java.util.Map;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.helpers.ServerTickRateManager;
import carpet.network.ClientNetworkHandler;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import subtick.RenderHandler;
import subtick.client.ClientTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import subtick.interfaces.IEntity;

@Mixin(ClientNetworkHandler.class)
public class ClientNetworkHandlerMixin
{
  @Shadow private static Map<String, BiConsumer<AbstractClientPlayer, Tag>> dataHandlers;

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void addDataHandlers(CallbackInfo ci)
  {
    dataHandlers.put("BlockHighlighting", (p, t) ->
    {
      RenderHandler.clear();
      for(int i = 0, count = ((ListTag)t).size(); i < count; i ++)
      {
        CompoundTag nbt = ((ListTag)t).getCompound(i);

        RenderHandler.addCuboid(
          nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"),
          nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
      }
    });

    dataHandlers.put("EntityHighlighting", (p, t) ->
    {
      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      level.tickingEntities.forEach((entity) -> ((IEntity)entity).setCGlowing(false));

      int[] ids = ((IntArrayTag)t).getAsIntArray();

      for(int i = 0; i < ids.length; i ++)
        ((IEntity)level.getEntity(ids[i])).setCGlowing(true);
    });

    dataHandlers.put("BlockEntityTicks", (p, t) ->
    {
      for(int i = 0, count = ((ListTag)t).size(); i < count; i ++)
      {
        CompoundTag nbt = ((ListTag)t).getCompound(i);
        ClientTickHandler.addPos(new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")));
      }
    });

    dataHandlers.put("TickingState", (p, t) ->
    {
      ClientTickHandler.setFreeze(((CompoundTag)t).getBoolean("is_paused"));
    });

    dataHandlers.put("TickPlayerActiveTimeout", (p, t) ->
    {
      if(((NumericTag)t).getAsInt() > ServerTickRateManager.PLAYER_GRACE)
        ClientTickHandler.scheduleTickStep(((NumericTag)t).getAsInt());
    });
  }
}
