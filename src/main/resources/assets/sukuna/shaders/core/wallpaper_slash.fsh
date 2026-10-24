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
    // 窗口跨显示器或高 DPI 边界时，超出桌面的区域改用窗口定位，不能整片回退纯黑。
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) uv = screen;
    vec3 wallpaper = texture(Sampler0, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
    fragColor = vec4(wallpaper, vertexColor.a) * ColorModulator;
}
