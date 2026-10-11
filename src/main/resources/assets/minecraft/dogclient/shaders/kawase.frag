#version 120

uniform sampler2D iChannel0;
uniform vec2 iResolution;
uniform float radius;

void main() {
    vec4 colorSum = texture2D(iChannel0, gl_TexCoord[0].st);
    colorSum.rgb *= colorSum.a;

    vec2 offsets[4] = vec2[4](
        vec2(radius, radius),
        vec2(radius, -radius),
        vec2(-radius, radius),
        vec2(-radius, -radius)
    );

    for (int i = 0; i < 4; i++) {
        vec4 sampleColor = texture2D(iChannel0, gl_TexCoord[0].st + offsets[i] / iResolution);
        sampleColor.rgb *= sampleColor.a;
        colorSum += sampleColor;
    }

    vec4 finalColor = colorSum / 4.5;
    gl_FragColor = vec4(finalColor.rgb / finalColor.a, finalColor.a / 1.2);
}