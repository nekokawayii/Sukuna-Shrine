#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float BloodTime;
uniform float FluidMode;
out vec4 vertexColor;
out vec2 texCoord;
out vec3 localPoint;
void main(){
    vec3 p=Position;
    // 小振幅多频波，轮廓与表面同时流动，不用旋转硬块冒充流体。
    float pulse=sin(BloodTime*2.4+p.x*1.8+p.z)*sin(p.y*1.3+BloodTime);
    p.y+=pulse*(FluidMode>0.5?0.085:0.018);
    gl_Position=ProjMat*ModelViewMat*vec4(p,1.0);
    vertexColor=Color;texCoord=UV0;localPoint=p;
}
