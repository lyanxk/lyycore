#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform int FogShape;

out vec2 texCoord;
out vec4 vertexColor;
out vec3 viewNormal;
out float materialCrystal;
out float vertexDistance;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    vertexDistance = fog_distance(view.xyz, FogShape);
    texCoord = UV0;
    vertexColor = Color;
    viewNormal = NormalMat * Normal;
    materialCrystal = float(UV1.x);
}
