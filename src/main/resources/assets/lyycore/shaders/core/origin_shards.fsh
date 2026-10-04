#version 150
in vec4 shardColor;
out vec4 fragColor;
void main() {
    if (shardColor.a < 0.004) discard;
    fragColor = shardColor;
}
