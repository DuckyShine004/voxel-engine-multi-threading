#version 330 core

in vec2 outTextureCoordinates;

in vec4 outVertexColour;

out vec4 fragmentColour;

flat in int outTextureIndex;

uniform sampler2DArray textureArray;

void main() {
    vec4 textureColour = texture(textureArray, vec3(outTextureCoordinates, outTextureIndex));

    if (textureColour.a < 0.1f) {
        discard;
    }

    fragmentColour = textureColour;
    // fragmentColour=outVertexColour;
};
