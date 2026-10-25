#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform float DomainTime;
uniform vec3 PrimaryColor;

in vec4 vertexColor;
in vec2 uv;
out vec4 fragColor;

void main() {
    const float TAU = 6.28318530718;
    float t = DomainTime;
    // 横向 8 次、纵向 4 次整数重复：u=0 与 u=1 接缝不跳
    vec2 baseUV = uv * vec2(8.0, 4.0);
    baseUV += vec2(t * .015, -t * .008);
    baseUV.x += sin(uv.y * TAU * 3.0 + t * .3) * .018;

    vec3 surface = texture(Sampler0, baseUV).rgb;
    float mask = texture(Sampler1, baseUV).r;
    float polar = smoothstep(.02, .16, uv.y) * (1.0 - smoothstep(.84, .98, uv.y));
    float pulse = .72 + .28 * sin(t * 1.3 + uv.y * 12.0);
    // 顶点绕序朝外：正面=外部，背面=内部；内部纹路略亮
    float insideBoost = gl_FrontFacing ? .75 : 1.0;

    vec3 background = vec3(.020, .010, .014);
    vec3 body = surface * (vec3(.16) + PrimaryColor * .65);
    vec3 glow = PrimaryColor * mask * pulse * .55 * polar * insideBoost
              + vec3(1.0, .34, .06) * mask * mask * .28 * polar;
    vec3 color = mix(background, body, .75) + glow;
    color *= vertexColor.rgb * ColorModulator.rgb;
    fragColor = vec4(color, 1.0);   // 结界本体不透明
}
