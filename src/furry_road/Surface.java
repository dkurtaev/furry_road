package furry_road;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Random;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class Surface {

  public Surface(float min_x, float max_x, float min_z, float max_z) {
    x_limits = new float[]{min_x, max_x};
    z_limits = new float[]{min_z, max_z};
  }

  public void Init(GL2 gl) throws Exception {
    InitShaderProgram(gl);
    GenerateOpacityMap(gl);
  }

  public void Draw(GL2 gl, int layer_idx, int n_layers, int color_texture_id) {
    gl.glUseProgram(shader_program);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glEnable(GL2.GL_BLEND);
    gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
    gl.glEnable(GL2.GL_ALPHA_TEST);

    gl.glUniform1i(loc_opacity_map, 0);
    gl.glUniform1i(loc_color_texture, 1);
    gl.glUniform1f(loc_layer_depth, (float)layer_idx / (n_layers - 1));

    gl.glActiveTexture(GL.GL_TEXTURE1);
    gl.glBindTexture(GL.GL_TEXTURE_2D, color_texture_id);

    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glBindTexture(GL.GL_TEXTURE_2D, opacity_map_id);
    gl.glBegin(GL2.GL_QUADS);
      gl.glTexCoord2f(1f, 0f); gl.glVertex3f(x_limits[0], 0f, z_limits[0]);
      gl.glTexCoord2f(0f, 0f); gl.glVertex3f(x_limits[0], 0f, z_limits[1]);
      gl.glTexCoord2f(0f, 1f); gl.glVertex3f(x_limits[1], 0f, z_limits[1]);
      gl.glTexCoord2f(1f, 1f); gl.glVertex3f(x_limits[1], 0f, z_limits[0]);
    gl.glEnd();

    gl.glDisable(GL.GL_TEXTURE_2D);
    gl.glDisable(GL2.GL_BLEND);
    gl.glDisable(GL2.GL_ALPHA_TEST);
    gl.glUseProgram(0);
  }

  private void InitShaderProgram(GL2 gl) throws Exception {
    shader_program =
        ShaderFactory.CreateShaderProgram(gl, "shaders/surface_vertex.glsl",
                                          "shaders/surface_fragment.glsl");
    loc_opacity_map = gl.glGetUniformLocation(shader_program, "u_opacity_map");
    loc_layer_depth = gl.glGetUniformLocation(shader_program, "u_layer_depth");
    loc_color_texture = gl.glGetUniformLocation(shader_program,
                                                "u_color_texture");
  }

  private void GenerateOpacityMap(GL2 gl) throws Exception {
    final int width = 256;
    final int height = 256;
    final float min_length = 0.9f;
    final float max_length = 1.0f;
    final float fur_dencity = 0.5f;

    final int n_seeds = (int)(width * height * fur_dencity);

    BufferedImage texture = new BufferedImage(width, height,
                                              BufferedImage.TYPE_BYTE_GRAY);
    BufferedImage fur = ImageIO.read(new File("/home/dkurtaev/Downloads/fur.png"));

    Random rand = new Random();
    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        texture.setRGB(x, y, 0x00000000);
      }
    }
    for (int i = 0; i < n_seeds; ++i) {
      float length = rand.nextFloat() * (max_length - min_length) + min_length;
      Color color = new Color(length, length, length);
      texture.setRGB(rand.nextInt(width), rand.nextInt(height), color.getRGB());
    }

    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        int rel_x = (int)(((float)x / (width - 1)) * (fur.getWidth() - 1));
        int rel_y = (int)(((float)y / (height - 1)) * (fur.getHeight() - 1));
        if (fur.getRGB(rel_x, rel_y) == 0xFF000000) {
          texture.setRGB(x, y, 0xFF000000);
        }
      }
    }

    opacity_map_id = GenTexture(gl, texture);
  }

  public static int GenTexture(GL2 gl, BufferedImage texture)
      throws Exception {
    final int width = texture.getWidth();
    final int height = texture.getHeight();

    int ids[]= new int[1];
    gl.glGenTextures(1, ids, 0);

    gl.glBindTexture(GL.GL_TEXTURE_2D, ids[0]);
    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
                       GL.GL_NEAREST);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
                       GL.GL_NEAREST);

    DataBufferByte buffer = (DataBufferByte)texture.getRaster()
                                                   .getDataBuffer();
    byte[] buffer_data = buffer.getData();
    ByteBuffer pixels = ByteBuffer.allocateDirect(buffer_data.length);
    pixels.put(buffer_data);
    pixels.position(0);

    final int n_channels = buffer_data.length / (width * height);
    switch (n_channels) {
      case 1: {
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 1, width, height, 0, GL2.GL_RED,
                        GL.GL_UNSIGNED_BYTE, pixels);
        break;
      }
      case 3: {
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 3, width, height, 0, GL2.GL_BGR,
                        GL.GL_UNSIGNED_BYTE, pixels);
        break;
      }
      default: {
        throw new Exception ("Texture has " + n_channels +
                             " channels (supports 3 or 1).");
      }
    };

    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
    return ids[0];
  }

  private float x_limits[];
  private float z_limits[];

  private int opacity_map_id;

  private int shader_program;
  private int loc_opacity_map;
  private int loc_layer_depth;
  private int loc_color_texture;

}
