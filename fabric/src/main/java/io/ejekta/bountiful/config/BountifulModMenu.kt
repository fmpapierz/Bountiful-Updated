package io.ejekta.bountiful.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen

/**
 * Puts Bountiful's settings behind the config button on its Mod Menu entry.
 *
 * Mod Menu is an optional dependency: this class is only ever loaded by Mod Menu itself through
 * the `modmenu` entrypoint, so nothing here runs when the player does not have it installed.
 */
class BountifulModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory<Screen> { parent -> BountifulConfigScreen(parent) }
}
