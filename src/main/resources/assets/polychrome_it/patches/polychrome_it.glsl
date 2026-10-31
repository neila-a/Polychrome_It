// Polychrome It! — 世界空间 PBR 覆盖（对 Chocapic13/Bliss 的修改，原署名保留）
uniform sampler2D polychromeDiffuse;
uniform sampler2D polychromeNormal;
uniform sampler2D polychromeSpecular;
// 复用 Bliss 已定义的 worldToView（内部用 gbufferModelView），避免重声明/未声明冲突
vec3 worldToView(vec3 worldPos);

// 识别我们的方块 image_pixel_block：其染色（顶点色 color varying）被设为品红 (1,0,1)。
// 面遮蔽 / AO 会等比例缩放 color，但 r≈b、g≈0 的比例关系保持不变，故用比例判定而非精确值，更稳健。
void applyPolychromeIt(inout vec4 albedo, inout vec3 normal, inout float roughness, vec3 worldpos, vec4 blockColor) {
    float cmax = max(max(blockColor.r, blockColor.g), blockColor.b);
    bool isOurBlock = (cmax > 0.02)
        && (blockColor.g < 0.25 * cmax)
        && (abs(blockColor.r - blockColor.b) < 0.25 * cmax);
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
#elif POLYCHROME_DEBUG == POLYCHROME_SPECULAR
    // 通道可视化（specular）：把粗糙度输出成灰度，确认 specular 图被采样
    if (isOurBlock) {
        vec2 uv = worldpos.xz / vec2(__SIZE_X__, __SIZE_Z__);
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) albedo.rgb = vec3(0.0);
        else albedo.rgb = vec3(texture2D(polychromeSpecular, uv).r);
        return;
    }
#endif
    if (!isOurBlock) return;
    vec2 uv = worldpos.xz / vec2(__SIZE_X__, __SIZE_Z__);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        // UV 越界：撤销品红识别染色（Bliss 在计算 Albedo 时已把顶点色乘进去），
        // 还原为白方块 × 面遮蔽/AO（识别染色是 (1,0,1)，blockColor.r 即缩放系数）。
        // 否则范围外的方块会露出大片品红，DH 的 LOD 顶点色也可能被误检放大。
        albedo.rgb = vec3(blockColor.r);
        return;
    }

    albedo.rgb = texture2D(polychromeDiffuse, uv).rgb;
    vec3 worldNormal = texture2D(polychromeNormal, uv).xyz * 2.0 - 1.0;
    // 退化保护：法线图平坦区若编码为 (0.5,0.5,0.5) 会得到 (0,0,0)，normalize 会出 NaN 让整块变黑
    if (dot(worldNormal, worldNormal) < 1e-4) worldNormal = vec3(0.0, 1.0, 0.0);
    vec3 viewNormal = worldToView(worldNormal);
    if (dot(viewNormal, viewNormal) > 1e-4) {
        normal = normalize(viewNormal); // 变换后仍退化则保留 Bliss 原法线，避免 NaN 传播
    }
    roughness = texture2D(polychromeSpecular, uv).r;
}
