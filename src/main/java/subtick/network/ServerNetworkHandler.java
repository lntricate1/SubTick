package subtick.network;

import java.util.ArrayList;

import carpet.CarpetSettings;
//#if MC >= 12006
//$$ import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
//#else
import carpet.helpers.TickSpeed;
//#endif
import carpet.network.CarpetClient;
import carpet.network.ClientNetworkHandler;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import subtick.QueueElement;
import subtick.TickPhase;
import subtick.mixins.carpet.ServerNetworkHandlerAccessor;
import subtick.util.Translations;

//#if MC >= 12006
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//$$ import net.minecraft.network.codec.StreamCodec;
//#endif

public class ServerNetworkHandler
{
  //#if MC >= 12006
  //$$ public static final ResourceLocation SUBTICK_CHANNEL = ResourceLocation.fromNamespaceAndPath("subtick", "hello");
  //$$ private record SubtickPayload(CompoundTag data) implements CustomPacketPayload
  //$$ {
  //$$   public static final StreamCodec<FriendlyByteBuf, SubtickPayload> STREAM_CODEC = CustomPacketPayload.codec(SubtickPayload::write, SubtickPayload::new);
  //$$   public static final Type<SubtickPayload> TYPE = new CustomPacketPayload.type<>(SUBTICK_CHANNEL);
  //$$ 
  //$$   public SubtickPayload(FriendlyByteBuf input)
  //$$   {
  //$$     this(input.readNbt());
  //$$   }
  //$$ 
  //$$   public void write(FriendlyByteBuf output)
  //$$   {
  //$$     output.writeNbt(data);
  //$$   }
  //$$ 
  //$$   @Override public Type<SubtickPayload> type()
  //$$   {
  //$$     return TYPE;
  //$$   }
  //$$ }
  //#endif

  private static boolean tryClient(ServerLevel level, CompoundTag tag)
  {
    if(level.server.isDedicatedServer())
      return false;

    FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
    packetBuf.writeVarInt(CarpetClient.DATA);
    packetBuf.writeNbt(tag);
    Minecraft minecraft = Minecraft.getInstance();
    ClientNetworkHandler.handleData(packetBuf, minecraft.player);
    return true;
  }

  public static void sendNbt(ServerPlayer player, CompoundTag tag, CommandSourceStack actor)
  {
    ServerLevel level = player.getLevel();
    if(tryClient(level, tag))
      return;

    FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
    packetBuf.writeVarInt(CarpetClient.DATA);
    packetBuf.writeNbt(tag);

    try
    {
      player.connection.send(new ClientboundCustomPayloadPacket(CarpetClient.CARPET_CHANNEL, packetBuf));
    }
    catch(IllegalArgumentException e)
    {
      Translations.m(actor, "queueCommand.err.packetSize");
    }
  }

  public static void sendNbt(ServerPlayer player, CompoundTag tag)
  {
    ServerLevel level = player.getLevel();
    if(tryClient(level, tag))
      return;

    FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
    packetBuf.writeVarInt(CarpetClient.DATA);
    packetBuf.writeNbt(tag);

    try
    {
      player.connection.send(new ClientboundCustomPayloadPacket(CarpetClient.CARPET_CHANNEL, packetBuf));
    }
    catch(IllegalArgumentException e)
    {}
  }

  public static void sendNbt(ServerLevel level, CompoundTag tag, CommandSourceStack actor)
  {
    if(tryClient(level, tag))
      return;

    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.getLevel() != level) continue;

      sendNbt(player, tag, actor);
    }
  }

  public static void sendNbt(ServerLevel level, CompoundTag tag)
  {
    if(tryClient(level, tag))
      return;

    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.getLevel() != level) continue;

      sendNbt(player, tag);
    }
  }

  public static void sendFrozen(ServerLevel level, TickPhase tickPhase)
  {
    if(CarpetSettings.superSecretSetting)
      return;

    CompoundTag tag = new CompoundTag();
    CompoundTag tickingState = new CompoundTag();
    tickingState.putBoolean("is_paused", true);
    tickingState.putBoolean("deepFreeze", true);
    tickingState.putInt("phase", tickPhase.phase());
    tickingState.putInt("dim", tickPhase.dim());
    ListTag listTag = new ListTag();
    for(String dim : TickPhase.getDimensions())
    {
      // Why is NBT like this? i know you can make a list of StringTags, but there seems to be no way of doing it in mojang code
      CompoundTag element = new CompoundTag();
      element.putString("d", dim);
      listTag.add(element);
    }
    tickingState.put("dims", listTag);
    tag.put("TickingState", tickingState);
    sendNbt(level, tag);
  }

  public static void sendFrozen(ServerPlayer player, boolean frozen, TickPhase tickPhase)
  {
    if(CarpetSettings.superSecretSetting || !ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().containsKey(player))
      return;

    CompoundTag tag = new CompoundTag();
    CompoundTag tickingState = new CompoundTag();
    if(frozen)
    {
      tickingState.putBoolean("is_paused", true);
      tickingState.putBoolean("deepFreeze", true);
      tickingState.putInt("phase", tickPhase.phase());
      tickingState.putInt("dim", tickPhase.dim());
    }
    else
    {
      tickingState.putBoolean("is_paused", false);
      tickingState.putBoolean("deepFreeze", false);
      tickingState.putInt("phase", -1);
      tickingState.putInt("dim", -1);
    }
    ListTag listTag = new ListTag();
    for(String dim : TickPhase.getDimensions())
    {
      CompoundTag element = new CompoundTag();
      element.putString("d", dim);
      listTag.add(element);
    }
    tickingState.put("dims", listTag);
    tag.put("TickingState", tickingState);
    sendNbt(player, tag);
  }

  public static void sendUnfrozen(ServerLevel level)
  {
    if(CarpetSettings.superSecretSetting)
      return;

    CompoundTag tag = new CompoundTag();
    CompoundTag tickingState = new CompoundTag();
    tickingState.putBoolean("is_paused", false);
    tickingState.putBoolean("deepFreeze", false);
    tickingState.putInt("phase", -1);
    tickingState.putInt("dim", -1);
    tag.put("TickingState", tickingState);
    sendNbt(level, tag);
  }

  public static void sendTickStep(ServerLevel level, int ticks, TickPhase tickPhase)
  {
    if(CarpetSettings.superSecretSetting)
      return;

    if(ticks != 0)
    {
      //#if MC >= 12006
      //$$ level.getServer().getPlayerList().broadcastAll(new ClientboundTickingStepPacket(ticks));
      //#else
      CompoundTag tag = new CompoundTag();
      tag.putInt("TickPlayerActiveTimeout", ticks + TickSpeed.PLAYER_GRACE);
      sendNbt(level, tag);
      //#endif
    }

    CompoundTag tag = new CompoundTag();
    CompoundTag phaseTag = new CompoundTag();
    phaseTag.putInt("dim", tickPhase.dim());
    phaseTag.putInt("phase", tickPhase.phase());
    tag.put("TickPhase", phaseTag);
    sendNbt(level, tag);
  }

  public static void sendQueueStep(ObjectLinkedOpenHashSet<QueueElement> queue, ArrayList<QueueElement> spentQueue, int newQueueElementsCount, int steps, ServerLevel level, CommandSourceStack actor)
  {
    if(CarpetSettings.superSecretSetting || (queue.isEmpty() && spentQueue.isEmpty()))
      return;

    CompoundTag tag = new CompoundTag();
    CompoundTag queueTag = new CompoundTag();
    ListTag list = new ListTag();
    for(QueueElement element : spentQueue)
    {
      CompoundTag elementTag = new CompoundTag();
      elementTag.putString("s", element.label());
      elementTag.putInt("x", element.x());
      elementTag.putInt("y", element.y());
      elementTag.putInt("z", element.z());
      elementTag.putInt("d", element.depth());
      list.add(elementTag);
    }
    for(QueueElement element : queue)
    {
      CompoundTag elementTag = new CompoundTag();
      elementTag.putString("s", element.label());
      elementTag.putInt("x", element.x());
      elementTag.putInt("y", element.y());
      elementTag.putInt("z", element.z());
      elementTag.putInt("d", element.depth());
      list.add(elementTag);
    }
    queueTag.put("queue", list);
    queueTag.putInt("steps", steps);
    queueTag.putInt("newElements", newQueueElementsCount);
    tag.put("QueueStep", queueTag);
    sendNbt(level, tag, actor);
  }

  public static void sendQueue(ObjectLinkedOpenHashSet<QueueElement> queue, ArrayList<QueueElement> spentQueue, ServerLevel level)
  {
    if(CarpetSettings.superSecretSetting || queue.isEmpty())
      return;

    CompoundTag tag = new CompoundTag();
    ListTag list = new ListTag();
    for(QueueElement element : spentQueue)
    {
      CompoundTag elementTag = new CompoundTag();
      elementTag.putString("s", element.label());
      elementTag.putInt("x", element.x());
      elementTag.putInt("y", element.y());
      elementTag.putInt("z", element.z());
      elementTag.putInt("d", element.depth());
      list.add(elementTag);
    }
    for(QueueElement element : queue)
    {
      CompoundTag elementTag = new CompoundTag();
      elementTag.putString("s", element.label());
      elementTag.putInt("x", element.x());
      elementTag.putInt("y", element.y());
      elementTag.putInt("z", element.z());
      elementTag.putInt("d", element.depth());
      list.add(elementTag);
    }
    tag.put("Queue", list);
    sendNbt(level, tag);
  }
}
