package subtick;

import static subtick.TickHandlers.t;

import net.minecraft.server.world.ServerWorld;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import carpet.utils.Messenger;

import carpet.helpers.TickSpeed;
import subtick.mixins.carpet.ServerNetworkHandlerAccessor;
import carpet.CarpetSettings;
import carpet.network.CarpetClient;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;

public class TickHandler
{
  public final ServerWorld world;
  public final String dimensionName;
  public final Queues queues = new Queues(this);
  public long time;

  // Freeze
  public boolean frozen = false;
  public boolean freezing = false;
  private boolean unfreezing = false;
  private int target_phase = 0;
  // Step
  private boolean stepping = false;
  private boolean in_first_stepped_phase = false;
  private int remaining_ticks = 0;
  public int current_phase = 0;

  public TickHandler(String dimensionName, ServerWorld world)
  {
    this.dimensionName = dimensionName.substring(0, 1).toUpperCase() + dimensionName.substring(1);
    this.world = world;
    this.time = world.getTime();
  }

  public void tickTime()
  {
    time += 1L;
  }

  public boolean shouldTick(int phase)
  {
    // Freezing
    if(freezing && phase == target_phase)
    {
      freezing = false;
      frozen = true;
      current_phase = phase;
      updateFrozenStateToConnectedPlayers();
      return false;
    }

    // Normal ticking
    if(!frozen)
    {
      current_phase = phase;
      return true;
    }
    // Everything below this is frozen logic

    // Unfreezing
    if(unfreezing && phase == current_phase)
    {
      unfreezing = false;
      frozen = false;
      updateFrozenStateToConnectedPlayers();
      return true;
    }

    if(!stepping || phase != current_phase) return false;
    // Continues only if stepping and in current_phase

    // Stepping
    if(in_first_stepped_phase)
      updateTickPlayerActiveTimeoutToConnectedPlayers();
    else if(phase == 0)
      --remaining_ticks;

    in_first_stepped_phase = false;
    if(remaining_ticks < 1 && phase == target_phase)
    {
      stepping = false;
      queues.executeScheduledSteps();
      return false;
    }
    advancePhase();
    return true;
  }

  public void advancePhase()
  {
    current_phase = (current_phase + 1) % TickHandlers.TOTAL_PHASES;
  }

  public void step(int ticks, int phase)
  {
    stepping = true;
    in_first_stepped_phase = true;
    remaining_ticks = ticks;
    target_phase = phase;
    if(ticks != 0 || phase != current_phase)
      queues.finishQueueStep();
  }

  public void freeze(int phase)
  {
    freezing = true;
    target_phase = phase;
  }

  public void unfreeze()
  {
    if(freezing) freezing = false;
    else
    {
      unfreezing = true;
      stepping = false;
      queues.finishQueueStep();
    }
  }

  public boolean canStep(CommandContext<ServerCommandSource> c, int count, int phase)
  {
    if(!frozen)
    {
      Messenger.m(c.getSource(), getDimension(), t(" cannot step because it's not frozen"));
      return false;
    }

    if(stepping)
    {
      Messenger.m(c.getSource(), getDimension(), t(" cannot step because it's already tick stepping"));
      return false;
    }

    if(count == 0 && phase < current_phase)
    {
      Messenger.m(c.getSource(), getDimension(), t(" cannot step to an earlier phase in the same tick"));
      return false;
    }

    if(queues.scheduled != -1)
    {
      Messenger.m(c.getSource(), getDimension(), t(" cannot step because it's already queueStepping"));
      return false;
    }

    return true;
  }

  public static void sendNbt(ServerPlayerEntity player, NbtCompound tag)
  {
      PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
      packetBuf.writeVarInt(CarpetClient.DATA);
      packetBuf.writeNbt(tag);

      player.networkHandler.sendPacket(new CustomPayloadS2CPacket(CarpetClient.CARPET_CHANNEL, packetBuf));
  }

  private void updateFrozenStateToConnectedPlayers()
  {
    if(CarpetSettings.superSecretSetting) return;

    for(ServerPlayerEntity player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.world != world) continue;

      NbtCompound tag = new NbtCompound();
      NbtCompound tickingState = new NbtCompound();
      tickingState.putBoolean("is_paused", frozen);
      tickingState.putBoolean("deepFreeze", frozen);
      tag.put("TickingState", tickingState);

      sendNbt(player, tag);
    }
  }

  public void updateFrozenStateToConnectedPlayer(ServerPlayerEntity player)
  {
    if(CarpetSettings.superSecretSetting) return;

    if(ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().containsKey(player))
    {
      NbtCompound tag = new NbtCompound();
      NbtCompound tickingState = new NbtCompound();
      tickingState.putBoolean("is_paused", frozen);
      tickingState.putBoolean("deepFreeze", frozen);
      tag.put("TickingState", tickingState);

      sendNbt(player, tag);
    }
  }

  private void updateTickPlayerActiveTimeoutToConnectedPlayers()
  {
    if(CarpetSettings.superSecretSetting) return;

    for(ServerPlayerEntity player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.world != world) continue;

      NbtCompound tag = new NbtCompound();
      tag.putInt("TickPlayerActiveTimeout", remaining_ticks + TickSpeed.PLAYER_GRACE);

      sendNbt(player, tag);
    }
  }

  public String getPhase()
  {
    return Settings.subtickPhaseFormat + " " + TickHandlers.tickPhaseNames[current_phase];
  }

  public String getDimension()
  {
    return Settings.subtickDimensionFormat + " " + dimensionName;
  }
}
