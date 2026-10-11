#version 120

uniform sampler2D iChannel0;
uniform sampler2D textureMask;
uniform float maskThreshold; // Adjustable threshold

void main() {
    vec4 baseColor = texture2D(iChannel0, gl_TexCoord[0].st);
    vec4 maskColor = texture2D(textureMask, gl_TexCoord[0].st);

    float maskFactor = smoothstep(maskThreshold, 1.0, maskColor.a);
    baseColor.a *= (1.0 - maskFactor);

    gl_FragColor = baseColor;
}