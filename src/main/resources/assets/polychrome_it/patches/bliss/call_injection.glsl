{
    float pcRoughness = gl_FragData[1].g;
    applyPolychromeIt(Albedo, normal, pcRoughness, worldpos, color);
    // https://docs.minegraph.cn/labpbrmaterialstandard#-pngq7a_27
    gl_FragData[1].rg = vec2(pcRoughness);
}