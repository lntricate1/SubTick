package subtick.client;

import java.util.ArrayList;

import carpet.helpers.ServerTickRateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

public class ClientTickHandler
{
  private static ArrayList<BlockPos> poses = new ArrayList<>();
  private static boolean scheduled_block_entity_tick;
  private static long scheduled_time;
  public static boolean frozen;

  private static boolean stepping;
  private static int remaining_ticks;
  public static boolean skip_block_entities;

  public static void addPos(BlockPos pos)
  {
    poses.add(pos);
    scheduled_block_entity_tick = true;
    scheduled_time = System.currentTimeMillis() + 50;
  }

  public static void setFreeze(boolean newFrozen)
  {
    frozen = newFrozen;
    if(!frozen)
    {
      Minecraft minecraftClient = Minecraft.getInstance();
      ClientBlockEntityQueue.end(minecraftClient.level);
    }
  }

  public static void scheduleTickStep(int ticks)
  {
    Minecraft minecraft = Minecraft.getInstance();
    if(ClientBlockEntityQueue.end(minecraft.level))
      skip_block_entities = true;

    stepping = true;
    remaining_ticks = ticks;
  }

  public static boolean shouldTick()
  {
    if(!frozen)
      return true;

    return stepping;
  }

  public static void onTick(ClientLevel level)
  {
    if(stepping && -- remaining_ticks <= ServerTickRateManager.PLAYER_GRACE)
    {
      stepping = false;
    }

    if(scheduled_block_entity_tick && System.currentTimeMillis() >= scheduled_time)
    {
      ClientBlockEntityQueue.step(level, poses);
      poses.clear();
      scheduled_block_entity_tick = false;
    }
    skip_block_entities = false;
  }
}
