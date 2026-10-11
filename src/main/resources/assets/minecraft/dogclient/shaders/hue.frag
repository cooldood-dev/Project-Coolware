#version 120

uniform vec2 size;
uniform float round;

// https://www.shadertoy.com/view/NtVSW1
float sdRoundRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

vec4 normalBlend(vec4 src, float dst) {
    float edgeSoftness = 1.0f;
    float finalAlpha = 1.0f - smoothstep(0.0f, edgeSoftness, dst);
    return vec4(src.rgb, src.a * finalAlpha);
}

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 hsv2rgb(float h, float s, float v) {
    float c = v * s;
    float x = c * (1.0 - abs(mod(h * 6.0, 2.0) - 1.0));
    float m = v - c;

    vec3 rgb;
    if (h < 1.0 / 6.0)      rgb = vec3(c, x, 0.0);
    else if (h < 2.0 / 6.0) rgb = vec3(x, c, 0.0);
    else if (h < 3.0 / 6.0) rgb = vec3(0.0, c, x);
    else if (h < 4.0 / 6.0) rgb = vec3(0.0, x, c);
    else if (h < 5.0 / 6.0) rgb = vec3(x, 0.0, c);
    else                    rgb = vec3(c, 0.0, x);

    return rgb + m;
}

void main() {
    vec2 fragCoord = gl_TexCoord[0].st * size;

    float distRect = sdRoundRect(fragCoord - (size / 2.0f), size / 2.0f - round - 1.0, round);

    float hue = gl_TexCoord[0].st.y;
    vec3 rgb = hsv2rgb(hue, 1.0, 1.0);
    vec4 color = vec4(rgb, 1.0);

    vec4 finalOutput = normalBlend(color, distRect);

    float factor = 0.005;
    float dither = rand(fragCoord) * factor;
    finalOutput.rgb += dither;

    gl_FragColor = finalOutput;
}