package furry_road;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.util.Random;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class Surface {

  public Surface(float min_x, float max_x, float min_z, float max_z) {
    x_limits = new float[]{min_x, max_x};
    z_limits = new float[]{min_z, max_z};
  }

  public void InitShaderProgram(GL2 gl) throws Exception {
    shader_program =
        ShaderFactory.CreateShaderProgram(gl, "shaders/surface_shader.vertex",
                                          "shaders/surface_shader.fragment");
    loc_opacity_map = gl.glGetUniformLocation(shader_program, "u_opacity_map");
  }

  public void Draw(GL2 gl, float light_vector[], int layer_idx) {
    gl.glUseProgram(shader_program);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glEnable(GL2.GL_BLEND);
    gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
    gl.glEnable(GL2.GL_ALPHA_TEST);

    gl.glUniform1i(loc_opacity_map, 0);

    gl.glBindTexture(GL.GL_TEXTURE_2D, opacity_map_id);
    gl.glBegin(GL2.GL_QUADS);
      gl.glTexCoord2f(0f, 0f); gl.glVertex3f(x_limits[0], 0f, z_limits[0]);
      gl.glTexCoord2f(1f, 0f); gl.glVertex3f(x_limits[0], 0f, z_limits[1]);
      gl.glTexCoord2f(1f, 1f); gl.glVertex3f(x_limits[1], 0f, z_limits[1]);
      gl.glTexCoord2f(0f, 1f); gl.glVertex3f(x_limits[1], 0f, z_limits[0]);
    gl.glEnd();

    gl.glDisable(GL.GL_TEXTURE_2D);
    gl.glDisable(GL2.GL_BLEND);
    gl.glDisable(GL2.GL_ALPHA_TEST);
    gl.glUseProgram(0);
  }

  public void GenerateOpacityMap(GL2 gl, int n_surfaces) {
    final int width = 256;
    final int height = 256;
    final float fur_dencity = 0.5f;

    final int dim = width * height;
    final int n_seeds = (int)(dim * fur_dencity);

    BufferedImage texture = new BufferedImage(width, height,
                                              BufferedImage.TYPE_BYTE_GRAY);
    Random rand = new Random();
    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        texture.setRGB(x, y, 0x00000000);
      }
    }
    for (int i = 0; i < n_seeds; ++i) {
      texture.setRGB(rand.nextInt(width), rand.nextInt(height), 0xFFFFFFFF);
    }

    opacity_map_id = GenGreyscaleTexture(gl, texture);
  }

  private static int GenGreyscaleTexture(GL2 gl, BufferedImage texture) {
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

    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 1, width, height, 0, GL2.GL_RED,
                    GL.GL_UNSIGNED_BYTE, pixels);
    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
    return ids[0];
  }

  private float x_limits[];
  private float z_limits[];

  private int opacity_map_id;

  private int shader_program;
  private int loc_opacity_map;

}
