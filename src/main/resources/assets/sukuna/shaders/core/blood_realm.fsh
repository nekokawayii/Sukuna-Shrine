#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 InverseViewProjection;
uniform mat4 ViewProjection;
uniform vec3 Center;
uniform float BloodTime;
uniform float Radius;
in vec2 uv;
out vec4 fragColor;
vec3 blood(vec2 p){
 vec2 f=vec2(sin(p.y*4.0+BloodTime*.8),cos(p.x*3.2-BloodTime*.7))*.12;
 vec3 tex=texture(Sampler2,fract(p+f+vec2(BloodTime*.017,-BloodTime*.011))).rgb;
 float veins=pow(1.0-abs(sin(p.x*13.0+sin(p.y*11.0+BloodTime))),10.0);
 return tex*.38+vec3(.16,.002,.014)+veins*vec3(.22,.009,.025);
}
void main(){
 vec4 farPoint=InverseViewProjection*vec4(uv*2.0-1.0,1.0,1.0);
 vec3 ray=normalize(farPoint.xyz/farPoint.w);
 float B=dot(ray,-Center),C=dot(Center,Center)-Radius*Radius;
 float disc=B*B-C;
 if(disc<0.0){fragColor=texture(Sampler0,uv);return;}
 float wall=-B+sqrt(disc);float dist=wall;bool sea=false;
 if(ray.y<-.0001){float water=Center.y/ray.y;if(water>0.0&&water<dist){dist=water;sea=true;}}
 vec3 at=ray*dist;vec3 local=at-Center;
 vec3 color;
 if(sea){
  vec2 p=local.xz*.11;float ripple=sin(length(local.xz)*2.0-BloodTime*3.0)*.025;
  color=blood(p+vec2(ripple));color+=pow(max(0.0,sin(p.x*6.0+p.y*4.0+BloodTime)),18.0)*vec3(.25,.065,.07);
 }else{
  vec3 n=normalize(local);vec2 p=vec2(atan(n.z,n.x)*9.0,asin(n.y)*10.0);
  color=blood(p)*.6;
  // 黑白眼睛由独立通道绘制，这里只保留流动血肉和血管。
 }
 vec4 projected=ViewProjection*vec4(at,1.0);float virtualDepth=projected.z/projected.w*.5+.5;
 float terrainDepth=texture(Sampler1,uv).r;
 // 地形仍保持原碰撞，只把其表面统一换成涌动血肉，不遮住后来绘制的实体。
 if(terrainDepth<virtualDepth-.00001){
  vec4 original=InverseViewProjection*vec4(uv*2.0-1.0,terrainDepth*2.0-1.0,1.0);vec3 surface=original.xyz/original.w-Center;
  vec3 base=texture(Sampler0,uv).rgb;float shade=clamp(dot(base,vec3(.3,.5,.2)),.18,.85);
  color=blood((abs(surface.y)<1.0?surface.xz:vec2(surface.x+surface.z,surface.y))*.16)*(.55+shade*.6);
 }
 float fog=clamp(dist/130.0,0.0,.65);color=mix(color,vec3(.12,.001,.016),fog);
 fragColor=vec4(color,1.0);
}
