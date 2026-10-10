#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec2 FrameSize;
uniform vec4 DesktopRegion;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec2 screen = vec2(gl_FragCoord.x / FrameSize.x, 1.0 - gl_FragCoord.y / FrameSize.y);
    vec2 uv = DesktopRegion.xy + screen * DesktopRegion.zw;
    vec3 wallpaper = vec3(0.0);
    if (all(greaterThanEqual(uv, vec2(0.0))) && all(lessThanEqual(uv, vec2(1.0)))) {
        wallpaper = texture(Sampler0, uv).rgb;
    }
    fragColor = vec4(wallpaper, vertexColor.a) * ColorModulator;
}
