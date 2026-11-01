{
    vec3 pcWorldPos = localPos.xyz + cameraPosition;
    float pcRoughness = 0.0;
    applyPolychromeIt(Albedo, normals, pcRoughness, pcWorldPos, gcolor);
}
