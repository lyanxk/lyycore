// 始源星海：暂不使用。原代码逐行注释保留，恢复前请重新验证。
// #version 150

// #moj_import <lyycore:origin_firmament.glsl>

// uniform float SceneTime;
// uniform float SurfacePass;
// uniform float SeaHeight;
// uniform vec2 CameraXZ;
// in vec3 viewDirection;
// out vec4 fragColor;

// float hash31(vec3 p) {
//     p = fract(p * 0.1031);
//     p += dot(p, p.yzx + 33.33);
//     return fract((p.x + p.y) * p.z);
// }

// float noise3(vec3 p) {
//     vec3 i = floor(p), f = fract(p);
//     f = f * f * (3.0 - 2.0 * f);
//     return mix(mix(mix(hash31(i), hash31(i + vec3(1,0,0)), f.x),
//                    mix(hash31(i + vec3(0,1,0)), hash31(i + vec3(1,1,0)), f.x), f.y),
//                mix(mix(hash31(i + vec3(0,0,1)), hash31(i + vec3(1,0,1)), f.x),
//                    mix(hash31(i + vec3(0,1,1)), hash31(i + vec3(1,1,1)), f.x), f.y), f.z);
// }

// float fbm(vec3 p) {
//     float sum = 0.0, amplitude = 0.53;
//     for (int i = 0; i < 5; i++) {
//         sum += noise3(p) * amplitude;
//         p = p * 2.07 + vec3(13.1, 7.7, 5.3);
//         amplitude *= 0.48;
//     }
//     return sum;
// }

// // Linear triangular interpolation gives the rupture hard, glass-like edges.
// float facets(vec2 p) {
//     vec2 i = floor(p), f = fract(p);
//     float a = hash31(vec3(i, 1.7)), b = hash31(vec3(i + vec2(1,0), 1.7));
//     float c = hash31(vec3(i + vec2(0,1), 1.7)), d = hash31(vec3(i + 1.0, 1.7));
//     return f.x + f.y < 1.0 ? a + (b-a)*f.x + (c-a)*f.y
//                           : d + (c-d)*(1.0-f.x) + (b-d)*(1.0-f.y);
// }

// vec3 stars(vec3 d) {
//     // Cube projection keeps the star sizes uniform through the zenith.
//     vec3 ad = abs(d);
//     vec2 uv;
//     float face;
//     if (ad.x > ad.y && ad.x > ad.z) { uv = d.yz / ad.x; face = sign(d.x); }
//     else if (ad.y > ad.z) { uv = d.xz / ad.y; face = 3.0 * sign(d.y); }
//     else { uv = d.xy / ad.z; face = 5.0 * sign(d.z); }
//     vec3 color = vec3(0.0);
//     for (int layer = 0; layer < 3; layer++) {
//         float scale = layer == 0 ? 210.0 : (layer == 1 ? 95.0 : 16.0);
//         vec2 grid = uv * scale;
//         vec2 id = floor(grid);
//         float h = hash31(vec3(id, face + float(layer) * 19.0));
//         vec2 center = vec2(hash31(vec3(id, face + 11.0)), hash31(vec3(id, face + 23.0)));
//         vec2 p = fract(grid) - (0.2 + center * 0.6);
//         float size = layer == 0 ? 0.032 : (layer == 1 ? 0.074 : 0.032);
//         float aa = max(length(fwidth(grid)) * 0.32, 0.008);
//         float core = (1.0 - smoothstep(size, size + aa, length(p))) * size / max(size, aa);
//         float halo = exp(-length(p) * (layer == 2 ? 17.0 : 23.0)) * 0.28;
//         float rays = 0.0;
//         if (layer == 2) {
//             vec2 r = vec2(p.x + p.y, p.x - p.y) * 0.7071;
//             rays = exp(-abs(r.x) * 140.0 - abs(r.y) * 11.0)
//                  + exp(-abs(r.y) * 140.0 - abs(r.x) * 11.0);
//         }
//         float chance = layer == 0 ? 0.98 : (layer == 1 ? 0.925 : 0.97);
//         float twinkle = 0.78 + 0.22 * sin(SceneTime * (0.35 + h) + h * 91.0);
//         vec3 tint = layer == 2 ? mix(vec3(0.15,0.8,1.0), vec3(0.95,0.22,0.83), center.x)
//                               : mix(vec3(0.42,0.57,1.0), vec3(0.95,0.87,1.0), h);
//         color += step(chance,h) * tint * (core + halo + rays * 0.48) * twinkle;
//     }
//     return color;
// }

// vec3 upperSky(vec3 d) {
//     float drift = SceneTime * 0.002;
//     float warp = fbm(d * 3.2 + vec3(0, drift, 4.1));
//     float cloud = fbm(d * 8.4 + warp * 2.8 + vec3(drift, 0, 0));
//     float dust = fbm(d * 18.0 + warp);
//     float band = exp(-pow((d.x * 0.81 + d.z * 0.51 - 0.06) * 3.1, 2.0));
//     vec3 color = mix(vec3(0.062,0.078,0.23), vec3(0.055,0.135,0.33), 1.0 - d.y);
//     float nebula = smoothstep(0.38, 0.74, cloud) * band;
//     vec3 nebulaColor = mix(vec3(0.07,0.29,0.68), vec3(0.32,0.075,0.53),
//                           smoothstep(0.3, 0.7, warp + d.y * 0.1));
//     color += nebulaColor * nebula * (0.45 + dust * 0.95);
//     color += vec3(0.018,0.1,0.26) * pow(max(0.0, dust - 0.35), 2.0) * band;
//     color *= 1.0 - smoothstep(0.56,0.77,dust) * band * 0.32;
//     color += vec3(0.023,0.019,0.05)*band;
//     float cascade = exp(-pow((d.x-d.z*0.31)*8.0,2.0))*smoothstep(0.15,0.55,d.z)
//                     * (1.0-smoothstep(0.68,0.88,d.y));
//     color += mix(vec3(0.065,0.18,0.47),vec3(0.12,0.36,0.75),dust)
//              * cascade * smoothstep(0.31,0.66,cloud);
//     color += stars(d) * (0.6 + 0.35 * (1.0 - nebula));

//     // Flattened blue/purple cloud banks just above the luminous horizon.
//     float horizon = exp(-abs(d.y - 0.01) * 12.0);
//     color += vec3(0.025,0.23,0.44) * horizon;
//     float banks = fbm(vec3(d.x * 6.0, d.y * 42.0 - drift, d.z * 6.0));
//     float bankMask = smoothstep(0.43,0.6,banks) * exp(-pow((d.y - 0.1) * 8.0,2.0));
//     color = mix(color, vec3(0.09,0.47,0.96) * (0.7 + banks * 0.6), bankMask * 0.78);
//     float violet = smoothstep(0.5,0.65,banks) * exp(-pow((d.y - 0.25) * 18.0,2.0));
//     color = mix(color, vec3(0.23,0.075,0.46), violet * 0.65);

//     if (d.y > 0.06) {
//         vec2 q = firmamentUV(d);
//         float outer = rimDistance(q), shelf = shelfDistance(q);
//         float angular = (facets(q * 27.0)-0.5)*0.066 + (facets(q * 67.0)-0.5)*0.025;
//         float edge = max(outer, -shelf) + angular;
//         float pixelEdge = max(fwidth(edge)*0.7,0.0003);
//         float opening = 1.0 - smoothstep(-pixelEdge,pixelEdge,edge);
//         // Small angular gaps break up the solid lip behind the suspended plates.
//         // Keep the main aperture's contour; only its narrow edge is chipped away.
//         float chipped = smoothstep(0.53,0.57,facets(q*62.0+vec2(3.7,1.4)));
//         float brokenLip = smoothstep(0.0,0.012,edge) * (1.0-smoothstep(0.016,0.065,edge));
//         opening = max(opening,chipped*brokenLip);

//         // The low curved underside gives the ceiling depth. Its shape follows the
//         // shared spherical rim; individual sectors descend to different altitudes.
//         float vault = (1.0-smoothstep(0.04,0.19,outer)) * smoothstep(-0.01,0.04,edge);
//         float strata = sin((outer + angular*0.2)*360.0 + dust*3.0)*0.5+0.5;
//         vec3 underside = vec3(0.082,0.055,0.19) + vec3(0.032,0.015,0.075)*strata;
//         underside += vec3(0.16,0.065,0.25)*exp(-abs(edge)*17.0)*dust;
//         color = mix(color, underside, vault*0.70);
//         // The inward shelf catches blue/purple light and crumbles into the cleft.
//         float shelfFace = (1.0-smoothstep(-0.012,0.026,shelf))*(1.0-smoothstep(0.02,0.14,outer));
//         color += mix(vec3(0.055,0.05,0.16),vec3(0.18,0.15,0.4),facets(q*58.0))*shelfFace;
//         float glow = exp(-abs(edge)*33.0);
//         color += vec3(0.42,0.012,0.32)*glow;
//         color += vec3(0.16,0.06,0.19)*exp(-abs(edge)*7.0);
//         color += vec3(0.12,0.025,0.35)*smoothstep(0.4,0.68,dust)*exp(-abs(edge-0.05)*14.0);
//         float veil = fbm(vec3(q * 5.0, 1.5 + drift));
//         vec3 pink = mix(vec3(0.94,0.38,0.85), vec3(1.0,0.72,0.98), smoothstep(0.25,0.72,veil));
//         pink += vec3(0.06,0.07,0.06)*exp(-length(q+vec2(0.15,-0.12))*2.8);
//         pink += stars(d * vec3(-1,1,-1)) * 0.32;
//         color = mix(color, pink, opening);
//         // Fine branching luminous faults outside the main opening.
//         float crack = abs(facets(q * 29.0) - 0.5);
//         float cracks = (1.0-smoothstep(0.007,0.023,crack)) * exp(-max(edge,0.0)*38.0)
//                      * smoothstep(0.009,0.035,edge) * smoothstep(0.42,0.58,veil);
//         color += vec3(0.88,0.35,0.78) * cracks;
//     }
//     return color;
// }

// void main() {
//     vec3 d = normalize(viewDirection);
//     vec3 color;
//     if (d.y < 0.0) {
//         // Reflection of the celestial environment, with slow, long horizontal ripples.
//         float distanceToSea = min(600.0, (SurfacePass > 0.5 ? -SeaHeight : 2.0) / max(-d.y,0.005));
//         vec2 p = d.xz * distanceToSea + CameraXZ;
//         float ripple = sin(p.x * 0.055 + p.y * 0.075 + SceneTime * 0.25)
//                      * sin(p.y * 0.035 - SceneTime * 0.18);
//         vec3 reflected = normalize(vec3(d.x + ripple * 0.003, -d.y, d.z + ripple * 0.005));
//         color = upperSky(reflected) * vec3(0.57,0.66,0.88);
//         color = mix(color, vec3(0.075,0.255,0.56), 0.36);
//         float streak = pow(0.5 + 0.5*sin(-d.y * 124.0 + ripple * 0.65 + SceneTime * 0.12), 14.0);
//         color += vec3(0.0,0.2,0.35) * streak * exp(d.y * 8.0);
//         color += vec3(0.0,0.25,0.5) * exp(d.y * 17.0);
//     } else color = upperSky(d);
//     // Gentle shadow lift keeps the sea and broken glass luminous rather than inky.
//     fragColor = vec4(pow(max(color,vec3(0.0)),vec3(0.90)), 1.0);
// }
