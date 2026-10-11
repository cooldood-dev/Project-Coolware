#version 120

uniform vec2 size;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 cornerRadii;

float variableRoundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    vec2 q = abs(p);

    float cornerRadius = (q.x > q.y)
    ? (p.x > 0.0 ? (p.y > 0.0 ? r.z : r.y) : (p.y > 0.0 ? r.w : r.x))
    : (p.x > 0.0 ? (p.y > 0.0 ? r.z : r.y) : (p.y > 0.0 ? r.w : r.x));

    q -= b - cornerRadius;

    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - cornerRadius;

    return dist;
}

vec4 normalBlend(vec4 src, float dst) {
    float edgeSoftness = 1.0;
    float finalAlpha = 1.0 - smoothstep(0.0, edgeSoftness, dst);
    return vec4(src.rgb, src.a * finalAlpha);
}

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 fragCoord = gl_TexCoord[0].st * size;
    vec2 halfSize = size * 0.5;

    float distRect = variableRoundedBoxSDF(fragCoord - halfSize, halfSize - 1.0, cornerRadii);

    vec4 color = mix(
        mix(color1, color2, gl_TexCoord[0].st.y),
        mix(color3, color4, gl_TexCoord[0].st.y),
        gl_TexCoord[0].st.x
    );

    vec4 finalOutput = normalBlend(color, distRect);
    finalOutput.rgb += rand(fragCoord) * 0.005;

    gl_FragColor = finalOutput;
}