#version 300 es

precision mediump float;

uniform sampler2D uTexture;
uniform vec2 resolution;
uniform vec2 uPointer;
uniform float uVelo;

in vec2 textureCoord;
out vec4 fragColor;

float circle(vec2 uv, vec2 disc_center, float disc_radius, float border_size) {
    uv -= disc_center;
    uv*=resolution;
    float dist = sqrt(dot(uv, uv));
    return smoothstep(disc_radius+border_size, disc_radius-border_size, dist);
}

void main()    {
    vec2 newUV = textureCoord;
    vec4 color = vec4(1., 0., 0., 1.);

    float c = circle(newUV, uPointer, 0.0, 0.8);
    float r = texture(uTexture, newUV.xy += c * (uVelo * .5)).x;
    float g = texture(uTexture, newUV.xy += c * (uVelo * .525)).y;
    float b = texture(uTexture, newUV.xy += c * (uVelo * .55)).z;
    color = vec4(r, g, b, 1.);

    fragColor = color;
}
