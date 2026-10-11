#version 120

uniform sampler2D textureIn;
uniform vec4 color;

void main() {
    float textureAlpha = texture2D(textureIn, gl_TexCoord[0].st).a;

    if (textureAlpha == 0.0) {
        discard;
    }

    float finalAlpha = color.a;

    gl_FragColor = vec4(color.rgb, color.a);
}