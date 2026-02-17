#include <metal_stdlib>
using namespace metal;

kernel void albumWarp(texture2d<float, access::sample> input [[texture(0)]],
                      texture2d<float, access::write> output [[texture(1)]],
                      constant float &time [[buffer(0)]],
                      uint2 gid [[thread_position_in_grid]]) {
    constexpr sampler s(filter::linear, address::clamp_to_edge);

    float2 size = float2(output.get_width(), output.get_height());
    float2 uv = float2(gid) / size;

    float t = time * 0.16;

    // Large-amplitude warp → samples color regions, not recognizable details
    float2 warp1 = float2(
        sin(uv.y * 2.5 + t) * 0.35 + cos(uv.y * 4.0 - t * 0.8) * 0.2,
        cos(uv.x * 2.5 + t * 0.7) * 0.35 + sin(uv.x * 3.5 + t * 0.5) * 0.2
    );
    float2 warp2 = float2(
        cos(uv.y * 3.0 - t * 0.6) * 0.3 + sin(uv.x * 2.0 + t * 0.9) * 0.15,
        sin(uv.x * 3.0 + t * 0.4) * 0.3 + cos(uv.y * 2.5 - t * 0.7) * 0.15
    );

    // Zoomed-in sampling (0.2~0.3 scale) extracts averaged color blobs
    float4 c1 = input.sample(s, uv * 0.3 + 0.35 + warp1 * 0.3);
    float4 c2 = input.sample(s, uv * 0.25 + float2(0.5, 0.25) + warp2 * 0.25);
    float4 c3 = input.sample(s, uv * 0.2 + float2(0.3, 0.5) - warp1 * 0.2);

    // Organic blending between color samples
    float blend1 = sin(uv.x * 2.0 + uv.y * 2.5 + t * 0.6) * 0.5 + 0.5;
    float blend2 = cos(t * 0.35 + uv.y * 1.5 - uv.x * 0.5) * 0.5 + 0.5;
    float4 color = mix(mix(c1, c2, blend1), c3, blend2);

    // Gentle breathing brightness
    color.rgb *= 0.95 + sin(t * 0.4) * 0.05;

    // Slight saturation boost for vibrancy
    float luma = dot(color.rgb, float3(0.299, 0.587, 0.114));
    color.rgb = mix(float3(luma), color.rgb, 1.2);

    output.write(float4(color.rgb, 1.0), gid);
}
