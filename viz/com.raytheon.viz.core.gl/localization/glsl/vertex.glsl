// Default vertex shader program for texture based fragment shaders

void main(void) {
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	gl_ClipVertex = gl_ModelViewMatrix * gl_Vertex;
	gl_TexCoord[0] = gl_MultiTexCoord0;
}