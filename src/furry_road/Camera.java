package furry_road;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;

public class Camera {

  public Camera(float radius, float azimuth, float zenith) {
    this.radius = radius;
    this.azimuth = azimuth;
    this.zenith = zenith;
  }

  public void Setup(GL2 gl, int view_width, int view_height) {
    final float z_near = 0.1f;
    final float z_far = 1000.0f;
    final float fovy = 30.0f;
    final float aspect = ((float)view_width) / view_height;

    final float cos_azimuth = (float)Math.cos(Math.toRadians(azimuth));
    final float sin_azimuth = (float)Math.sin(Math.toRadians(azimuth));
    final float cos_zenith = (float)Math.cos(Math.toRadians(zenith));
    final float sin_zenith = (float)Math.sin(Math.toRadians(zenith));

    final float camera_x = radius * sin_azimuth * cos_zenith;
    final float camera_y = radius * sin_zenith;
    final float camera_z = radius * cos_azimuth * cos_zenith;

    GLU glu = new GLU();

    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glLoadIdentity();
    glu.gluPerspective(fovy, aspect, z_near, z_far);
    glu.gluLookAt(camera_x, camera_y, camera_z, 0, 0, 0, 0, 1, 0);
  }

  public void ChangeAzimuth(boolean increment) {
    azimuth += azimuth_inc * (increment ? 1f : -1f);
    if (azimuth < 0.0f) {
      azimuth += 360f;
    }
    if (azimuth >= 360f) {
      azimuth -= 360f;
    }
  }

  public void ChangeZenith(boolean increment) {
    zenith += zenith_inc * (increment ? 1f : -1f);
    zenith = Math.max(-89f, Math.min(zenith, 89f));
  }

  private static final float azimuth_inc = 1.5f;
  private static final float zenith_inc = 1.0f;
  private float radius;
  private float azimuth;
  private float zenith;
}
