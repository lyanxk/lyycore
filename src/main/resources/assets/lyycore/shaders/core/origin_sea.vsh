// 始源星海：暂不使用。原代码逐行注释保留，恢复前请重新验证。
// #version 150

// in vec3 Position;
// uniform mat4 ModelViewMat;
// uniform mat4 ProjMat;
// uniform float SurfacePass;
// uniform float SeaHeight;
// out vec3 viewDirection;

// void main() {
//     vec3 p = Position;
//     if (SurfacePass > 0.5) p = vec3(Position.x * 2048.0, SeaHeight, Position.z * 2048.0);
//     viewDirection = p;
//     vec4 clip = ProjMat * ModelViewMat * vec4(p, SurfacePass > 0.5 ? 1.0 : 0.0);
//     // Keep the infinite sky at the far plane even with the minimum chunk distance.
//     gl_Position = SurfacePass > 0.5 ? clip : clip.xyww;
// }
