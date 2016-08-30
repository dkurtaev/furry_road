package furry_road;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class Surface {

  public Surface(float min_x, float max_x, float min_z, float max_z) {
    x_limits = new float[]{min_x, max_x};
    z_limits = new float[]{min_z, max_z};
  }

  public void Init(GL2 gl) throws Exception {
    InitShaderProgram(gl);
    GenHeightsMap(gl);
  }

  public void Draw(GL2 gl, float layer_height, int texture_id,
                   int fur_places_texture_id) {
    gl.glUseProgram(shader_program);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glEnable(GL2.GL_BLEND);
    gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
    gl.glEnable(GL2.GL_ALPHA_TEST);

    // Uniforms.
    gl.glUniform1i(loc_heights_map, 0);
    gl.glUniform1i(loc_texture, 1);
    gl.glUniform1i(loc_fur_places, 2);
    gl.glUniform1f(loc_layer_height, layer_height);

    // Textures.
    gl.glActiveTexture(GL.GL_TEXTURE2);
    gl.glBindTexture(GL.GL_TEXTURE_2D, fur_places_texture_id);

    gl.glActiveTexture(GL.GL_TEXTURE1);
    gl.glBindTexture(GL.GL_TEXTURE_2D, texture_id);

    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glBindTexture(GL.GL_TEXTURE_2D, heights_map_id);

    // Drawing.
    gl.glBegin(GL2.GL_QUADS);
      gl.glTexCoord2f(0f, 1f); gl.glVertex2f(x_limits[0], z_limits[0]);
      gl.glTexCoord2f(1f, 1f); gl.glVertex2f(x_limits[1], z_limits[0]);
      gl.glTexCoord2f(1f, 0f); gl.glVertex2f(x_limits[1], z_limits[1]);
      gl.glTexCoord2f(0f, 0f); gl.glVertex2f(x_limits[0], z_limits[1]);
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
    loc_heights_map = gl.glGetUniformLocation(shader_program, "u_heights_map");
    loc_texture = gl.glGetUniformLocation(shader_program, "u_texture");
    loc_fur_places = gl.glGetUniformLocation(shader_program, "u_fur_places");
    loc_layer_height = gl.glGetUniformLocation(shader_program,
                                               "u_layer_height");
  }

  private void GenHeightsMap(GL2 gl) throws Exception {
    final int width = 256;
    final int height = 256;
    final float min_length = 0.9f;
    final float max_length = 1.0f;
    final float fur_dencity = 0.5f;

    final int n_fibers = (int)(width * height * fur_dencity);

    BufferedImage texture = new BufferedImage(width, height,
                                              BufferedImage.TYPE_BYTE_GRAY);
    Random rand = new Random();
    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        texture.setRGB(x, y, 0x00000000);
      }
    }
    for (int i = 0; i < n_fibers; ++i) {
      float length = rand.nextFloat() * (max_length - min_length) + min_length;
      Color color = new Color(length, length, length);
      texture.setRGB(rand.nextInt(width), rand.nextInt(height), color.getRGB());
    }

    heights_map_id = TextureFactory.GenTexture(gl, texture);
  }

  private float x_limits[];
  private float z_limits[];

  private int heights_map_id;

  private int shader_program;
  private int loc_heights_map;
  private int loc_texture;
  private int loc_fur_places;
  private int loc_layer_height;

}
