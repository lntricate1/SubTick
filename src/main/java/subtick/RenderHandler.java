package subtick;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

public class RenderHandler
{
  private static Minecraft mc = Minecraft.getInstance();
  private static ArrayList<Shape> shapes = new ArrayList<Shape>();

  public static void render()
  {
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.disableDepthTest();
    Camera camera = mc.gameRenderer.getMainCamera();

    for(Shape shape : List.copyOf(shapes))
      shape.render(camera);
  }

  public static void clear()
  {
    shapes = new ArrayList<Shape>();
  }

  public static void addCuboid(double x, double y, double z, double X, double Y, double Z, int color)
  {
    shapes.add(new Cuboid(x, y, z, X, Y, Z, color));
  }

  private static interface Shape
  {
    public void render(Camera camera);
  }

  private static class Line implements Shape
  {
    private final double x, y, z, X, Y, Z;
    private final int color;

    public Line(double x, double y, double z, double X, double Y, double Z, int color)
    {
      this.x = x;
      this.y = y;
      this.z = z;
      this.X = X;
      this.Y = Y;
      this.Z = Z;
      this.color = color;
    }

    @Override
    public void render(Camera camera)
    {
      double cx = camera.getPosition().x();
      double cy = camera.getPosition().y();
      double cz = camera.getPosition().z();
      BufferBuilder buffer = Tesselator.getInstance().getBuilder();
      buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(x-cx, y-cy, z-cz).color(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F).endVertex();
      buffer.vertex(X-cx, Y-cy, Z-cz).color(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F).endVertex();
      Tesselator.getInstance().end();
    }
  }

  private static class Cuboid implements Shape
  {
    private final Line[] edges;

    public Cuboid(double x, double y, double z, double X, double Y, double Z, int color)
    {
      edges = new Line[]
      {
        new Line(x, y, z, X, y, z, color),
        new Line(X, y, z, X, Y, z, color),
        new Line(X, Y, z, x, Y, z, color),
        new Line(x, Y, z, x, y, z, color),

        new Line(x, y, z, x, y, Z, color),
        new Line(X, y, z, X, y, Z, color),
        new Line(x, Y, z, x, Y, Z, color),
        new Line(X, Y, z, X, Y, Z, color),

        new Line(x, y, Z, X, y, Z, color),
        new Line(X, y, Z, X, Y, Z, color),
        new Line(X, Y, Z, x, Y, Z, color),
        new Line(x, Y, Z, x, y, Z, color)
      };
    }

    @Override
    public void render(Camera camera)
    {
      for(Line line : edges)
        line.render(camera);
    }
  }
}
