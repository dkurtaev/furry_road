package furry_road;

import javax.swing.JFrame;

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

public class GLView extends JFrame {

  public GLView() {
    super("Furry road");

    Renderer renderer = new Renderer();

    GLCanvas glCanvas = new GLCanvas();
    glCanvas.addGLEventListener(renderer);
    add(glCanvas);

    Animator animator = new Animator(glCanvas);
    animator.setRunAsFastAsPossible(true);
    animator.start();
  }

}
