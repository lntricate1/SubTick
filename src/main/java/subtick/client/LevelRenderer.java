package subtick.client;

import java.util.HashSet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
//#if MC >= 11900
//$$ import org.joml.Quaternionf;
//#else
import com.mojang.math.Quaternion;
//#endif
import com.mojang.math.Transformation;

import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public class LevelRenderer
{
  private static final Minecraft mc = Minecraft.getInstance();
  private static final Font font = mc.font;
  private static final HashSet<Line> lines = new HashSet<>();
  private static final HashSet<Quad> quads = new HashSet<>();
  private static final HashSet<Text> texts = new HashSet<>();

  public static synchronized void render(PoseStack poseStack)
  {
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableDepthTest();
    Camera camera = mc.gameRenderer.getMainCamera();
    Vec3 cpos = camera.getPosition();

    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder buffer = tesselator.getBuilder();
    buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
    for(Line line : lines)
      line.render(buffer, cpos.x, cpos.y, cpos.z);
    tesselator.end();

    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    for(Quad quad : quads)
      quad.render(buffer, cpos.x, cpos.y, cpos.z);
    tesselator.end();

    PoseStack ps = RenderSystem.getModelViewStack();
    //#if MC >= 11900
    //$$ Quaternionf rot = camera.rotation();
    //#else
    Quaternion rot = camera.rotation();
    //#endif
    for(Text text : texts)
      text.render(buffer, ps, rot, cpos.x, cpos.y, cpos.z);
  }

  public static synchronized void clear()
  {
    lines.clear();
    quads.clear();
    texts.clear();
  }

  public static void addCuboid(int x, int y, int z, Color4f color)
  {
    addCuboidFaces(x, y, z, x+1, y+1, z+1, color);
    addCuboidEdges(x, y, z, x+1, y+1, z+1, color);
  }

  public static void addCuboidFaces(int x, int y, int z, Color4f color)
  {
    addCuboidFaces(x, y, z, x+1, y+1, z+1, color);
  }

  public static void addCuboidEdges(int x, int y, int z, Color4f color)
  {
    addCuboidEdges(x, y, z, x+1, y+1, z+1, color);
  }

  public static synchronized void addCuboidFaces(double x, double y, double z, double X, double Y, double Z, Color4f color)
  {
    quads.add(new QuadCuboid(x, y, z, X, Y, Z, color));
  }

  public static synchronized void addCuboidEdges(double x, double y, double z, double X, double Y, double Z, Color4f color)
  {
    lines.add(new LineCuboid(x, y, z, X, Y, Z, color));
  }

  public static synchronized void addText(String text, int x, int y, int z, Color4f color)
  {
    texts.add(new TextBasic(text, x + 0.5, y + 0.5, z + 0.5, color));
  }

  public static synchronized void addLabel(int index, int depth, int x, int y, int z, Color4f color1, Color4f color2)
  {
    texts.add(new DepthLabel(String.valueOf(index), String.valueOf(depth), x + 0.5, y + 0.5, z + 0.5, color1.intValue, color2.intValue));
  }

  private static interface Line
  {
    public void render(BufferBuilder buffer, double cx, double cy, double cz);
  }

  // private static record LineBasic(double x, double y, double z, double X, double Y, double Z, Color4f color) implements Line
  // {
  //   public void render(BufferBuilder buffer, double cx, double cy, double cz)
  //   {
  //     buffer.vertex(x-cx, y-cy, z-cz).color(color.r, color.g, color.b, color.a).endVertex();
  //     buffer.vertex(X-cx, Y-cy, Z-cz).color(color.r, color.g, color.b, color.a).endVertex();
  //   }
  // }

  private static record LineCuboid(double x, double y, double z, double X, double Y, double Z, Color4f color) implements Line
  {
    public void render(BufferBuilder buffer, double cx, double cy, double cz)
    {
      double x = this.x - cx, y = this.y - cy, z = this.z - cz;
      double X = this.X - cx, Y = this.Y - cy, Z = this.Z - cz;
      // Bottom
      buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      // Middle
      buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      // Top
      buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
    }
  }

  private static interface Quad
  {
    public void render(BufferBuilder buffer, double cx, double cy, double cz);
  }

  // private static record QuadBasic(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color4f color) implements Quad
  // {
  //   public void render(BufferBuilder buffer, double cx, double cy, double cz)
  //   {
  //     buffer.vertex(x1-cx, y1-cy, z1-cz).color(color.r, color.g, color.b, color.a).endVertex();
  //     buffer.vertex(x2-cx, y2-cy, z2-cz).color(color.r, color.g, color.b, color.a).endVertex();
  //     buffer.vertex(x3-cx, y3-cy, z3-cz).color(color.r, color.g, color.b, color.a).endVertex();
  //     buffer.vertex(x4-cx, y4-cy, z4-cz).color(color.r, color.g, color.b, color.a).endVertex();
  //   }
  // }

  private static record QuadCuboid(double x, double y, double z, double X, double Y, double Z, Color4f color) implements Quad
  {
    public void render(BufferBuilder buffer, double cx, double cy, double cz)
    {
      double x = this.x - cx, y = this.y - cy, z = this.z - cz;
      double X = this.X - cx, Y = this.Y - cy, Z = this.Z - cz;
      buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();

      buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
      buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
    }
  }

  private static interface Text
  {
    //#if MC >= 11900
    //$$ public void render(BufferBuilder builder, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz);
    //#else
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz);
    //#endif
  }

  private static record TextBasic(String text, double x, double y, double z, Color4f color) implements Text
  {
    @Override
    //#if MC >= 11900
    //$$ public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
    //#else
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz)
    //#endif
    {
      poseStack.pushPose();
      poseStack.translate(x - cx, y - cy, z - cz);
      poseStack.mulPose(rotation);
      poseStack.scale(-0.07F, -0.07F, 0.07F);
      RenderSystem.applyModelViewMatrix();
      MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 11904
      //$$ font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      immediate.endBatch();
      poseStack.popPose();
    }
  }

  private static record DepthLabel(String index, String depth, double x, double y, double z, int color1, int color2) implements Text
  {
    @Override
    //#if MC >= 11900
    //$$ public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
    //#else
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz)
    //#endif
    {
      poseStack.pushPose();
      poseStack.translate(x - cx, y - cy, z - cz);
      poseStack.mulPose(rotation);
      poseStack.scale(-0.07F, -0.07F, 0.08F);
      RenderSystem.applyModelViewMatrix();

      MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 11904
      //$$ font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      immediate.endBatch();

      poseStack.translate(font.width(index)/2F, 0, 0);
      poseStack.scale(0.5F, 0.5F, 0.5F);
      RenderSystem.applyModelViewMatrix();
      immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 11904
      //$$ font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      immediate.endBatch();

      poseStack.popPose();
    }
  }
}
