#version 120
uniform sampler2D Sampler0;
uniform vec2 size;
uniform float round;
uniform bool useCrop;

//https://www.shadertoy.com/view/NtVSW1
float sdRoundRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b;
    return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r;
}

vec4 normalBlend(vec4 src, float dst) {
    float edgeSoftness = 1.0f;
    float finalAlpha = 1.0f-smoothstep(0.0f, edgeSoftness, dst);
    return vec4(
    src.rgb, src.a * finalAlpha
    );
}

float rand(vec2 co) {
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec2 fragCoord = gl_TexCoord[0].st * size;

    vec2 cropMin = vec2(8.0, 8.0);
    vec2 cropMax = vec2(16.0, 16.0);

    vec2 texSize = vec2(64.0, 64.0);
    vec2 croppedUV = useCrop ? mix(cropMin / texSize, cropMax / texSize, gl_TexCoord[0].st) : gl_TexCoord[0].st;

    vec4 texColor = texture2D(Sampler0, croppedUV);

    float distRect = sdRoundRect(fragCoord - (size / 2.0f), size / 2.0f - round - 1.0, round);
    vec4 finalOutput = normalBlend(texColor, distRect);

    float factor = 0.005;
    float dither = rand(fragCoord) * factor;
    finalOutput.rgb += dither;

    gl_FragColor = finalOutput;
}