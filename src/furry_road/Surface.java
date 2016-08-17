package furry_road;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferByte;
import java.awt.image.Kernel;
import java.nio.ByteBuffer;
import java.util.Random;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class Surface {

  public Surface(float min_x, float max_x, float min_z, float max_z,
                 float light_vector[], float surfaces_shift) {
    x_limits = new float[]{min_x, max_x};
    z_limits = new float[]{min_z, max_z};
  }

  public void InitShaderProgram(GL2 gl) throws Exception {
    shader_program =
        ShaderFactory.CreateShaderProgram(gl, "shaders/surface_shader.vertex",
                                          "shaders/surface_shader.fragment");
    loc_opacity_map = gl.glGetUniformLocation(shader_program, "u_opacity_map");
    loc_shadows_map = gl.glGetUniformLocation(shader_program, "u_shadows_map");
    loc_opacity_threshold = gl.glGetUniformLocation(shader_program,
                                                    "u_opacity_threshold");
    loc_layer_idx = gl.glGetUniformLocation(shader_program,
                                            "u_relative_layer_idx");
  }

  public void Draw(GL2 gl, int layer_idx, float surfaces_shift, int n_layers) {
    gl.glUseProgram(shader_program);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glEnable(GL2.GL_BLEND);
    gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
    gl.glEnable(GL2.GL_ALPHA_TEST);

    gl.glUniform1i(loc_opacity_map, 0);
    gl.glUniform1i(loc_shadows_map, 1);
    gl.glUniform1f(loc_opacity_threshold, layer_idx * threshold_step +
                                          mean_opacity);
    gl.glUniform1f(loc_layer_idx, (float)layer_idx / (n_layers - 1));

    gl.glActiveTexture(GL.GL_TEXTURE1);
    gl.glBindTexture(GL.GL_TEXTURE_2D, shadows_map_id);

    gl.glActiveTexture(GL.GL_TEXTURE0);
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

  public void GenerateOpacityMap(GL2 gl, int n_surfaces, float light_vector[],
                                 float surfaces_shift) {
    final int width = 256;
    final int height = 256;
    final int dim = width * height;

    // Uniformly distributed noise.
    BufferedImage noise = new BufferedImage(width, height,
                                            BufferedImage.TYPE_BYTE_GRAY);
    Random rand = new Random();
    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        float value = rand.nextFloat();
        noise.setRGB(x, y, new Color(value, value, value).getRGB());
      }
    }

    // Convolutional averaging.
    BufferedImage texture = new BufferedImage(width, height,
                                              BufferedImage.TYPE_BYTE_GRAY);
    final int conv_kernel_size = 3;
    float conv_kernel_data[] = new float[conv_kernel_size * conv_kernel_size];
    float value = 1.0f / conv_kernel_data.length;
    for (int i = 0; i < conv_kernel_data.length; ++i) {
      conv_kernel_data[i] = value;
    }

    Kernel conv_kernel = new Kernel(conv_kernel_size, conv_kernel_size,
                                    conv_kernel_data);
    ConvolveOp conv_op = new ConvolveOp(conv_kernel);
    conv_op.filter(noise, texture);

    // Generate texture.
    int ids[]= new int[1];
    gl.glGenTextures(1, ids, 0);
    opacity_map_id = ids[0];

    gl.glBindTexture(GL.GL_TEXTURE_2D, opacity_map_id);
    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
                       GL.GL_NEAREST);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
                       GL.GL_NEAREST);

    DataBufferByte buffer = (DataBufferByte)texture.getRaster()
                                                   .getDataBuffer();
    byte[] buffer_data = buffer.getData();
    ByteBuffer pixels = ByteBuffer.allocateDirect(dim);
    pixels.put(buffer_data);
    pixels.position(0);

    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 1, width, height, 0, GL2.GL_RED,
                    GL.GL_UNSIGNED_BYTE, pixels);
    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

    // Compute mean and max values for thresholds step.
    float max_opacity = 0;
    mean_opacity = 0;
    float opacity_map[][] = new float[height][width];
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        // Byte in [-128, 127]. This means that 127=127, 128=-128, 129=-127.
        float pixel = buffer_data[y * width + x];
        if (pixel < 0) {
          pixel += 256;
        }
        pixel /= 255f;
        if (pixel > max_opacity) {
          max_opacity = pixel;
        }
        mean_opacity += pixel;
        opacity_map[y][x] = pixel;
      }
    }
    mean_opacity /= dim;

    threshold_step = (max_opacity - mean_opacity) / (n_surfaces - 1);

    BufferedImage shadow_map = ShadowsMap.Build(opacity_map, light_vector,
                                                surfaces_shift, n_surfaces,
                                                threshold_step,
                                                x_limits[0], x_limits[1],
                                                z_limits[0], z_limits[1]);
    shadows_map_id = GenGreyscaleTexture(gl, shadow_map);
  }

  private static int GenGreyscaleTexture(GL2 gl, BufferedImage img) {
    final int width = img.getWidth();
    final int height = img.getHeight();

    int ids[]= new int[1];
    gl.glGenTextures(1, ids, 0);

    gl.glBindTexture(GL.GL_TEXTURE_2D, ids[0]);
    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
                       GL.GL_NEAREST);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
                       GL.GL_NEAREST);

    DataBufferByte buffer = (DataBufferByte)img.getRaster()
                                               .getDataBuffer();
    byte[] buffer_data = buffer.getData();
    ByteBuffer pixels = ByteBuffer.allocateDirect(width * height);
    pixels.put(buffer_data);
    pixels.position(0);

    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 1, width, height, 0, GL2.GL_RED,
                    GL.GL_UNSIGNED_BYTE, pixels);
    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

    return ids[0];
  }

  private float x_limits[];
  private float z_limits[];
  float threshold_step;
  float mean_opacity;

  private int opacity_map_id;
  private int shadows_map_id;

  private int shader_program;
  private int loc_opacity_map;
  private int loc_shadows_map;
  private int loc_opacity_threshold;
  private int loc_layer_idx;

}
