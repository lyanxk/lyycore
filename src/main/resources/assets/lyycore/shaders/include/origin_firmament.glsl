// 始源星海：暂不使用。原代码逐行注释保留，恢复前请重新验证。
// // One connected aperture, viewed from inside a broken spherical firmament.
// // Stereographic coordinates cover the entire upper hemisphere without an azimuth seam.
// // The screenshots show different sectors of this SAME rim, not separate portals.
// const int RIM_COUNT = 21;
// const vec2 RIM[21] = vec2[](
//     vec2(-0.66,-0.22), vec2(-0.55,-0.47), vec2(-0.37,-0.49),
//     vec2(-0.16,-0.61), vec2( 0.10,-0.55), vec2( 0.26,-0.68),
//     vec2( 0.49,-0.51), vec2( 0.60,-0.30), vec2( 0.54,-0.08),
//     vec2( 0.73, 0.05), vec2( 0.64, 0.28), vec2( 0.71, 0.48),
//     vec2( 0.51, 0.69), vec2( 0.29, 0.81), vec2( 0.04, 0.76),
//     vec2(-0.13, 0.61), vec2(-0.38, 0.68), vec2(-0.51, 0.47),
//     vec2(-0.70, 0.35), vec2(-0.63, 0.10), vec2(-0.77,-0.04)
// );

// // A single broken shelf projects inward from one sector. The aperture remains
// // connected around it, with the deep pink branching cleft seen in references 4/6.
// const int SHELF_COUNT = 9;
// const vec2 SHELF[9] = vec2[](
//     vec2( 0.57, 0.87), vec2( 0.42, 0.70), vec2( 0.37, 0.54),
//     vec2( 0.23, 0.42), vec2( 0.07, 0.43), vec2(-0.04, 0.54),
//     vec2( 0.04, 0.65), vec2(-0.08, 0.78), vec2(-0.06, 0.90)
// );

// // Angular span is calibrated against the horizon in all six references. The
// // opening is large overhead, but must not replace most of the horizon with pink.
// const float FIRMAMENT_SCALE = 0.70;
// vec2 firmamentUV(vec3 d) { return d.xz / ((1.0 + d.y) * FIRMAMENT_SCALE); }
// vec3 firmamentDirection(vec2 p) {
//     p *= FIRMAMENT_SCALE;
//     float r2 = dot(p,p);
//     return vec3(2.0*p.x, 1.0-r2, 2.0*p.y) / (1.0+r2);
// }

// float rimDistance(vec2 p) {
//     float distance2 = 10.0;
//     bool inside = false;
//     for (int i = 0; i < RIM_COUNT; i++) {
//         vec2 a = RIM[i], b = RIM[(i+1) % RIM_COUNT];
//         vec2 e = b-a, w = p-a;
//         vec2 q = w - e * clamp(dot(w,e) / dot(e,e),0.0,1.0);
//         distance2 = min(distance2,dot(q,q));
//         if ((a.y > p.y) != (b.y > p.y)) {
//             if (p.x < (b.x-a.x)*(p.y-a.y)/(b.y-a.y)+a.x) inside = !inside;
//         }
//     }
//     return sqrt(distance2) * (inside ? -1.0 : 1.0);
// }

// float shelfDistance(vec2 p) {
//     float distance2 = 10.0;
//     bool inside = false;
//     for (int i = 0; i < SHELF_COUNT; i++) {
//         vec2 a = SHELF[i], b = SHELF[(i+1) % SHELF_COUNT];
//         vec2 e = b-a, w = p-a;
//         vec2 q = w - e * clamp(dot(w,e) / dot(e,e),0.0,1.0);
//         distance2 = min(distance2,dot(q,q));
//         if ((a.y > p.y) != (b.y > p.y)) {
//             if (p.x < (b.x-a.x)*(p.y-a.y)/(b.y-a.y)+a.x) inside = !inside;
//         }
//     }
//     return sqrt(distance2) * (inside ? -1.0 : 1.0);
// }

// // Used by the fragment emitter as well as the sky, so debris follows the actual
// // aperture and shelf, including the low, torn sector; it never forms a separate ring.
// vec2 fragmentRim(float index, float along, bool shelf) {
//     if (shelf) {
//         // Only the five exposed inner edges border the aperture. The closing
//         // edges are buried in the vault and must not emit low, stray fragments.
//         int i = 1 + int(index * 5.0);
//         return mix(SHELF[i], SHELF[(i+1) % SHELF_COUNT], along);
//     }
//     int i = int(index * float(RIM_COUNT));
//     return mix(RIM[i], RIM[(i+1) % RIM_COUNT], along);
// }
