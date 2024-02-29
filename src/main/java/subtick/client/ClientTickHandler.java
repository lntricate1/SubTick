package subtick.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import carpet.helpers.TickSpeed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import subtick.QueueElement;
import subtick.TickPhase;

public class ClientTickHandler
{
  private static final Minecraft mc = Minecraft.getInstance();
  public static final List<QueueElement> queue = new ArrayList<>();
  public static final List<String> dimensions = new ArrayList<>();
  public static boolean frozen;
  public static TickPhase tickPhase = TickPhase.INVALID;
  public static int queueIndex1 = 0, queueIndex2 = 0;

  private static boolean stepping;
  private static int remaining_ticks;
  public static boolean skip_block_entities;

  private static void clearRenders()
  {
    LevelRenderer.clear();
    mc.level.tickingEntities.forEach((entity) -> ((IEntity)entity).setCGlowing(false));
  }

  public static void setFreeze(CompoundTag tag)
  {
    if(frozen = tag.getBoolean("is_paused"))
    {
      try
      {
        setPhase(new TickPhase(tag));
        ListTag listTag = (ListTag)tag.get("dims");
        dimensions.clear();
        for(Tag element : listTag)
          dimensions.add(((CompoundTag)element).getString("d"));
      }
      catch(Exception e)
      {
        setPhase(TickPhase.INVALID);
      }
    }
    else
    {
      ClientBlockEntityQueue.end(mc.level);
      clearQueue();
      clearRenders();
    }
  }

  public static void setPhase(TickPhase phase)
  {
    tickPhase = phase;
    clearQueue();
    clearRenders();
    queueIndex1 = 0;
    queueIndex2 = 0;
  }

  public static synchronized void clearQueue()
  {
    queue.clear();
  }

  public static synchronized void setQueue(ListTag tag)
  {
    queue.clear();
    tag.forEach((Tag t) ->
    {
      CompoundTag t1 = (CompoundTag)t;
      queue.add(new QueueElement(t1.getString("s"), t1.getInt("x"), t1.getInt("y"), t1.getInt("z"), t1.getInt("d")));
    });
  }

  public static synchronized void queueStep(CompoundTag tag)
  {
    setQueue((ListTag)tag.get("queue"));
    int steps = tag.getInt("steps");
    queueIndex1 = queueIndex2;
    queueIndex2 += steps;
    // out of bounds protection
    int index2 = Math.min(queueIndex2, queue.size());
    int index1 = Math.min(queueIndex1, index2);

    clearRenders();
    if(tickPhase.phase() == TickPhase.ENTITY)
    {
      ClientLevel level = mc.level;
      level.tickingEntities.forEach((entity) -> ((IEntity)entity).setCGlowing(false));
      for(int i = queueIndex1; i < queueIndex2; i ++)
      {
        QueueElement element = queue.get(i);
        ((IEntity)level.getEntity(element.x())).setCGlowing(true);
      }
      return;
    }

    boolean blockEntity = tickPhase.phase() == TickPhase.BLOCK_ENTITY;
    boolean depth = tickPhase.phase() == TickPhase.BLOCK_EVENT || tickPhase.phase() == TickPhase.BLOCK_TICK;
    int i = 0;
    Iterator<QueueElement> iter = queue.iterator();
    while(i < index1)
    {
      QueueElement element = iter.next();
      LevelRenderer.addCuboidFaces(element.x(), element.y(), element.z(), Configs.STEPPED_BG.getColor());
      if(depth)
        LevelRenderer.addLabel(++i, element.depth(), element.x(), element.y(), element.z(), Configs.STEPPED_TEXT.getColor(), Configs.STEPPED_DEPTH.getColor());
      else
        LevelRenderer.addText(String.valueOf(++i), element.x(), element.y(), element.z(), Configs.STEPPED_TEXT.getColor());
    }
    while(i < index2)
    {
      QueueElement element = iter.next();
      LevelRenderer.addCuboidFaces(element.x(), element.y(), element.z(), Configs.STEPPING_BG.getColor());
      if(depth)
      LevelRenderer.addLabel(++i, element.depth(), element.x(), element.y(), element.z(), Configs.STEPPING_TEXT.getColor(), Configs.STEPPING_DEPTH.getColor());
      else
        LevelRenderer.addText(String.valueOf(++i), element.x(), element.y(), element.z(), Configs.STEPPING_TEXT.getColor());

      if(blockEntity)
        ClientBlockEntityQueue.addPos(element);
    }
    while(i < queue.size())
    {
      QueueElement element = iter.next();
      LevelRenderer.addCuboidFaces(element.x(), element.y(), element.z(), Configs.TO_STEP_BG.getColor());
      if(depth)
      LevelRenderer.addLabel(++i, element.depth(), element.x(), element.y(), element.z(), Configs.TO_STEP_TEXT.getColor(), Configs.TO_STEP_DEPTH.getColor());
      else
        LevelRenderer.addText(String.valueOf(++i), element.x(), element.y(), element.z(), Configs.TO_STEP_TEXT.getColor());
    }
  }

  public static void scheduleTickStep(int ticks)
  {
    if(ticks <= TickSpeed.PLAYER_GRACE)
      return;

    clearQueue();
    clearRenders();

    if(ClientBlockEntityQueue.end(mc.level))
      skip_block_entities = true;

    stepping = true;
    remaining_ticks = ticks;
  }

  public static boolean shouldTick()
  {
    return !frozen || stepping;
  }

  public static void onTick(ClientLevel level)
  {
    if(stepping && -- remaining_ticks <= TickSpeed.PLAYER_GRACE)
      stepping = false;

    ClientBlockEntityQueue.step(level);
    skip_block_entities = false;
  }
}
