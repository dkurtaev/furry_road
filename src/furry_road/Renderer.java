package furry_road;

import java.awt.Font;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.awt.TextRenderer;

public class Renderer implements GLEventListener {

  public Renderer() {
    camera = new Camera(40, 45, 30);
    surface = new Surface(-10, 10, -10, 10, 16, 16);
    text_renderer = new TextRenderer(new Font("Courier", Font.PLAIN, 14));
  }

  @Override
  public void init(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();

    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    try {
      surface.InitShaderProgram(gl);
    } catch (Exception e) {
      e.printStackTrace();
    }

    surface.GenerateOpacityMap(gl, n_surfaces);

    drawable.getAnimator().setUpdateFPSFrames(n_frames_for_fps, null);
  }

  @Override
  public void dispose(GLAutoDrawable drawable) {}

  @Override
  public void display(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();

    gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

    camera.Setup(gl, view_width, view_height);

    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glLoadIdentity();

    gl.glColor3f(0.5f, 0.8f, 0.3f);
    for (int i = 0; i < n_surfaces; ++i) {
      surface.Draw(gl, new float[]{-1.0f, -1.0f, -1.0f}, i);
      gl.glTranslatef(0.0f, surfaces_shift, 0.0f);
    }

    text_renderer.beginRendering(view_width, view_height);
    text_renderer.setColor(0, 1, 0, 1);
    int fps = (int)drawable.getAnimator().getLastFPS();
    text_renderer.draw("fps: " + fps, 0, 8);
    text_renderer.endRendering();
  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
    GL2 gl = drawable.getGL().getGL2();
    gl.glViewport(0, 0, w, h);
    view_width = w;
    view_height = h;
  }

  public void KeyPressed(int key_code) {
    switch (key_code) {
      case 37: camera.ChangeAzimuth(false); break;  // Left arrow.
      case 38: camera.ChangeZenith(true); break;  // Up arrow.
      case 39: camera.ChangeAzimuth(true); break;  // Right arrow.
      case 40: camera.ChangeZenith(false); break;  // Down arrow.
      default: break;
    }
  }

  private Camera camera;
  private int view_width;
  private int view_height;
  private Surface surface;
  private static final float surfaces_shift = 0.05f;
  private static final int n_surfaces = 50;
  // Number of frames after which updates FPS counter.
  private static final int n_frames_for_fps = 30;
  private TextRenderer text_renderer;

}
