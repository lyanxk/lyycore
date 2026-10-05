// 始源星海：暂不使用。原代码逐行注释保留，恢复前请重新验证。
// #version 150

// #moj_import <lyycore:origin_firmament.glsl>

// in vec3 Position;
// in vec4 Color;
// in vec2 UV0;
// uniform mat4 ModelViewMat;
// uniform mat4 ProjMat;
// uniform float SceneTime;
// uniform float ShardCount;
// out vec4 shardColor;

// float hash(float p) { return fract(sin(p * 127.1 + 311.7) * 43758.5453); }
// mat2 rotate(float a) { return mat2(cos(a),-sin(a),sin(a),cos(a)); }

// void main() {
//     float id = UV0.x;
//     if (id >= ShardCount) {
//         gl_Position = vec4(2.0,2.0,2.0,1.0);
//         shardColor = vec4(0.0);
//         return;
//     }
//     float a = hash(id + 1.1) * 6.2831853;
//     float r = hash(id + 5.3);
//     float seed = hash(id + 31.7);
//     // Permanent broken plates define the torn lip; only detached debris falls.
//     // Both populations share the full contour, including its inward shelf.
//     bool rimPlate = mod(id, 5.0) < 3.0;
//     bool shelf = hash(id+8.2)>0.76;
//     vec2 contour = fragmentRim(hash(id+3.7),hash(id+5.3),shelf);
//     if (rimPlate) {
//         // Bias the plates toward the pink opening so their separated silhouettes
//         // read clearly, with several irregular layers straddling the broken lip.
//         vec2 intoOpening = shelf ? normalize(contour-vec2(0.23,0.68)) : -normalize(contour);
//         contour += intoOpening * (-0.025 + hash(id+7.2)*0.15);
//         contour += vec2(cos(a),sin(a)) * 0.025;
//     } else contour += vec2(cos(a),sin(a)) * (0.008 + hash(id+7.2)*0.035);
//     vec3 center = firmamentDirection(contour) * (150.0 + r*110.0);
//     float lifetime = 12.0 + hash(id+18.0)*8.0;
//     float progress = fract(seed + SceneTime / lifetime);
//     float age = progress * lifetime;
//     float fall = progress * (0.38 + 0.62 * progress);
//     if (!rimPlate) {
//         // Detached flakes dissolve high in the sky instead of reaching the sea.
//         float travel = min(26.0 + r*20.0, center.y*0.32);
//         center.y -= fall * travel;
//         center.xz += vec2(cos(a),sin(a)) * (progress*progress * 6.0);
//         center.xz += vec2(sin(age*0.43+seed*40.0),cos(age*0.31+seed*20.0)) * (progress*1.2);
//     }
//     float size = rimPlate ? 1.2 + pow(hash(id + 9.9),2.0)*7.8 : 0.25 + pow(hash(id + 9.9),3.0)*2.6;
//     float spin = 0.58 * (0.65 + r) * (hash(id+23.1) > 0.5 ? 1.0 : -1.0);
//     // The suspended lip never falls or respawns; it only rocks very gently.
//     float angle = seed * 20.0 + (rimPlate ? sin(SceneTime*0.23+seed*9.0)*0.10 : age*spin);
//     vec3 p = Position * size;
//     p.xy = rotate(angle) * p.xy;
//     p.xz = rotate(angle * 0.63 + a) * p.xz;
//     p.yz = rotate(seed * 9.0 + angle * 0.41) * p.yz;
//     // A celestial direction has no camera translation or walking parallax.
//     vec4 clip = ProjMat * ModelViewMat * vec4(center + p,0.0);
//     gl_Position = clip.xyww;
//     vec3 amethyst = mix(vec3(0.13,0.075,0.30), vec3(0.32,0.14,0.46), hash(id + 15.0));
//     vec3 tint = mix(amethyst, vec3(0.10,0.25,0.50), step(0.82,hash(id + 41.2)));
//     tint += vec3(0.16,0.10,0.20) * pow(Color.r, 3.0);
//     float fade = rimPlate ? 1.0 : smoothstep(0.0,0.035,progress) * (1.0-smoothstep(0.86,1.0,progress));
//     shardColor = vec4(tint * (0.65 + Color.rgb * 0.9) + vec3(0.035,0.018,0.065), fade*0.91);
// }
