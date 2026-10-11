#version 120
uniform sampler2D textureIn;
uniform vec2 texelSize;
uniform float edgeThreshold;
uniform vec4 color;
uniform float alpha;
void main() {
    vec2 offsets[9] = vec2[](
    vec2(-texelSize.x, texelSize.y), vec2(0.0, texelSize.y), vec2(texelSize.x, texelSize.y),
    vec2(-texelSize.x, 0.0), vec2(0.0, 0.0), vec2(texelSize.x, 0.0),
    vec2(-texelSize.x, -texelSize.y), vec2(0.0, -texelSize.y), vec2(texelSize.x, -texelSize.y)
    );

    float kernelX[9] = float[]( -1.0, 0.0, 1.0, -2.0, 0.0, 2.0, -1.0, 0.0, 1.0 );
    float kernelY[9] = float[](  1.0, 2.0, 1.0,  0.0, 0.0, 0.0, -1.0, -2.0, -1.0 );

    float sumX = 0.0;
    float sumY = 0.0;

    for (int i = 0; i < 9; i++) {
        vec4 currSample = texture2D(textureIn, gl_TexCoord[0].st + offsets[i]);
        float intensity = dot(currSample.rgb, vec3(0.299, 0.587, 0.114));
        sumX += intensity * kernelX[i];
        sumY += intensity * kernelY[i];
    }

    float edge = length(vec2(sumX, sumY));
    float edgeAlpha = smoothstep(edgeThreshold - 0.1, edgeThreshold + 0.1, edge);

    vec4 originalColor = texture2D(textureIn, gl_TexCoord[0].st);

    float innerAlpha = (1.0 - edgeAlpha) * alpha;
    float finalAlpha = max(edgeAlpha, innerAlpha);

    gl_FragColor = vec4(originalColor.rgb, originalColor.a * finalAlpha);
}