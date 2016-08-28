package furry_road;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class TextureFactory {

  public static int GenTexture(GL2 gl, BufferedImage texture) throws Exception {
    final int width = texture.getWidth();
    final int height = texture.getHeight();

    int ids[]= new int[1];
    gl.glGenTextures(1, ids, 0);

    gl.glBindTexture(GL.GL_TEXTURE_2D, ids[0]);
    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
                       GL.GL_NEAREST);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
                       GL.GL_NEAREST);

    DataBufferByte buffer = (DataBufferByte)texture.getRaster()
                                                   .getDataBuffer();
    byte[] buffer_data = buffer.getData();
    ByteBuffer pixels = ByteBuffer.allocateDirect(buffer_data.length);
    pixels.put(buffer_data);
    pixels.position(0);

    final int n_channels = buffer_data.length / (width * height);
    switch (n_channels) {
      case 1: {
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 1, width, height, 0, GL2.GL_RED,
                        GL.GL_UNSIGNED_BYTE, pixels);
        break;
      }
      case 3: {
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 3, width, height, 0, GL2.GL_BGR,
                        GL.GL_UNSIGNED_BYTE, pixels);
        break;
      }
      default: {
        throw new Exception ("Texture has " + n_channels +
                             " channels (supports 3 or 1).");
      }
    };

    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
    return ids[0];
  }

  public static int GenTexture(GL2 gl, String path) throws Exception {
    BufferedImage img = ImageIO.read(new File(path));
    return GenTexture(gl, img);
  }
}
