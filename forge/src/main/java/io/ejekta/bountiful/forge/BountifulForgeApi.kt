package io.ejekta.bountiful.forge

import io.ejekta.bountiful.bridge.BountifulSharedApi
import net.minecraftforge.fml.ModList

class BountifulForgeApi : BountifulSharedApi {
    override fun isModLoaded(id: String): Boolean = ModList.isLoaded(id)

    override fun registerCompostables() {
        // Compostables are data-driven on Forge, same as NeoForge; see data.neoforge/compostables.
    }
}
