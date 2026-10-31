    // Polychrome It! — DH LOD 世界空间 PBR 覆盖（对 Chocapic13/Bliss 的修改，原署名保留）
    {
        vec3 pcWorldPos = localPos.xyz + cameraPosition;
        float pcRoughness = 0.0;
        applyPolychromeIt(Albedo, normals, pcRoughness, pcWorldPos, gcolor);
    }
