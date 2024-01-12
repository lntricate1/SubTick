package subtick.client;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//#if MC >= 12000
//$$ import net.minecraft.client.gui.GuiGraphics;
//#else
import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import subtick.QueueElement;
import subtick.TickPhase;
import subtick.util.Translations;

public class HudRenderer
{
  private static final Minecraft mc = Minecraft.getInstance();
  private static final Font font = mc.font;

  private static Color4f STEPPED_BG;
  private static int STEPPED_TEXT;
  private static Color4f STEPPING_BG;
  private static int STEPPING_TEXT;
  private static Color4f TO_STEP_BG;
  private static int TO_STEP_TEXT;

  private static Pair<Integer, Integer> trimQueue(int max, int maxHighlights)
  {
    List<QueueElement> queue = ClientTickHandler.queue;
    if(queue.size() <= max)
      return Pair.of(0, queue.size());

    int size = ClientTickHandler.queueIndex2 - ClientTickHandler.queueIndex1 + 1;
    if(size > maxHighlights)
    {
      int start = Math.min(queue.size() - max, ClientTickHandler.queueIndex2 - maxHighlights);
      return Pair.of(start, start + max);
    }

    int start = Math.min(queue.size() - max, ClientTickHandler.queueIndex1);
      return Pair.of(start, start + max);
  }

  public static enum Align implements IConfigOptionListEntry
  {
    TOP_LEFT(0, 0, "top_left"), TOP(1, 0, "top"), TOP_RIGHT(2, 0, "top_right"),
    LEFT(0, 1, "left"), CENTER(1, 1, "center"), RIGHT(2, 1, "right"),
    BOTTOM_LEFT(0, 2, "bottom_left"), BOTTOM(1, 2, "bottom"), BOTTOM_RIGHT(2, 2, "bottom_right");

    private final int x, y;
    private final String translationKey;

    private static final Map<String, Align> byString = Map.of(
      "top_left", TOP_LEFT, "top", TOP, "top_right", TOP_RIGHT,
      "left", LEFT, "center", CENTER, "right", RIGHT,
      "bottom_left", BOTTOM_LEFT, "bottom", BOTTOM, "bottom_right", BOTTOM_RIGHT);

    Align(int x, int y, String key)
    {
      this.x = x; this.y = y;
      translationKey = key;
    }

    public int getX(int w)
    {
      return x * ((mc.getWindow().getGuiScaledWidth() - w)/2);
    }

    public int getY(int h)
    {
      return y * ((mc.getWindow().getGuiScaledHeight() - h)/2);
    }

    @Override
    public String getDisplayName()
    {
      return Translations.tr("subtick.client.align." + translationKey);
    }

    @Override
    public String getStringValue()
    {
      return translationKey;
    }

    @Override
    public Align fromString(String name)
    {
      return byString.get(name);
    }

    @Override
    public Align cycle(boolean forwards)
    {
      int id = ordinal();
      if(forwards)
        return values()[++id == values().length ? 0 : id];
      else
        return values()[--id == -1 ? values().length - 1 : id];
    }
  }

  // i am too lazy to add 2 more of these comment thingys so i'm just gonna call it poseStack
  //#if MC >= 12000
  //$$ public static void render(GuiGraphics poseStack)
  //#else
  public static void render(PoseStack poseStack)
  //#endif
  {
    synchronized(ClientTickHandler.class)
    {
      if(!ClientTickHandler.frozen || !Configs.SHOW_HUD.getBooleanValue())
        return;

      STEPPED_BG = Configs.STEPPED_BG.getColor();
      STEPPED_TEXT = Configs.STEPPED_TEXT.getColor().intValue;
      STEPPING_BG = Configs.STEPPING_BG.getColor();
      STEPPING_TEXT = Configs.STEPPING_TEXT.getColor().intValue;
      TO_STEP_BG = Configs.TO_STEP_BG.getColor();
      TO_STEP_TEXT = Configs.TO_STEP_TEXT.getColor().intValue;

      TickPhase tickPhase = ClientTickHandler.tickPhase;

      Align align = (Align)Configs.HUD_ALIGNMENT.getOptionListValue();
      int xOff = Configs.HUD_OFFSET_X.getIntegerValue();
      int yOff = Configs.HUD_OFFSET_Y.getIntegerValue();

      int wPhase = 0;
      int wDim = 0;
      for(int phase = 0; phase < TickPhase.totalPhases; phase ++)
        wPhase = Math.max(wPhase, font.width(TickPhase.getPhaseName(phase)));
      for(String dim : ClientTickHandler.dimensions)
        wDim = Math.max(wDim, font.width(dim));
      wPhase += 2;
      wDim += 2;
      int wDimPhase = wDim + wPhase + 10;

      int h = font.lineHeight + 1;

      if(ClientTickHandler.queue.isEmpty())
        renderHudA(poseStack, tickPhase, align.getX(wDimPhase + 10) + xOff, align.getY(h*TickPhase.totalPhases) + yOff, wDim, wPhase, h);
      else
      {
        Component[] queue = new Component[Math.min(ClientTickHandler.queue.size(), Configs.MAX_QUEUE_SIZE.getIntegerValue())];
        Pair<Integer, Integer> indices = trimQueue(Configs.MAX_QUEUE_SIZE.getIntegerValue(), Configs.MAX_HIGHLIGHT_SIZE.getIntegerValue());
        int wQueue = 0;
        int i = indices.getLeft();
        int j = 0;
        boolean depth = tickPhase.phase() == TickPhase.BLOCK_TICK || tickPhase.phase() == TickPhase.BLOCK_EVENT;
        while(i < indices.getRight())
        {
          QueueElement element = ClientTickHandler.queue.get(i++);
          Component s = queue[j++] = text(element, i, depth);
          wQueue = Math.max(wQueue, font.width(s));
        }
        wQueue += 2;
        int wAll = wDimPhase + wQueue + 10;
        renderHudB(poseStack, queue, tickPhase, ClientTickHandler.queueIndex1 - indices.getLeft(), ClientTickHandler.queueIndex2 - indices.getLeft(), align.getX(wAll + 10) + xOff, align.getY(h*Math.max(TickPhase.totalPhases, indices.getRight() - indices.getLeft())) + yOff, wDim, wPhase, wQueue, h);
      }
    }
  }

  private static String color(ConfigColor color)
  {
    return "#" + color.getStringValue().substring(3);
  }

  private static Component text(QueueElement element, int i, boolean depth)
  {
    return depth ?
      Component.Serializer.fromJsonLenient(String.format("[\"#%d (\", {\"color\":\"%s\",\"text\":\"%d\"}, \"): %s\"]", i, color(i <= ClientTickHandler.queueIndex1 ? Configs.STEPPED_DEPTH : i <= ClientTickHandler.queueIndex2 ? Configs.STEPPING_DEPTH : Configs.TO_STEP_DEPTH), element.depth(), element.label())) :
      //#if MC >= 11900
      //$$ Component.literal(String.format("#%d: %s", i, element.label()));
      //#else
      new TextComponent(String.format("#%d: %s", i, element.label()));
      //#endif
  }

  public static void renderHudA(
    //#if MC >= 12000
    //$$ GuiGraphics guiGraphics,
    //#else
    PoseStack poseStack,
    //#endif
    TickPhase phase, int x, int y, int wDim, int wPhase, int h)
  {
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();

    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder buffer = tesselator.getBuilder();
    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

    drawTableB(buffer, x, y, h, wDim, ClientTickHandler.dimensions.size(), phase.dim());
    drawTableA(buffer, x+wDim+10, y, h, wPhase, TickPhase.totalPhases, phase.phase());


    tesselator.end();

    x += 2;
    for(int y1 = y+2, i = 0; i < ClientTickHandler.dimensions.size(); i++, y1 += h)
      //#if MC >= 12000
      //$$ guiGraphics.drawString(font, ClientTickHandler.dimensions.get(i), x, y1, i < phase.dim() ? STEPPED_TEXT : TO_STEP_TEXT, false);
      //#else
      font.draw(poseStack, ClientTickHandler.dimensions.get(i), x, y1, i < phase.dim() ? STEPPED_TEXT : TO_STEP_TEXT);
      //#endif

    x += wDim + 10;
    for(int y1 = y+2, i = 0; i < TickPhase.totalPhases; i++, y1 += h)
      //#if MC >= 12000
      //$$ guiGraphics.drawString(font, TickPhase.getPhaseName(i), x, y1, i < phase.phase() ? STEPPED_TEXT : TO_STEP_TEXT, false);
      //#else
      font.draw(poseStack, TickPhase.getPhaseName(i), x, y1, i < phase.phase() ? STEPPED_TEXT : TO_STEP_TEXT);
      //#endif
  }

  public static void renderHudB(
    //#if MC >= 12000
    //$$ GuiGraphics guiGraphics,
    //#else
    PoseStack poseStack,
    //#endif
    Component[] queue, TickPhase phase, int iqueue1, int iqueue2, int x, int y, int wDim, int wPhase, int wQueue, int h)
  {
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();

    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder buffer = tesselator.getBuilder();
    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

    drawTableB(buffer, x, y, h, wDim, ClientTickHandler.dimensions.size(), phase.dim());
    drawTableB(buffer, x+wDim+10, y, h, wPhase, TickPhase.totalPhases, phase.phase());
    drawTableC(buffer, x+wDim+wPhase+20, y, h, wQueue, queue.length, iqueue1, iqueue2);

    int sx = x + wDim + 10 + wPhase + 1;
    int sy = y + phase.phase() * h + 1;
    drawQuad(buffer,
      sx, sy, sx, sy + h - 1,
      sx + 9, y + queue.length * h, sx + 9, y, Configs.POSITION.getColor());

    tesselator.end();

    x += 2;
    for(int y1 = y+2, i = 0; i < ClientTickHandler.dimensions.size(); i++, y1 += h)
      //#if MC >= 12000
      //$$ guiGraphics.drawString(font, ClientTickHandler.dimensions.get(i), x, y1, i < phase.dim() ? STEPPED_TEXT : TO_STEP_TEXT, false);
      //#else
      font.draw(poseStack, ClientTickHandler.dimensions.get(i), x, y1, i < phase.dim() ? STEPPED_TEXT : TO_STEP_TEXT);
      //#endif

    x += wDim + 10;
    for(int y1 = y+2, i = 0; i < TickPhase.totalPhases; i++, y1 += h)
      //#if MC >= 12000
      //$$ guiGraphics.drawString(font, TickPhase.getPhaseName(i), x, y1, i < phase.phase() ? STEPPED_TEXT : TO_STEP_TEXT, false);
      //#else
      font.draw(poseStack, TickPhase.getPhaseName(i), x, y1, i < phase.phase() ? STEPPED_TEXT : TO_STEP_TEXT);
      //#endif

    x += wPhase + 10;
    for(int y1 = y+2, i = 0; i < queue.length; i++, y1 += h)
      //#if MC >= 12000
      //$$ guiGraphics.drawString(font, queue[i], x, y1, i < iqueue1 ? STEPPED_TEXT : i < iqueue2 ? STEPPING_TEXT : TO_STEP_TEXT, false);
      //#else
      font.draw(poseStack, queue[i], x, y1, i < iqueue1 ? STEPPED_TEXT : i < iqueue2 ? STEPPING_TEXT : TO_STEP_TEXT);
      //#endif
  }

  private static void drawTableA(BufferBuilder buffer, int x, int y, int h, int w, int count, int index)
  {
    // Left & right borders
    int Y = y + count * h + 1;
    int X = x + w;
    drawRect(buffer, x, y, x + 1, Y, Configs.SEPARATOR.getColor());
    drawRect(buffer, X, y, X + 1, Y, Configs.SEPARATOR.getColor());
    x += 1;
    w -= 1;
    h -= 1;

    // Fill
    for(int i = 0; i < count; i++)
    {
      if(i == index)
      {
        drawQuad(buffer, X, y, X, y+1, X+5, y+3, X+5, y-3, Configs.POSITION.getColor());
        drawRect(buffer, x, y, X, y += 1, Configs.POSITION.getColor());
      }
      else
        drawRect(buffer, x, y, X, y += 1, Configs.SEPARATOR.getColor());
      drawRect(buffer, x, y, X, y += h, i < index ? STEPPED_BG : TO_STEP_BG);
    }
    drawRect(buffer, x, y, X, y + 1, Configs.SEPARATOR.getColor());
  }

  private static void drawTableB(BufferBuilder buffer, int x, int y, int h, int w, int count, int index)
  {
    // Left & right borders
    int Y = y + count * h + 1;
    int X = x + w;
    drawRect(buffer, x, y, x + 1, Y, Configs.SEPARATOR.getColor());
    drawRect(buffer, X, y, X + 1, Y, Configs.SEPARATOR.getColor());
    x += 1;
    w -= 1;
    h -= 1;

    // Middle
    for(int i = 0; i < count; i++)
    {
      drawRect(buffer, x, y, X, y += 1, Configs.SEPARATOR.getColor()); // Border
      drawRect(buffer, x, y, X, y += h, i < index ? STEPPED_BG : i == index ? Configs.POSITION.getColor() : TO_STEP_BG); // Fill
    }
    drawRect(buffer, x, y, X, y + 1, Configs.SEPARATOR.getColor());
  }


  private static void drawTableC(BufferBuilder buffer, int x, int y, int h, int w, int count, int index1, int index2)
  {
    // Left & right borders
    int Y = y + count * h + 1;
    int X = x + w;
    drawRect(buffer, x, y, x + 1, Y, Configs.SEPARATOR.getColor());
    drawRect(buffer, X, y, X + 1, Y, Configs.SEPARATOR.getColor());
    x += 1;
    w -= 1;
    h -= 1;

    // Middle
    for(int i = 0; i < count; i++)
    {
      if(i == index2)
      {
        drawQuad(buffer, X, y, X, y+1, X+5, y+3, X+5, y-3, Configs.POSITION.getColor());
        drawRect(buffer, x, y, X, y += 1, Configs.POSITION.getColor());
        drawRect(buffer, x, y, X, y += h, TO_STEP_BG);
      }
      else
      {
        drawRect(buffer, x, y, X, y += 1, Configs.SEPARATOR.getColor());
        drawRect(buffer, x, y, X, y += h, i < index1 ? STEPPED_BG : i < index2 ? STEPPING_BG : TO_STEP_BG);
      }
    }
    drawRect(buffer, x, y, X, y + 1, Configs.SEPARATOR.getColor());
  }

  private static void drawRect(BufferBuilder buffer, int x, int y, int X, int Y, Color4f color)
  {
    buffer.vertex(x, y, 0D).color(color.r, color.g, color.b, color.a).endVertex();
    buffer.vertex(x, Y, 0D).color(color.r, color.g, color.b, color.a).endVertex();
    buffer.vertex(X, Y, 0D).color(color.r, color.g, color.b, color.a).endVertex();
    buffer.vertex(X, y, 0D).color(color.r, color.g, color.b, color.a).endVertex();
  }

  private static void drawQuad(BufferBuilder buffer, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, Color4f color)
  {
    buffer.vertex(x1, y1, 0D).color(color.r, color.g, color.b, color.a).endVertex();
    buffer.vertex(x2, y2, 0D).color(color.r, color.g, color.b, color.a).endVertex();
    buffer.vertex(x3, y3, 0D).color(color.r, color.g, color.b, color.a).endVertex();
    buffer.vertex(x4, y4, 0D).color(color.r, color.g, color.b, color.a).endVertex();
  }
}
