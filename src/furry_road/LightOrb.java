package furry_road;

import java.util.Random;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.gl2.GLUT;

public class LightOrb {


  public LightOrb(float orbit_radius) {
    this.orbit_radius = orbit_radius;
    rotation_axis = new float[2];
    SetRotationAxis();
  }

  public void Draw(GL2 gl) {
    final int n_slices = 10;
    final int n_stacks = 10;

    GLUT glut = new GLUT();
    gl.glColor4f(1f, 1f, 1f, 1f);

    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glPushMatrix();
      gl.glRotatef(angle, rotation_axis[0], 0f, rotation_axis[1]);
      gl.glTranslatef(0f, orbit_radius, 0f);
      glut.glutSolidSphere(orb_radius, n_slices, n_stacks);
    gl.glPopMatrix();

    angle += rotation_angle;
    if (angle >= 360f) {
      angle = 360f - angle;
      SetRotationAxis();
    }
  }

  private void SetRotationAxis() {
    Random rand = new Random();
    float norm = 0;
    for (int i = 0; i < rotation_axis.length; ++i) {
      rotation_axis[i] = rand.nextFloat();
      norm += rotation_axis[i] * rotation_axis[i];
    }
    norm = (float)Math.sqrt(norm);
    for (int i = 0; i < rotation_axis.length; ++i) {
      rotation_axis[i] /= norm;
    }
  }

  private static final float orb_radius = 0.5f;
  private static final float rotation_angle = 0.2f;

  private float orbit_radius;
  // Angle of rotation respectively initial position.
  private float angle;
  private float rotation_axis[];

}
