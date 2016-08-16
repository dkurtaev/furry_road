package furry_road;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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

    KeyListener listener = new KeyListener() {

      @Override
      public void keyTyped(KeyEvent e) {}

      @Override
      public void keyReleased(KeyEvent e) {}

      @Override
      public void keyPressed(KeyEvent e) {
        renderer.KeyPressed(e.getKeyCode());
      }

    };

    addKeyListener(listener);
  }

}
