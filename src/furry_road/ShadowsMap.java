package furry_road;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ShadowsMap {

  public static BufferedImage Build(float opacity_map[][], float light_vector[],
                                    float surfaces_shift, int n_surfaces,
                                    float threshold_step,
                                    float min_x, float max_x,
                                    float min_z, float max_z) {
    final int width = opacity_map[0].length;
    final int height = opacity_map.length;
    // Compute shadow.                 ______
    //        ^                        \     |n   angl = dot(n_normal, -n_light)
    //    \   | plane's normal         l\angl|o
    //     \  |                         i\   |r
    //      \ |                          g\  |m
    // light v|                           h\^|a
    // -------*-------- plane              t\|l
    //
    //
    // --^----p3-*-------------- layer 2   d - constant distance between layers.
    // d |        \ <- light vector
    // --x------p2-*------------ layer 1
    // d |          \
    // --v--------p1-*---------- layer 0
    //               ^
    //               | current fragment
    //
    // Point p2 is p1 - L * light, where L = d / cos(angl)
    //
    // Compute shift of corresponding texture coordinates.
    //     -------p3-*-------------- layer 2
    //               |
    //   ---------p2-*------------ layer 1    texture shift
    //               |                            --->
    // -----------p1-*---------- layer 0
    float light_vec_norm = (float)Math.sqrt(light_vector[0] * light_vector[0] +
                                            light_vector[1] * light_vector[1] +
                                            light_vector[2] * light_vector[2]);
    // (0, 1, 0) is normal to y=0 plane.
    float cos_angle = -light_vector[1] / light_vec_norm;
    float step_by_light = surfaces_shift / cos_angle;
    // Shift from p2 to p1 in world coordinates (projected to y=0 plane).
    float position_shift[] = {step_by_light * light_vector[0],
                              step_by_light * light_vector[2]};
    // Shift between texture coordinates of corresponding vertices.
    float dw = width * position_shift[1] / (max_z - min_z);
    float dh = height * position_shift[0] / (max_x - min_x);

    // 0 - bottom layer, 1 - top layer. Value at shadow map - shadow border.
    // ---------- layer 1.00
    // ---------- layer 0.75    --- lighted fragments
    // ---------- layer 0.50    *** shadowed fragments
    // ---*------ layer 0.25
    // ---*------ layer 0.00
    //    ^
    //    | here 0.25 at shadow map
    float shadow_map[][] = new float[height][width];
    for (int i = n_surfaces - 1; i >= 1; --i) {
      float layer_threshold = i * threshold_step;
      for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
          // Check texel provides shadow.
          if (opacity_map[y][x] >= layer_threshold) {
            // Make shadow to all layers under.
            for (int j = i - 1; j >= 0; --j) {
              float relative_layer_coords = (float)j / (n_surfaces - 1);
              int shifted_y = (int)(y + dh * (i - j));
              int shifted_x = (int)(x + dw * (i - j));
              if (0 <= shifted_x && shifted_x < width &&
                  0 <= shifted_y && shifted_y < height) {
                shadow_map[shifted_y][shifted_x] =
                    Math.max(relative_layer_coords,
                             shadow_map[shifted_y][shifted_x]);
              } else {
                break;
              }
            }
          }
        }
      }
    }

    BufferedImage map = new BufferedImage(width, height,
                                          BufferedImage.TYPE_BYTE_GRAY);
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        float value = shadow_map[y][x];
        map.setRGB(x, y, new Color(value, value, value).getRGB());
      }
    }

    try {
      File outputfile = new File("saved2.png");
      ImageIO.write(map, "png", outputfile);
  } catch (IOException e) {
  }

    return map;
  }

}
