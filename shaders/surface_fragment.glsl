uniform sampler2D u_opacity_map;
uniform sampler2D u_color_texture;
uniform float u_layer_depth;  // Value in [0, 1]: 0 for bottom layer, 1 for top.

varying vec2 v_tex_coords;

void main() {
  float depth = texture2D(u_opacity_map, v_tex_coords)[0];
  if (depth >= u_layer_depth) {
    if (texture2D(u_color_texture, v_tex_coords) != vec4(1.0, 1.0, 1.0, 1.0)) {
      gl_FragColor = texture2D(u_color_texture, v_tex_coords) * u_layer_depth;
      gl_FragColor[3] = 1.0;
    } else {
      gl_FragColor = vec4(0.0);
    }
  } else {
    gl_FragColor = vec4(0.0);
  }
}
