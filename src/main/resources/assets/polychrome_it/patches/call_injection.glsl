    // Polychrome It! — 世界空间 PBR 覆盖（对 Chocapic13/Bliss 的修改，原署名保留）
    {
        float pcRoughness = gl_FragData[1].r;
        applyPolychromeIt(Albedo, normal, pcRoughness, worldpos, color);
        gl_FragData[1].r = pcRoughness;
    }
