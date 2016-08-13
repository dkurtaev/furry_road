package furry_road;

import javax.swing.JFrame;

public class Main {

  public static void main(String[] args) {
    GLView gl_view = new GLView();
    gl_view.setSize(500, 500);
    gl_view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    gl_view.setVisible(true);
  }

}
