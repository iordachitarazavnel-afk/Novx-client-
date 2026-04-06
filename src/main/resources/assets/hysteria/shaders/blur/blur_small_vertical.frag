#version 330 core
in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize; // 1/width, 1/height
uniform float uRadius;   // radius in pixels (<= 3.5)

const float MIN_RADIUS = 0.05;
const float MAX_RADIUS = 3.5;
const float MIN_SIGMA = 0.02;

vec4 computeSmallKernelWeights(float radius) {
    float clampedRadius = clamp(radius, MIN_RADIUS, MAX_RADIUS);
    float sigma = max(clampedRadius * 0.5, MIN_SIGMA);
    float sigma2 = sigma * sigma;
    float twoSigma2 = 2.0 * sigma2;

    float weights[4];
    weights[0] = 1.0;

    float stepFactor = exp(-1.0 / twoSigma2);
    float ratioFactor = exp(-1.0 / sigma2);
    float incremental = stepFactor;
    float current = weights[0];
    float total = weights[0];

    for (int i = 0; i < 3; ++i) {
        current *= incremental;
        weights[i + 1] = current;
        total += 2.0 * current;
        incremental *= ratioFactor;
    }

    float normalization = 1.0 / total;
    return vec4(weights[0], weights[1], weights[2], weights[3]) * normalization;
}

void main() {
    vec4 taps = computeSmallKernelWeights(uRadius);
    vec2 step = vec2(0.0, uTexelSize.y);

    vec4 color = texture(uSource, vUv) * taps.x;
    color += (texture(uSource, vUv + step * 1.0) + texture(uSource, vUv - step * 1.0)) * taps.y;
    color += (texture(uSource, vUv + step * 2.0) + texture(uSource, vUv - step * 2.0)) * taps.z;
    color += (texture(uSource, vUv + step * 3.0) + texture(uSource, vUv - step * 3.0)) * taps.w;

    FragColor = color;
}
