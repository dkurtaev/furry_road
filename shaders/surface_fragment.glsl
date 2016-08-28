uniform sampler2D u_heights_map;
uniform sampler2D u_texture;
uniform sampler2D u_fur_places;
uniform float u_layer_height;  // Value in [0, 1]: 0 is bottom layer, 1 is top.

varying vec2 v_tex_coords;

void main() {
  float is_fur = texture2D(u_fur_places, v_tex_coords)[0];
  if (is_fur == 0.0) {
    gl_FragColor = vec4(0.0);
    return;
  }

  float max_height = texture2D(u_heights_map, v_tex_coords)[0];
  if (u_layer_height <= max_height) {
    gl_FragColor = texture2D(u_texture, v_tex_coords) * u_layer_height;
    gl_FragColor[3] = 1.0;
  } else {
    gl_FragColor = vec4(0.0);
  }
}
