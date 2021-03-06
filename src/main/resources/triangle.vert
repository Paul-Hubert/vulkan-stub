#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec2 a_position;

out gl_PerVertex {
    vec4 gl_Position;
};

vec2 positions[3] = vec2[](
    vec2(0.0, -0.5),
    vec2(0.5, 0.5),
    vec2(-0.5, 0.5)
);

void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
}