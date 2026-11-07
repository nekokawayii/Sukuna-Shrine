#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 InverseViewProjection;
uniform mat4 ViewProjection;
uniform vec2 FrameSize;
uniform int LightCount;
uniform float Intensity;
uniform vec4 Light0, Light1, Light2, Light3;
uniform vec4 Tint0, Tint1, Tint2, Tint3;
uniform vec4 Shadow0, Shadow1, Shadow2, Shadow3;
uniform vec4 Lens0, Lens1, Lens2, Lens3;
in vec2 uv;
out vec4 fragColor;
const float RANGE = 24.0;
vec3 reconstruct(vec2 p, float depth) {
    vec4 v = InverseViewProjection * vec4(p*2.0-1.0,depth*2.0-1.0,1.0);
    return v.xyz/v.w;
}
vec2 octa(vec3 d) {
    d /= max(0.0001,abs(d.x)+abs(d.y)+abs(d.z));
    vec2 signs = vec2(d.x>=0.0?1.0:-1.0,d.y>=0.0?1.0:-1.0);
    if(d.z<0.0)d.xy=(1.0-abs(d.yx))*signs;
    return d.xy*.5+.5;
}
float visibility(int index, vec3 point, vec4 origin) {
    // 阴影图尚未轮到预算时先保留照明，下一刻自动补遮挡，不能把新光源直接关掉。
    if(origin.w<.5)return 1.0;
    vec3 ray=point-origin.xyz;
    float distance=length(ray);
    vec2 coord=clamp(octa(ray),vec2(.5/48.0),vec2(47.5/48.0));
    vec2 encoded=texture(Sampler2,vec2((float(index)+coord.x)/4.0,coord.y)).rg;
    float obstacle=(encoded.r*65280.0+encoded.g*255.0)/65535.0*RANGE;
    // 角度分辨率产生的偏差随距离增长；不是忽略墙体的径向光晕。
    return 1.0-smoothstep(obstacle+.08,obstacle+.16+distance*.008,distance);
}
// 对当前可见几何再做短程接触阴影，补足方向深度图的边缘，也让实体投下接触阴影。
float contactShadow(vec3 point,vec3 light) {
    vec3 ray=light-point;float distance=length(ray);
    for(int j=1;j<=10;j++) {
        float t=float(j)/11.0;
        vec3 q=point+ray*t;
        vec4 clip=ViewProjection*vec4(q,1.0);
        if(clip.w<=0.0)continue;
        vec2 coord=clip.xy/clip.w*.5+.5;
        if(any(lessThan(coord,vec2(0.0)))||any(greaterThan(coord,vec2(1.0))))continue;
        float d=texture(Sampler1,coord).r;
        if(d>=.999999)continue;
        vec3 surface=reconstruct(coord,d);
        float gap=length(q)-length(surface);
        if(gap>.06 && gap<.20+distance*.012)return 0.0;
    }
    return 1.0;
}
void main() {
    vec4 lights[4]=vec4[4](Light0,Light1,Light2,Light3);
    vec4 tints[4]=vec4[4](Tint0,Tint1,Tint2,Tint3);
    vec4 shadows[4]=vec4[4](Shadow0,Shadow1,Shadow2,Shadow3);
    vec4 lenses[4]=vec4[4](Lens0,Lens1,Lens2,Lens3);
    float depth=texture(Sampler1,uv).r;
    vec3 point=reconstruct(uv,depth);
    vec3 gradient=cross(dFdx(point),dFdy(point));
    vec3 normal=gradient/max(length(gradient),.00001);
    if(dot(normal,-point)<0.0)normal=-normal;
    vec2 sampleUV=uv;
    vec3 halo=vec3(0.0), illumination=vec3(0.0);
    float aspect=FrameSize.x/FrameSize.y;
    for(int i=0;i<4;i++) {
        if(i>=LightCount)break;
        vec4 light=lights[i], tint=tints[i], lens=lenses[i];
        vec3 toLight=light.xyz-point;
        float distance=length(toLight);
        if(depth<.999999 && distance<RANGE) {
            float falloff=pow(max(0.0,1.0-distance/RANGE),2.0)/(1.0+distance*distance*.16);
            float diffuse=max(0.0,dot(normal,toLight/max(distance,.0001)));
            float shadow=visibility(i,point+normal*.025,shadows[i]);
            if(shadow>.01 && diffuse>.01)shadow*=contactShadow(point+normal*.04,light.xyz);
            illumination+=tint.rgb*Intensity*falloff*diffuse*shadow;
        }
        if(lens.z<=0.0)continue;
        vec2 delta=(uv-lens.xy)*vec2(aspect,1.0);
        float r=length(delta)/max(lens.z,.00001);
        // 比球体更近的墙和实体保持原样，不能透墙折射前景。
        bool behind=depth>=.999999 || length(point)>lens.w-light.w*.35;
        if(!behind||r>3.6)continue;
        float haloPower=exp(-r*r*1.4)*.28;
        halo+=tint.rgb*haloPower;
        if(tint.w>.5 && r>.06) {
            float envelope=1.0-smoothstep(2.4,3.6,r);
            // 薄透镜方程 beta = theta - thetaE^2/theta：环内反像，环外放大。
            float bend=1.35*envelope/(r*r+.12);
            vec2 warped=uv-delta/vec2(aspect,1.0)*bend;
            // 不把侧边的前景方块拉进后方透镜；越界钳制避免黑边。
            warped=clamp(warped,1.0/FrameSize,1.0-1.0/FrameSize);
            float warpedDepth=texture(Sampler1,warped).r;
            if(warpedDepth>=.999999||length(reconstruct(warped,warpedDepth))>lens.w)
                sampleUV+=warped-uv;
            float ring=exp(-pow((r-1.02)*22.0,2.0));
            halo+=mix(tint.rgb,vec3(1.0),.65)*ring*.65;
        }
    }
    vec3 base=texture(Sampler0,clamp(sampleUV,vec2(0.0),vec2(1.0))).rgb;
    // 保留黑暗处的材质纹理，以局部色调映射压缩高亮，不把整个场景曝光抬高。
    vec3 lit=1.0-exp(-illumination*(base*.85+.24));
    fragColor=vec4(clamp(base+(1.0-base)*lit+halo,0.0,1.0),1.0);
}
