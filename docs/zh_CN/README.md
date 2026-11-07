# 多彩化它！

[English](/docs/en_US/README.md) | 大陆简体中文

从位置或覆盖整个世界的图片中加载全彩的方块颜色、粗糙度、法线甚至是环境光遮蔽。

## 模组支持

- *遥远的地平线*：已支持。
- *Xaero 的地图*：已支持。
- *旅行地图*：支持。

## 使用

### 非基于物理的渲染

1. 在*WorldMachine*中导出高度图和位图。
1. 在*WorldPainter*中导入高度图。
    - 用`polychrome_it:image_pixel_block`覆盖地面。
1. 将位图放在`.minecraft/config/polychrome_it/diffuse.png`。

### 基于物理的渲染

需要*Iris*。  
*Optifine*不起作用。

1. 在*WorldMachine*中导出高度图和材料。
1. 在*WorldPainter*中导入高度图。
    - 用`polychrome_it:image_pixel_block`覆盖地面。
1. 将材料放到`.minecraft/config/polychrome_it`：
    - `roughness.png`
    - `normal.png`
    - `diffuse.png`
    - `ao.png`
1. 给游戏安装合适的光影包。  
    光影包必须是文件夹格式。  
    目前支持的光影包有：
    - *Bliss*
1. 进入游戏，选择带有` + Polychrome It!`的光影包。
1. 在光影包设置中，必须启用资源包支持中的高光反射。
1. 这一点不是必须的，但建议把太阳反射强度调到 2 以上。

## 已知问题

- *Distant Horizons*中 PBR 不起作用。
