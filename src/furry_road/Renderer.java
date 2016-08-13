package furry_road;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

public class Renderer implements GLEventListener {

  @Override
  public void init(GLAutoDrawable drawable) {}

  @Override
  public void dispose(GLAutoDrawable drawable) {}

  @Override
  public void display(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();
    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
    GL2 gl = drawable.getGL().getGL2();
    gl.glViewport(0, 0, w, h);
  }

}
