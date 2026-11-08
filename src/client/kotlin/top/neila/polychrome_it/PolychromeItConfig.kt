package top.neila.polychrome_it

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

@Config(name = MOD_ID)
class PolychromeItConfig : ConfigData {
    @JvmField
    @ConfigEntry.Gui.Tooltip
    var blurTexture = false
}
