package furry_road;

import java.awt.Font;
import java.io.File;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.awt.TextRenderer;

public class Renderer implements GLEventListener {

  public Renderer() {
    camera = new Camera(40, 45, 30);
    surface = new Surface(-10, 10, -10, 10);
    text_renderer = new TextRenderer(new Font("Courier", Font.PLAIN, 14));
  }

  @Override
  public void init(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();

    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glEnable(GL2.GL_DEPTH_TEST);

    try {
      surface.Init(gl);
      color_texture_id = Surface.GenTexture(gl, ImageIO.read(new File("/home/dkurtaev/Downloads/character_jump.png")));
    } catch (Exception e) {
      e.printStackTrace();
    }

    drawable.getAnimator().setUpdateFPSFrames(n_frames_for_fps, null);
  }

  @Override
  public void dispose(GLAutoDrawable drawable) {}

  @Override
  public void display(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();

    gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

    camera.Setup(gl, view_width, view_height);

    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glLoadIdentity();

    gl.glColor3f(1f, 1f, 1f);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glBindTexture(GL2.GL_TEXTURE_2D, color_texture_id);
    gl.glBegin(GL2.GL_QUADS);
      gl.glTexCoord2f(1f, 0f); gl.glVertex3f(-10, 0f, -10);
      gl.glTexCoord2f(0f, 0f); gl.glVertex3f(-10, 0f, 10);
      gl.glTexCoord2f(0f, 1f); gl.glVertex3f(10, 0f, 10);
      gl.glTexCoord2f(1f, 1f); gl.glVertex3f(10, 0f, -10);
    gl.glEnd();
    gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

    gl.glPushMatrix();
    for (int i = 0; i < n_surfaces; ++i) {
      surface.Draw(gl, i, n_surfaces, color_texture_id);
      gl.glTranslatef(0.015f, surfaces_shift, -0.015f);
    }
    gl.glPopMatrix();

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
      case 37: camera.IncrementAzimuth(-1f); break;  // Left arrow.
      case 38: camera.IncrementZenith(1f); break;  // Up arrow.
      case 39: camera.IncrementAzimuth(1f); break;  // Right arrow.
      case 40: camera.IncrementZenith(-1f); break;  // Down arrow.
      default: break;
    }
  }

  public void WheelRotation(int rotation) {
    camera.IncrementRadius(rotation > 0 ? 1f : -1f);
  }

  private Camera camera;
  private int view_width;
  private int view_height;
  private Surface surface;
  private static final float surfaces_shift = 0.025f;
  private static final int n_surfaces = 50;
  // Number of frames after which updates FPS counter.
  private static final int n_frames_for_fps = 30;
  private TextRenderer text_renderer;
  private int color_texture_id;
}
