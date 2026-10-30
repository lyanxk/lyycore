#version 150

in vec3 Position;
uniform mat4 ViewProjection;
uniform vec3 CameraPosition;
uniform float FloorHeight;
uniform float FloorExtent;
uniform float RenderFloor;
out vec2 screenPosition;
out vec3 relativePosition;

void main() {
    screenPosition = Position.xy;
    relativePosition = vec3(Position.x * FloorExtent, FloorHeight - CameraPosition.y, Position.y * FloorExtent);
    gl_Position = RenderFloor > 0.5
        ? ViewProjection * vec4(relativePosition, 1.0)
        : vec4(Position.xy, 1.0, 1.0);
}
