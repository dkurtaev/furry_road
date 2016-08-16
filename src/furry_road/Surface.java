package furry_road;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferByte;
import java.awt.image.Kernel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class Surface {

  public Surface(float min_x, float max_x, float min_y, float max_y,
                 float min_z, float max_z, int n_nodes_by_x, int n_nodes_by_z) {
    final float dx = (max_x - min_x) / (n_nodes_by_x - 1);
    final float dz = (max_z - min_z) / (n_nodes_by_z - 1);

    Random rand = new Random();

    // Vertices in order: x from max to min, z from min to max for each x.
    // x
    // ^ 0 --> 1
    // | 2 --> 3
    // | 4 --> 5
    // +------> z
    float vertices_data[] = new float[n_nodes_by_x * n_nodes_by_z * 3];
    int offset = 0;
    for (int x = 0; x < n_nodes_by_x; ++x) {
      for (int z = 0; z < n_nodes_by_z; ++z) {
        vertices_data[offset++] = max_x - x * dx;
        vertices_data[offset++] = min_y + (max_y - min_y) * rand.nextFloat();
        vertices_data[offset++] = min_z + z * dz;
      }
    }
    coords_buffer = ByteBuffer.allocateDirect(vertices_data.length *
                                              SIZE_OF_FLOAT)
                              .order(ByteOrder.nativeOrder())
                              .asFloatBuffer();
    coords_buffer.put(vertices_data);
    coords_buffer.position(0);

    // Texture coordinates.
    float tex_coods_data[] = new float[n_nodes_by_x * n_nodes_by_z * 2];
    offset = 0;
    for (int x = 0; x < n_nodes_by_x; ++x) {
      for (int z = 0; z < n_nodes_by_z; ++z) {
        tex_coods_data[offset++] = (float)z / (n_nodes_by_z - 1);
        tex_coods_data[offset++] = (float)x / (n_nodes_by_x - 1);
      }
    }
    tex_coords_buffer = ByteBuffer.allocateDirect(tex_coods_data.length *
                                                  SIZE_OF_FLOAT)
                                  .order(ByteOrder.nativeOrder())
                                  .asFloatBuffer();
    tex_coords_buffer.put(tex_coods_data);
    tex_coords_buffer.position(0);

    // x  00 -- 01 -- 02 -- 03  target indices: 00, 04, 01, 05, 02, 06, 03, 07,
    // ^   |  /  |  /  |  /  |                  07, 11, 06, 10, 05, 09, 04, 08,
    // |  04 -- 05 -- 06 -- 07                  08, 12, 09, 13, 10, 14, 11, 15
    // |   |  /  |  /  |  /  |
    // |  08 -- 09 -- 10 -- 11
    // |   |  /  |  /  |  /  |
    // |  12 -- 13 -- 14 -- 15
    // +----> z
    n_drawing_indices = 2 * (n_nodes_by_x - 1) * n_nodes_by_z;
    short indices[] = new short[n_drawing_indices];
    offset = 0;
    for (int x = 0; x < n_nodes_by_x - 1; ++x) {
      for (int z = 0; z < n_nodes_by_z; ++z) {
        int inc = (x % 2 != 0 ? n_nodes_by_z - 1 - z : z);
        indices[offset++] = (short)(x * n_nodes_by_z + inc);
        indices[offset++] = (short)((x + 1) * n_nodes_by_z + inc);
      }
    }
    indices_buffer = ByteBuffer.allocateDirect(indices.length * SIZE_OF_SHORT)
                               .order(ByteOrder.nativeOrder())
                               .asShortBuffer();
    indices_buffer.put(indices);
    indices_buffer.position(0);

    // Normals.
    float normals_data[] = new float[n_nodes_by_x * n_nodes_by_z * 3];
    for (int x = 1; x < n_nodes_by_x - 1; ++x) {
      for (int z = 1; z < n_nodes_by_z - 1; ++z) {
        offset = 3 * (x * n_nodes_by_z  + z);
        float top_y = vertices_data[offset - 3 * n_nodes_by_z + 1];
        float left_y = vertices_data[offset - 3 + 1];
        float right_y = vertices_data[offset + 3 + 1];
        float bottom_y = vertices_data[offset + 3 * n_nodes_by_z + 1];

        normals_data[offset] = (top_y - bottom_y) / (2 * dx);
        normals_data[offset + 1] = 1.0f;
        normals_data[offset + 2] = (left_y - right_y) / (2 * dx);
      }
    }
    normals_buffer = ByteBuffer.allocateDirect(normals_data.length *
                                               SIZE_OF_FLOAT)
                               .order(ByteOrder.nativeOrder())
                               .asFloatBuffer();
    normals_buffer.put(normals_data);
    normals_buffer.position(0);

  }

  public void InitShaderProgram(GL2 gl) throws Exception {
    shader_program =
        ShaderFactory.CreateShaderProgram(gl, "shaders/surface_shader.vertex",
                                          "shaders/surface_shader.fragment");
    loc_position = gl.glGetAttribLocation(shader_program, "a_position");
    loc_normal = gl.glGetAttribLocation(shader_program, "a_normal");
    loc_tex_coords = gl.glGetAttribLocation(shader_program, "a_tex_coords");
    loc_light = gl.glGetUniformLocation(shader_program, "u_light_vector");
    loc_opacity_map = gl.glGetUniformLocation(shader_program, "u_opacity_map");
    loc_threshold = gl.glGetUniformLocation(shader_program,
                                            "u_opacity_threshold");
  }

  public void Draw(GL2 gl, float light_vector[], int layer_idx) {
    gl.glUseProgram(shader_program);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glEnable(GL2.GL_BLEND);
    gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
    gl.glEnable(GL2.GL_ALPHA_TEST);

    gl.glUniform3fv(loc_light, 1, light_vector, 0);
    gl.glUniform1i(loc_opacity_map, 0);
    gl.glUniform1f(loc_threshold, opacity_thresholds[layer_idx]);

    gl.glEnableVertexAttribArray(loc_position);
    gl.glVertexAttribPointer(loc_position, NUM_VERTEX_COORDS, GL2.GL_FLOAT,
                             false, 0, coords_buffer);

    gl.glEnableVertexAttribArray(loc_normal);
    gl.glVertexAttribPointer(loc_normal, NUM_VERTEX_COORDS, GL2.GL_FLOAT,
                             false, 0, normals_buffer);

    gl.glEnableVertexAttribArray(loc_tex_coords);
    gl.glVertexAttribPointer(loc_tex_coords, 2, GL2.GL_FLOAT, false, 0,
                             tex_coords_buffer);

    gl.glBindTexture(GL.GL_TEXTURE_2D, opacity_map_id);
    gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, n_drawing_indices,
                      GL2.GL_UNSIGNED_SHORT, indices_buffer);

    gl.glDisableVertexAttribArray(loc_normal);
    gl.glDisableVertexAttribArray(loc_position);
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
    float conv_kernel_data[] = {0.11f, 0.11f, 0.11f, 0.11f, 0.11f, 0.11f, 0.11f,
                                0.11f, 0.11f};
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
    int mean = 0, max = 0;
    for (int i = 0; i < dim; ++i) {
      // Byte in [-128, 127]. This means that 127 = 127, 128 = -128, 129 = -127
      int pixel = buffer_data[i];
      if (pixel < 0) {
        pixel += 256;
      }
      mean += pixel;
      if (pixel > max) {
        max = pixel;
      }
    }
    mean /= dim;

    opacity_thresholds = new float[n_surfaces];
    float threshold_step = (float)max / n_surfaces;
    for (int i = 0; i < n_surfaces; ++i) {
      opacity_thresholds[i] = (threshold_step * i) / 255;
    }
  }

  private static final int SIZE_OF_FLOAT = 4;
  private static final int SIZE_OF_SHORT = 2;
  private static final int NUM_VERTEX_COORDS = 3;  // x, y, z.

  private FloatBuffer coords_buffer;
  private FloatBuffer normals_buffer;
  private FloatBuffer tex_coords_buffer;
  private ShortBuffer indices_buffer;
  private int shader_program;
  private int loc_position;
  private int loc_normal;
  private int loc_tex_coords;
  private int loc_light;
  private int loc_opacity_map;
  private int loc_threshold;
  private final int n_drawing_indices;
  private int opacity_map_id;
  private float opacity_thresholds[];

}
