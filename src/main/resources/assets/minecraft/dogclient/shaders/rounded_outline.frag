#version 120

uniform vec2 size;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform float width;
uniform float round;

// Signed distance function for a rounded rectangle
float sdRoundRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

// Normal blending for smooth transitions
vec4 normalBlend(vec4 src, float alpha) {
    float edgeSoftness = 1.0;
    float finalAlpha = 1.0 - smoothstep(0.0, edgeSoftness, alpha);
    return vec4(src.rgb, src.a * finalAlpha);
}

// Random function for dithering
float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 fragCoord = gl_TexCoord[0].st * size;
    vec2 center = size / 2.0;

    float distOuter = sdRoundRect(fragCoord - center, size / 2.0 - round - 1.0, round);
    float distInner = sdRoundRect(fragCoord - center, size / 2.0 - round - 1.0 - (width / 2.0), round - (width / 2.0) - 1.0);

    float dynamicWidth = width / (1.0 + abs(distOuter) * 0.5);
    float stroke = smoothstep(-0.5, 0.5, -distOuter) - smoothstep(-0.5, 0.5, -distInner + dynamicWidth);

    vec4 color = mix(
        mix(color1, color2, gl_TexCoord[0].st.y),
        mix(color3, color4, gl_TexCoord[0].st.y),
        gl_TexCoord[0].st.x
    );

    vec4 finalOutput = color * stroke;

    float factor = 0.005;
    float dither = rand(fragCoord) * factor;
    finalOutput.rgb += dither;

    gl_FragColor = finalOutput;
}