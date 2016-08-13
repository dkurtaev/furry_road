package furry_road;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

public class Renderer implements GLEventListener {

  public Renderer() {
    camera = new Camera();
  }

  @Override
  public void init(GLAutoDrawable drawable) {}

  @Override
  public void dispose(GLAutoDrawable drawable) {}

  @Override
  public void display(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();

    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

    camera.Setup(gl, view_width, view_height, 30, 25, 30);

    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glLoadIdentity();

    gl.glColor3f(0.1f, 0.4f, 0.3f);
    gl.glBegin(GL2.GL_QUADS);
      gl.glVertex3f(10, 0, 10);
      gl.glVertex3f(10, 0, -10);
      gl.glVertex3f(-10, 0, -10);
      gl.glVertex3f(-10, 0, 10);
    gl.glEnd();
  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
    GL2 gl = drawable.getGL().getGL2();
    gl.glViewport(0, 0, w, h);
    view_width = w;
    view_height = h;
  }

  private Camera camera;
  private int view_width;
  private int view_height;

}
