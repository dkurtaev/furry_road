package furry_road;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;

public class Camera {

  public void Setup(GL2 gl, int view_width, int view_height,
                    float camera_x, float camera_y, float camera_z) {
    final float z_near = 0.1f;
    final float z_far = 1000.0f;
    final float fovy = 30.0f;
    final float aspect = ((float)view_width) / view_height;

    GLU glu = new GLU();

    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glLoadIdentity();
    glu.gluPerspective(fovy, aspect, z_near, z_far);
    glu.gluLookAt(camera_x, camera_y, camera_z, 0, 0, 0, 0, 1, 0);
  }

}
