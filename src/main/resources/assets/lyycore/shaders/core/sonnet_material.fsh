#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform sampler2D Sampler4;
uniform vec4 ColorModulator;
uniform vec2 LightUV;
uniform vec2 OverlayUV;
uniform float Crystal;
uniform float CrystalOpacity;
uniform float Gui;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in vec2 texCoord;
in vec4 vertexColor;
in vec3 viewNormal;
in float materialCrystal;
in float vertexDistance;
out vec4 fragColor;

void main() {
    vec4 base = texture(Sampler0, texCoord) * vertexColor * ColorModulator;
    if (base.a < 0.1) discard;
    vec3 normal = normalize(viewNormal);
    float crystal = max(Crystal, materialCrystal);
    if (!gl_FrontFacing && crystal > 0.5) normal = -normal;
    // A stable key light in view space keeps the painted details readable in all
    // item contexts. World brightness still comes from Minecraft's light map.
    float diffuse = dot(normal, normalize(vec3(-0.35, 0.65, 0.68))) * 0.5 + 0.5;
    vec3 light = texelFetch(Sampler2, ivec2(clamp(LightUV, 0.0, 15.0)), 0).rgb;
    float ordinaryToon = mix(0.78, 1.0, smoothstep(0.38, 0.60, diffuse));
    vec3 color = base.rgb * ordinaryToon * light;
    float alpha = base.a;
    if (crystal > 0.5) {
        // MMD additive sphere mapping (envFlag=2). This is a normal lookup;
        // it needs no world reflection capture.
        vec2 sphereUV = clamp(vec2(normal.x, -normal.y) * 0.5 + 0.5, 0.001, 0.999);
        vec3 sphere = texture(Sampler3, sphereUV).rgb;
        vec3 toon = texture(Sampler4, vec2(0.5, 1.0 - diffuse)).rgb;
        color = (base.rgb * mix(vec3(0.65), vec3(1.0), toon) + sphere * 0.85)
                * mix(vec3(0.72), vec3(1.0), light);
        // Clearer broad faces, denser grazing edges and bright painted details.
        // The string and facet highlights stay readable without an extra pass.
        float rim = pow(1.0 - abs(normal.z), 3.0);
        float shine = smoothstep(0.25, 0.9, max(sphere.r, max(sphere.g, sphere.b)));
        float pale = smoothstep(0.65, 0.95, min(base.r, min(base.g, base.b)));
        float coverage = min(1.0, max(CrystalOpacity + rim * 0.22 + shine * 0.12, pale * 0.82));
        alpha *= coverage;
    }
    vec4 overlay = texelFetch(Sampler1, ivec2(OverlayUV), 0);
    color = mix(overlay.rgb, color, overlay.a);
    vec4 result = vec4(color, alpha);
    fragColor = Gui > 0.5 ? result : linear_fog(result, vertexDistance, FogStart, FogEnd, FogColor);
}
