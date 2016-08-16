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

    // Compute shadow.                 ______
    //        ^                        \     |n   angl = dot(n_normal, -n_light)
    //    \   | plane's normal         l\angl|o
    //     \  |                         i\   |r
    //      \ |                          g\  |m
    // light v|                           h\^|a
    // -------*-------- plane              t\|l
    //
    //
    // --^----p3-*-------------- layer 2   d - constant distance between layers.
    // d |        \ <- light vector
    // --x------p2-*------------ layer 1
    // d |          \
    // --v--------p1-*---------- layer 0
    //               ^
    //               | current fragment
    //
    // Point p2 is p1 - L * light, where L = d / cos(angl)
    // Compute shift of corresponding texture coordinates.
    float light_vec_norm = (float)Math.sqrt(light_vector[0] * light_vector[0] +
                                            light_vector[1] * light_vector[1] +
                                            light_vector[2] * light_vector[2]);
    float cos_angle = -light_vector[1] / light_vec_norm;
    float step_by_light = surfaces_shift / cos_angle;
    float position_shift[] = {-step_by_light * light_vector[0],
                              -step_by_light * light_vector[2]};
    texture_shift = new float[]{position_shift[1] / (max_z - min_z),
                                position_shift[0] / (max_x - min_x)};
  }

  public void InitShaderProgram(GL2 gl) throws Exception {
    shader_program =
        ShaderFactory.CreateShaderProgram(gl, "shaders/surface_shader.vertex",
                                          "shaders/surface_shader.fragment");
    loc_opacity_map = gl.glGetUniformLocation(shader_program, "u_opacity_map");
    loc_threshold = gl.glGetUniformLocation(shader_program,
                                            "u_opacity_threshold");
    loc_num_layers_above = gl.glGetUniformLocation(shader_program,
                                                   "u_num_layers_above");
    loc_opacity_threshold_step =
        gl.glGetUniformLocation(shader_program, "u_opacity_threshold_step");
    loc_n_layers = gl.glGetUniformLocation(shader_program, "u_n_layers");
    loc_tex_coords_shift = gl.glGetUniformLocation(shader_program,
                                                   "u_tex_coords_shift");
  }

  public void Draw(GL2 gl, int layer_idx, float surfaces_shift, int n_layers) {
    gl.glUseProgram(shader_program);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glEnable(GL2.GL_BLEND);
    gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
    gl.glEnable(GL2.GL_ALPHA_TEST);

    gl.glUniform1i(loc_opacity_map, 0);
    gl.glUniform1f(loc_threshold, opacity_thresholds[layer_idx]);
    gl.glUniform2fv(loc_tex_coords_shift, 1, texture_shift, 0);

    gl.glUniform1i(loc_num_layers_above, n_layers - layer_idx - 1);
    gl.glUniform1f(loc_opacity_threshold_step, opacity_thresholds[1] -
                                               opacity_thresholds[0]);
    gl.glUniform1i(loc_n_layers, n_layers);

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
    final int conv_kernel_size = 5;
    float conv_kernel_data[] = new float[conv_kernel_size * conv_kernel_size];
    float value = 1.0f / conv_kernel_data.length;
    for (int i = 0; i < conv_kernel_data.length; ++i) {
      conv_kernel_data[i] = value;
    }
    Kernel conv_kernel = new Kernel(3, 3, conv_kernel_data);
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
    int max = 0;
    for (int i = 0; i < dim; ++i) {
      // Byte in [-128, 127]. This means that 127 = 127, 128 = -128, 129 = -127
      int pixel = buffer_data[i];
      if (pixel < 0) {
        pixel += 256;
      }
      if (pixel > max) {
        max = pixel;
      }
    }

    opacity_thresholds = new float[n_surfaces];
    float threshold_step = (float)max / n_surfaces;
    for (int i = 0; i < n_surfaces; ++i) {
      opacity_thresholds[i] = (threshold_step * i) / 255;
    }
  }

  private int shader_program;
  private int loc_opacity_map;
  private int loc_threshold;
  private int loc_num_layers_above;
  private int loc_opacity_threshold_step;
  private int loc_n_layers;
  private int loc_tex_coords_shift;
  private int opacity_map_id;
  private float opacity_thresholds[];
  private float x_limits[];
  private float z_limits[];
  private float texture_shift[];

}
