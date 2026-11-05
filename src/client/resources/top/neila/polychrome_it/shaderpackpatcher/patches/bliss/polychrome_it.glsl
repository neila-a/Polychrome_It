uniform sampler2D polychromeDiffuse;
uniform sampler2D polychromeNormal;
uniform sampler2D polychromeRoughness;
uniform sampler2D polychromeAo;
// 复用 Bliss 已定义的 worldToView（内部用 gbufferModelView），避免重声明/未声明冲突
vec3 worldToView(vec3 worldPos);

// 本 mod 方块识别：
//  - 全精度地形（all_solid 等）已通过 Bliss 现成的 flat varying float blockID（= mc_Entity.x，即本 mod 在
//    block.properties 中为本方块指定的固定 ID）传入，直接比较该 ID 即可（ID 在生成派生包时以 #define 注入）。
//  - DH 等无 blockID 的着色器：走 DhApiBlockColorOverrideEvent 回传的洋红作后备判定。
bool polychromeIsMagenta(vec4 c) {
    float cmax = max(max(c.r, c.g), c.b);
    return (cmax > 0.02) && (c.g < 0.25 * cmax) && (abs(c.r - c.b) < 0.25 * cmax);
}

#ifdef POLYCHROME_HAS_BLOCKID
// 地形着色器：用 blockID（注册表方块 ID）识别，并用洋红作后备
bool polychromeIsOurBlock(vec4 blockColor) {
    bool byId = abs(blockID - float(POLYCHROME_ID_IMAGE_PIXEL_BLOCK)) < 0.5
             || abs(blockID - float(POLYCHROME_ID_SIMPLE_GRADIENT_BLOCK)) < 0.5;
    return byId || polychromeIsMagenta(blockColor);
}
#else
// DH 等无 blockID：仅用洋红后备判定
bool polychromeIsOurBlock(vec4 blockColor) {
    return polychromeIsMagenta(blockColor);
}
#endif

void applyPolychromeIt(inout vec4 albedo, inout vec3 normal, inout float roughness, vec3 worldpos, vec4 blockColor) {
    float cmax = max(max(blockColor.r, blockColor.g), blockColor.b);
    bool isOurBlock = polychromeIsOurBlock(blockColor);
#if POLYCHROME_DEBUG == POLYCHROME_DETECT
    // 隔离诊断（detect）：洋红=命中我们的方块；青色=其它方块；黄色=color 近乎全黑
    if (isOurBlock)       albedo.rgb = vec3(1.0, 0.0, 1.0);
    else if (cmax < 0.02) albedo.rgb = vec3(1.0, 1.0, 0.0);
    else                  albedo.rgb = vec3(0.0, 1.0, 1.0);
    return;
#elif POLYCHROME_DEBUG == POLYCHROME_NORMAL
    // 通道可视化（normal）：把世界法线编码成 RGB（[-1,1] -> [0,1]），确认法线图被采样
    if (isOurBlock) {
        vec2 uv = worldpos.xz / vec2(__SIZE_X__, __SIZE_Z__);
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) albedo.rgb = vec3(0.0);
        else {
            vec3 wn = texture2D(polychromeNormal, uv).xyz * 2.0 - 1.0;
            albedo.rgb = wn * 0.5 + 0.5;
        }
        return;
    }
#elif POLYCHROME_DEBUG == POLYCHROME_ROUGHNESS
    // 通道可视化（roughness）：把粗糙度输出成灰度，确认 roughness 图被采样
    if (isOurBlock) {
        vec2 uv = worldpos.xz / vec2(__SIZE_X__, __SIZE_Z__);
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) albedo.rgb = vec3(0.0);
        else albedo.rgb = texture2D(polychromeRoughness, uv).rgb;
        return;
    }
#elif POLYCHROME_DEBUG == POLYCHROME_AO
    // 通道可视化（ao）：把 AO 图原值输出成灰度，确认 ao 图被采样、方向对齐且数值合理
    if (isOurBlock) {
        vec2 uv = worldpos.xz / vec2(__SIZE_X__, __SIZE_Z__);
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) albedo.rgb = vec3(0.0);
        else albedo.rgb = vec3(texture2D(polychromeAo, uv).r);
        return;
    }
#endif
    if (!isOurBlock) return;
    vec2 uv = worldpos.xz / vec2(__SIZE_X__, __SIZE_Z__);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        // UV 越界：还原为本方块真实染色。colorInWorld 已返回真实色，小地图也读真实色，
        // 不再依赖洋红识别色。
        albedo.rgb = blockColor.rgb;
        return;
    }

    albedo.rgb = texture2D(polychromeDiffuse, uv).rgb;
    // 环境光遮蔽：AO 图原值（白=无遮蔽，黑=全遮蔽）直接压暗反照率，与大多数烘焙 AO 做法一致。
    albedo.rgb *= texture2D(polychromeAo, uv).r;
    vec3 worldNormal = texture2D(polychromeNormal, uv).xyz * 2.0 - 1.0;
    // 退化保护：法线图平坦区若编码为 (0.5,0.5,0.5) 会得到 (0,0,0)，normalize 会出 NaN 让整块变黑
    if (dot(worldNormal, worldNormal) < 1e-4) worldNormal = vec3(0.0, 1.0, 0.0);
    vec3 viewNormal = worldToView(worldNormal);
    if (dot(viewNormal, viewNormal) > 1e-4) {
        normal = normalize(viewNormal); // 变换后仍退化则保留 Bliss 原法线，避免 NaN 传播
    }
    roughness = texture2D(polychromeRoughness, uv).r;
}
