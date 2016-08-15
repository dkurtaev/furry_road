package furry_road;

import java.io.File;
import java.util.Scanner;

import com.jogamp.opengl.GL2;

public class ShaderFactory {

  public static
  int CreateShaderProgram(GL2 gl, String vertex_shader_path,
                          String fragment_shader_path) throws Exception {
    int program = gl.glCreateProgram();
    if (program == 0) throw new Exception("Shader program not created");

    int vert_shader = CreateShader(gl, GL2.GL_VERTEX_SHADER,
                                   vertex_shader_path);
    int frag_shader = CreateShader(gl, GL2.GL_FRAGMENT_SHADER,
                                   fragment_shader_path);

    gl.glAttachShader(program, vert_shader);
    gl.glAttachShader(program, frag_shader);

    gl.glLinkProgram(program);
    CheckProgramIsLinked(gl, program);

    gl.glDeleteShader(vert_shader);
    gl.glDeleteShader(frag_shader);

    return program;
  }

  private static
  int CreateShader(GL2 gl, int type, String src_file) throws Exception {
    Scanner scanner = new Scanner(new File(src_file));
    String src = scanner.useDelimiter("//Z").next();
    scanner.close();

    int shader = gl.glCreateShader(type);
    if (shader == 0) throw new Exception("Shader not created");

    gl.glShaderSource(shader, 1, new String[]{src}, null);
    gl.glCompileShader(shader);
    CheckShaderIsCompiled(gl, shader);

    return shader;
  }

  private static
  void CheckShaderIsCompiled(GL2 gl, int shader) throws Exception {
    int success[] = {GL2.GL_FALSE};
    gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, success, 0);
    if (success[0] == GL2.GL_FALSE) {
      int log_length[] = new int[1];
      gl.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, log_length, 0);

      byte log_msg[] = new byte[log_length[0]];
      gl.glGetShaderInfoLog(shader, log_length[0], log_length, 0, log_msg, 0);
      throw new Exception(new String(log_msg));
    }
  }

  private static
  void CheckProgramIsLinked(GL2 gl, int program) throws Exception {
    int success[] = {GL2.GL_FALSE};
    gl.glGetProgramiv(program, GL2.GL_LINK_STATUS, success, 0);
    if (success[0] == GL2.GL_FALSE) {
      int log_length[] = new int[1];
      gl.glGetProgramiv(program, GL2.GL_INFO_LOG_LENGTH, log_length, 0);

      byte log_msg[] = new byte[log_length[0]];
      gl.glGetProgramInfoLog(program, log_length[0], log_length, 0, log_msg, 0);
      throw new Exception(new String(log_msg));
    }
  }

}
