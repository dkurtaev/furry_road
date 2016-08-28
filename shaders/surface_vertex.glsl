varying vec2 v_tex_coords;

void main() {
  v_tex_coords = gl_MultiTexCoord0.xy;
  gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
