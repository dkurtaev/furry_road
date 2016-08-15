package furry_road;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

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

    // x  00 -- 01 -- 02 -- 03  target indices: 00, 04, 01, 05, 02, 06, 03, 07,
    // ^   |     |     |     |                  07, 11, 06, 10, 05, 09, 04, 08,
    // |  04 -- 05 -- 06 -- 07                  08, 12, 09, 13, 10, 14, 11, 15
    // |   |     |     |     |
    // |  08 -- 09 -- 10 -- 11
    // |   |     |     |     |
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
  }

  public void InitShaderProgram(GL2 gl) throws Exception {
    shader_program =
        ShaderFactory.CreateShaderProgram(gl, "shaders/surface_shader.vertex",
                                          "shaders/surface_shader.fragment");
    loc_position = gl.glGetAttribLocation(shader_program, "a_position");
  }

  public void Draw(GL2 gl) {
    gl.glUseProgram(shader_program);

    gl.glEnableVertexAttribArray(loc_position);
    gl.glVertexAttribPointer(loc_position, NUM_VERTEX_COORDS, GL2.GL_FLOAT,
                             false, 0, coords_buffer);

    gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, n_drawing_indices,
                      GL2.GL_UNSIGNED_SHORT, indices_buffer);

    gl.glDisableVertexAttribArray(loc_position);
    gl.glUseProgram(0);
  }

  private static final int SIZE_OF_FLOAT = 4;
  private static final int SIZE_OF_SHORT = 2;
  private static final int NUM_VERTEX_COORDS = 3;  // x, y, z.

  private FloatBuffer coords_buffer;
  private ShortBuffer indices_buffer;
  private int shader_program;
  private int loc_position;
  private final int n_drawing_indices;

}
