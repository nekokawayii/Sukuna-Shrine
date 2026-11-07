#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float BloodTime;
uniform float FluidMode;
in vec4 vertexColor;
in vec2 texCoord;
in vec3 localPoint;
out vec4 fragColor;
float wave(vec2 p){return sin(p.x*5.7+sin(p.y*4.3+BloodTime*.9))*cos(p.y*6.1-BloodTime*.55);}
void main(){
    vec2 uv=texCoord;
    vec2 flow=vec2(wave(uv+BloodTime*.035),wave(uv.yx-BloodTime*.027))*.065;
    vec3 surface=texture(Sampler0,fract(uv+flow+vec2(BloodTime*.019,-BloodTime*.013))).rgb;
    float vein=pow(max(0.0,1.0-abs(sin((uv.x+flow.x)*17.0+sin(uv.y*13.0+BloodTime)))),12.0);
    float pulse=.78+.22*sin(BloodTime*3.1+uv.y*3.0);
    vec3 fluid=mix(surface*.48,vec3(.65,.012,.035),.36)+vein*pulse*vec3(.25,.018,.027);
    float spec=pow(max(0.0,wave(uv*1.9+flow)),9.0);
    fluid+=spec*vec3(.52,.18,.20);
    float edge=FluidMode<.5?smoothstep(0.0,.12,min(texCoord.y,1.0-texCoord.y)):1.0;
    fragColor=vec4(fluid*vertexColor.rgb*ColorModulator.rgb,vertexColor.a*ColorModulator.a*edge);
    if(fragColor.a<.008)discard;
}
