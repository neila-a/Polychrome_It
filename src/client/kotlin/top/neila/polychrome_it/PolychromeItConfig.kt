package top.neila.polychrome_it

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config

@Config(name = MOD_ID)
class PolychromeItConfig : ConfigData {
    @JvmField
    var blurTexture = false
}
