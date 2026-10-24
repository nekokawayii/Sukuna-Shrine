#version 150

uniform vec4 ColorModulator;
uniform float DomainTime;
uniform vec3 PrimaryColor;

in vec4 vertexColor;
in vec2 uv;
out vec4 fragColor;

void main() {
    const float TAU = 6.28318530718;
    float t = DomainTime;
    float longitude = uv.x * TAU;
    float latitude = uv.y;

    // 火山岩纹理：粗裂缝 + 细裂纹 + 缓慢流向地面的熔岩亮带
    float warp = sin(longitude * 3.0 + t * .23) * .6
               + sin(longitude * 7.0 - t * .11) * .16;
    float flow = sin(latitude * 34.0 + warp + t * .55);
    float fine = sin(latitude * 151.0 + sin(longitude * 11.0 + t * .33) * 1.9);
    float lava = sin(latitude * 9.0 - t * .8 + sin(longitude * 5.0 + t * .2) * 1.2);

    float ribbon = pow(max(0.0, flow), 12.0);
    float thread = pow(max(0.0, fine), 34.0);
    float lavaBand = pow(max(0.0, lava), 26.0);
    float polarFade = smoothstep(0.0, .10, latitude) * (1.0 - smoothstep(.90, 1.0, latitude));
    float breathe = .93 + .07 * sin(t * 1.3);

    vec3 base = vec3(.020, .010, .014);
    vec3 color = base + PrimaryColor * (.08 + .22 * ribbon) * breathe;
    color += vec3(.95, .34, .06) * lavaBand * .55 * polarFade;
    color += vec3(.70, .36, .42) * thread * .12 * polarFade;
    color *= vertexColor.rgb * ColorModulator.rgb;

    fragColor = vec4(color, 1.0);   // 主体永远不透明
}
