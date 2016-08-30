package furry_road;

import java.awt.Font;

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
    final String texture_path = "/home/dkurtaev/Downloads/seed.png";
    final String fur_places_path = "/home/dkurtaev/Downloads/seed_alpha.png";

    GL2 gl = drawable.getGL().getGL2();

    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glEnable(GL2.GL_DEPTH_TEST);

    try {
      surface.Init(gl);
      texture_id = TextureFactory.GenTexture(gl, texture_path);
      fur_places_texture_id = TextureFactory.GenTexture(gl, fur_places_path);
    } catch (Exception e) {
      e.printStackTrace();
    }

    drawable.getAnimator().setUpdateFPSFrames(n_frames_for_fps, null);
    last_events_time = System.currentTimeMillis();
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
    gl.glTranslated(root_y, 0, 0);

    gl.glColor3f(1f, 1f, 1f);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glBindTexture(GL2.GL_TEXTURE_2D, texture_id);
    gl.glBegin(GL2.GL_QUADS);
      gl.glTexCoord2f(1f, 0f); gl.glVertex3f(-10, 0f, -10);
      gl.glTexCoord2f(0f, 0f); gl.glVertex3f(-10, 0f, 10);
      gl.glTexCoord2f(0f, 1f); gl.glVertex3f(10, 0f, 10);
      gl.glTexCoord2f(1f, 1f); gl.glVertex3f(10, 0f, -10);
    gl.glEnd();
    gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

    gl.glPushMatrix();
    for (int i = 0; i < n_surfaces; ++i) {
      surface.Draw(gl, (float)i / (n_surfaces - 1), texture_id,
                   fur_places_texture_id);
      gl.glTranslated(fur_length * Math.sin(angle), surfaces_shift,
                      -fur_length * Math.cos(angle));
    }
    gl.glPopMatrix();

    text_renderer.beginRendering(view_width, view_height);
    text_renderer.setColor(0, 1, 0, 1);
    int fps = (int)drawable.getAnimator().getLastFPS();
    text_renderer.draw("fps: " + fps, 0, 8);
    text_renderer.endRendering();

    long current_time = System.currentTimeMillis();
    if (current_time - last_events_time >= 40) {
      last_events_time = current_time;

      float y0 = 2;
      float y1 = 4;
      float top = 8;
      float y2 = 6;

      if (root_y_inc > 0) {
        if (y0 <= root_y && root_y < y1) {
          angle += (-1.0f / 3.0f * Math.PI - angle) / ((y1 - root_y) / root_y_inc);
        }
        root_y += root_y_inc;
        if (root_y >= top) {
          root_y_inc *= -1;
          root_y = top;
        }
      } else {
        if (y2 < root_y && root_y <= top) {
          angle += (0.25 * Math.PI - angle) / ((root_y - y2) / (-root_y_inc));
        }
        root_y += root_y_inc;
        if (root_y <= y0) {
          root_y_inc *= -1;
          root_y = y0;
        }
      }
    }
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
  private int texture_id;
  private int fur_places_texture_id;


  private float fur_length = 0.015f;
  private float root_y = 2.0f;
  private float angle = (float) (-0.25f * Math.PI);
  private float root_y_inc = 0.1f;
  private long last_events_time;
}
