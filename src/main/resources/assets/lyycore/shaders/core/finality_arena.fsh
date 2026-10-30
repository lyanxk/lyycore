#version 150

uniform mat4 InverseViewProjection;
uniform vec3 CameraPosition;
uniform float SceneTime;
uniform float RenderFloor;
in vec2 screenPosition;
in vec3 relativePosition;
out vec4 fragColor;

const float PI = 3.14159265;
const vec3 GOLD = vec3(1.0, 0.79, 0.49);
const vec3 HORIZON = vec3(0.77, 0.65, 0.86);

float hash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1,0,0)), f.x),
                   mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
                   mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);
}

float clouds(vec3 p) {
    float value = 0.0, weight = 0.56;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * weight;
        p = p.yzx * 2.07 + vec3(13.2, 7.1, 3.8);
        weight *= 0.48;
    }
    return value;
}

// Analytic antialiasing keeps fine engravings stable when viewed at a shallow angle.
float line(float distance, float width) {
    float aa = max(fwidth(distance), 0.00001);
    return 1.0 - smoothstep(width, width + aa, abs(distance));
}

vec3 starField(vec3 ray) {
    // Sample a spherical shell through a 3D field, without longitude seams or polar crowding.
    float angularPixel = max(length(dFdx(ray)), length(dFdy(ray)));
    vec3 result = vec3(0);
    for (int layer = 0; layer < 2; layer++) {
        float scale = layer == 0 ? 150.0 : 65.0;
        vec3 p = ray * scale;
        vec3 cell = floor(p);
        float seed = hash(cell + float(layer) * 17.0);
        vec3 center = vec3(hash(cell + 2.0), hash(cell + 7.0), hash(cell + 13.0)) * 0.5 + 0.25;
        vec3 delta = fract(p) - center;
        float baseRadius = layer == 0 ? 0.055 : 0.07;
        // Keep filtering inside the cell and conserve brightness as a point gets wider.
        float radius = clamp(angularPixel * scale * 0.55, baseRadius, 0.10);
        float core = exp(-dot(delta, delta) / (radius * radius));
        core *= baseRadius * baseRadius / (radius * radius);
        float present = step(layer == 0 ? 0.70 : 0.92, seed);
        float twinkle = 0.85 + 0.15 * sin(SceneTime * 0.7 + seed * 90.0);
        result += mix(vec3(0.70, 0.69, 1.0), vec3(1.0, 0.86, 0.67), seed)
                * core * present * twinkle * (layer == 0 ? 2.0 : 3.0);
    }
    return result;
}

vec3 sky(vec3 ray) {
    // A closed, slow motion avoids a visible jump when the animation clock wraps.
    float phase = SceneTime * (2.0 * PI / 4096.0);
    vec3 drift = vec3(sin(phase), cos(phase), 0.0) * 0.18;
    float wisps = clouds(ray * 10.0 + drift);
    float band = dot(ray, normalize(vec3(0.65, -0.65, -0.18)));
    float secondBand = dot(ray, normalize(vec3(-0.72, -0.28, 0.27)));
    float galaxy = exp(-pow((band + (wisps - 0.5) * 0.8) * 4.8, 2.0));
    galaxy += exp(-pow((secondBand + (wisps - 0.5) * 0.5) * 6.0, 2.0)) * 0.55;
    float detail = noise(ray * 105.0 + wisps * 3.0);
    float dust = smoothstep(0.43, 0.74, wisps) * min(galaxy, 1.0);
    vec3 color = vec3(0.016, 0.013, 0.036);
    color += vec3(0.12, 0.055, 0.23) * galaxy * wisps;
    color += vec3(0.34, 0.26, 0.48) * dust * (0.22 + detail);
    color += vec3(0.38, 0.34, 0.33) * pow(dust, 2.5) * detail;
    color += starField(ray) * (0.55 + galaxy * 0.45);

    vec3 orbitRay = ray;
    if (orbitRay.y > 0.04) {
        vec2 p = orbitRay.xz / orbitRay.y - vec2(0.0, -3.8);
        float radius = length(p);
        float angle = atan(p.y, p.x);
        float rings = line(radius - 2.7, 0.0025) + line(radius - 1.84, 0.0012) * 0.45;
        rings += line(radius - 2.19, 0.001) * step(0.35, sin(angle * 100.0)) * 0.28;
        float spokeAngle = abs(fract(angle / (2.0 * PI) * 12.0 + 0.5) - 0.5);
        float gems = exp(-abs(radius - 2.7) * 45.0 - spokeAngle * 350.0);
        float rays = exp(-abs(radius - 2.7) * 6.0 - spokeAngle * 800.0)
                   + exp(-abs(radius - 2.7) * 100.0 - spokeAngle * 70.0);
        color += GOLD * (rings * 0.85 + gems * 1.6 + rays * 0.8) * smoothstep(0.04, 0.12, orbitRay.y);
    }
    float haze = exp(-max(ray.y, 0.0) * 14.0);
    color = mix(color, HORIZON, haze * 0.82);
    color += vec3(0.25, 0.18, 0.28) * exp(-abs(ray.y) * 100.0);
    return color;
}

vec3 floorSurface(vec2 p, float distance) {
    float marble = noise(vec3(p * 0.16, 4.2));
    float grain = noise(vec3(p * 2.4, 8.1));
    float veins = pow(1.0 - abs(sin((p.x + p.y) * 0.16 + marble * 7.0)), 10.0);
    vec3 color = mix(vec3(0.65, 0.60, 0.70), vec3(0.84, 0.81, 0.85), marble);
    color += veins * vec3(0.065, 0.052, 0.058);
    color += (grain - 0.5) * 0.06 + (hash(vec3(floor(p / 4.0), 6.0)) - 0.5) * 0.025;
    vec2 grid = abs(mod(p + 2.0, 4.0) - 2.0);
    float footprint = max(fwidth(p.x), fwidth(p.y));
    float seams = max(line(grid.x, 0.015), line(grid.y, 0.015)) * (1.0 - smoothstep(0.5, 2.0, footprint));
    color = mix(color, vec3(0.49, 0.40, 0.53), seams * 0.44);

    float radius = length(p);
    float rings = line(radius - 12.0, 0.025) + line(radius - 12.6, 0.012)
                + line(radius - 24.0, 0.028) + line(radius - 25.2, 0.012)
                + line(radius - 48.0, 0.028) + line(radius - 49.0, 0.015)
                + line(radius - 72.0, 0.02);
    vec2 largeGrid = abs(mod(p + 8.0, 16.0) - 8.0);
    float paths = max(line(largeGrid.x, 0.012), line(largeGrid.y, 0.012));
    float diamonds = line(largeGrid.x + largeGrid.y - 0.32, 0.02);
    float symbols = line(largeGrid.x, 0.025) * (1.0 - smoothstep(0.3, 0.6, largeGrid.y))
                  + line(largeGrid.y, 0.025) * (1.0 - smoothstep(0.3, 0.6, largeGrid.x));
    float angle = atan(p.y, p.x);
    float ticks = line(sin(angle * 48.0) * radius, 0.025)
                * (1.0 - smoothstep(0.2, 0.5, abs(radius - 24.6)));
    float engravings = clamp(rings + paths * 0.7 + diamonds + symbols + ticks, 0.0, 1.0)
                    * (1.0 - smoothstep(0.5, 4.0, footprint));
    float fade = exp(-distance * 0.003);
    color += vec3(0.30, 0.24, 0.12) * engravings * fade;
    return mix(color, HORIZON + vec3(0.13, 0.10, 0.12), 1.0 - exp(-distance * 0.008));
}

void main() {
    if (RenderFloor > 0.5) {
        fragColor = vec4(floorSurface(CameraPosition.xz + relativePosition.xz, length(relativePosition)), 1.0);
        return;
    }
    vec4 nearPoint = InverseViewProjection * vec4(screenPosition, -1.0, 1.0);
    // A point inside the frustum also works with an infinite far projection.
    vec4 farPoint = InverseViewProjection * vec4(screenPosition, 0.0, 1.0);
    vec3 origin = nearPoint.xyz / nearPoint.w;
    vec3 ray = normalize(farPoint.xyz / farPoint.w - origin);
    fragColor = vec4(sky(ray), 1.0);
}
