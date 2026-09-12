package io.ejekta.bountiful.forge

import io.ejekta.bountiful.bridge.Bountybridge
import io.ejekta.bountiful.client.AnalyzerScreen
import io.ejekta.bountiful.client.BoardScreen
import io.ejekta.bountiful.client.EditorScreen
import io.ejekta.bountiful.config.BountifulConfigScreen
import io.ejekta.bountiful.content.BountifulContent
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.eventbus.api.bus.BusGroup
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

/**
 * Client-only Forge wiring. Only touched when the mod is running on a client, so a dedicated
 * server never loads the screen classes.
 */
object BountifulForgeClient {

    fun register(modBus: BusGroup) {
        FMLClientSetupEvent.getBus(modBus).addListener(this::initClient)
        BuildCreativeModeTabContentsEvent.BUS.addListener(this::onItemGroups)

        // Puts Bountiful's settings behind the config button on its entry in the mod list.
        MinecraftForge.registerConfigScreen { _, parent -> BountifulConfigScreen(parent) }
    }

    private fun initClient(evt: FMLClientSetupEvent) {
        evt.enqueueWork {
            Bountybridge.registerItemDynamicTextures()
            MenuScreens.register(BountifulContent.BOARD_SCREEN_HANDLER, ::BoardScreen)
            MenuScreens.register(BountifulContent.ANALYZER_SCREEN_HANDLER, ::AnalyzerScreen)
            MenuScreens.register(BountifulContent.EDITOR_SCREEN_HANDLER, ::EditorScreen)
        }
    }

    private fun onItemGroups(evt: BuildCreativeModeTabContentsEvent) {
        Bountybridge.getItemGroups()[evt.tabKey]?.forEach { item ->
            evt.accept(item())
        }
    }
}
