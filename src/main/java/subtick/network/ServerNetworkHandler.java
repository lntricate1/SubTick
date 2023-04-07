package subtick.network;

import java.util.List;

import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.network.CarpetClient;
import carpet.utils.Messenger;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import subtick.TickHandlers;
import subtick.mixins.carpet.ServerNetworkHandlerAccessor;

public class ServerNetworkHandler
{
  public static void sendNbt(ServerPlayer player, CompoundTag tag, CommandSourceStack actor)
  {
      FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
      packetBuf.writeVarInt(CarpetClient.DATA);
      packetBuf.writeNbt(tag);

      try
      {
        player.connection.send(new ClientboundCustomPayloadPacket(CarpetClient.CARPET_CHANNEL, packetBuf));
      }
      catch(IllegalArgumentException e)
      {
        Messenger.m(actor, TickHandlers.err("Highlights not sent because packet size exceeds maximum. Step less at a time to see highlights."));
      }
  }

  public static void sendNbt(ServerPlayer player, CompoundTag tag)
  {
      FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
      packetBuf.writeVarInt(CarpetClient.DATA);
      packetBuf.writeNbt(tag);

      try
      {
        player.connection.send(new ClientboundCustomPayloadPacket(CarpetClient.CARPET_CHANNEL, packetBuf));
      }
      catch(IllegalArgumentException e)
      {
      }
  }

  public static void sendNbt(ServerLevel level, CompoundTag tag, CommandSourceStack actor)
  {
    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.level != level) continue;

      sendNbt(player, tag, actor);
    }
  }

  public static void sendNbt(ServerLevel level, CompoundTag tag)
  {
    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.level != level) continue;

      sendNbt(player, tag);
    }
  }

  public static void updateFrozenStateToConnectedPlayers(ServerLevel level, boolean frozen)
  {
    if(CarpetSettings.superSecretSetting) return;

    CompoundTag tag = new CompoundTag();
    CompoundTag tickingState = new CompoundTag();
    tickingState.putBoolean("is_paused", frozen);
    tickingState.putBoolean("deepFreeze", frozen);
    tag.put("TickingState", tickingState);
    sendNbt(level, tag);
  }

  public static void updateFrozenStateToConnectedPlayer(ServerPlayer player, boolean frozen)
  {
    if(CarpetSettings.superSecretSetting) return;
    if(!ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().containsKey(player)) return;

    CompoundTag tag = new CompoundTag();
    CompoundTag tickingState = new CompoundTag();
    tickingState.putBoolean("is_paused", frozen);
    tickingState.putBoolean("deepFreeze", frozen);
    tag.put("TickingState", tickingState);
    sendNbt(player, tag);
  }

  public static void updateTickPlayerActiveTimeoutToConnectedPlayers(ServerLevel level, int ticks)
  {
    if(CarpetSettings.superSecretSetting) return;

    CompoundTag tag = new CompoundTag();
    tag.putInt("TickPlayerActiveTimeout", ticks + TickSpeed.PLAYER_GRACE);
    sendNbt(level, tag);
  }

  public static void sendBlockHighlights(List<AABB> aabbs, ServerLevel level, CommandSourceStack actor)
  {
    if(CarpetSettings.superSecretSetting || aabbs.isEmpty()) return;

    CompoundTag tag = new CompoundTag();
    ListTag list = new ListTag();
    for(AABB aabb : aabbs)
    {
      CompoundTag nbt = new CompoundTag();
      nbt.putDouble("x", aabb.minX);
      nbt.putDouble("y", aabb.minY);
      nbt.putDouble("z", aabb.minZ);
      nbt.putDouble("X", aabb.maxX);
      nbt.putDouble("Y", aabb.maxY);
      nbt.putDouble("Z", aabb.maxZ);
      list.add(nbt);
    }
    tag.put("BlockHighlighting", list);
    sendNbt(level, tag, actor);
  }

  public static void sendEntityHighlights(List<Integer> ids, ServerLevel level, CommandSourceStack actor)
  {
    if(CarpetSettings.superSecretSetting) return;

    CompoundTag tag = new CompoundTag();
    tag.put("EntityHighlighting", new IntArrayTag(ids));
    sendNbt(level, tag, actor);
  }

  public static void clearBlockHighlights(ServerLevel level)
  {
    CompoundTag tag = new CompoundTag();
    tag.put("BlockHighlighting", new ListTag());
    sendNbt(level, tag);
  }

  public static void clearEntityHighlights(ServerLevel level)
  {
    if(CarpetSettings.superSecretSetting) return;

    CompoundTag tag = new CompoundTag();
    tag.put("EntityHighlighting", new IntArrayTag(new int[]{}));
    sendNbt(level, tag);
  }
}
