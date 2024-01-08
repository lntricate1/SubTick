package subtick.client;

import java.util.Map;
import java.util.function.BiConsumer;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import subtick.TickPhase;

public class ClientNetworkHandler
{
  public static void addDataHandlers(Map<String, BiConsumer<AbstractClientPlayer, Tag>> dataHandlers)
  {
    dataHandlers.put("TickingState", (p, t) ->
    {
      ClientTickHandler.setFreeze((CompoundTag)t);
    });

    dataHandlers.put("TickPhase", (p, t) ->
    {
      ClientTickHandler.setPhase(new TickPhase((CompoundTag)t));
    });

    dataHandlers.put("TickPlayerActiveTimeout", (p, t) ->
    {
      ClientTickHandler.scheduleTickStep(((NumericTag)t).getAsInt());
    });

    dataHandlers.put("Queue", (p, t) ->
    {
      ClientTickHandler.setQueue((ListTag)t);
    });

    dataHandlers.put("QueueStep", (p, t) ->
    {
      ClientTickHandler.queueStep((CompoundTag)t);
    });
  }
}
