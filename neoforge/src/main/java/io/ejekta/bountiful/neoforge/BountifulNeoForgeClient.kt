package io.ejekta.bountiful.neoforge

import io.ejekta.bountiful.bridge.Bountybridge
import io.ejekta.bountiful.client.AnalyzerScreen
import io.ejekta.bountiful.client.BoardScreen
import io.ejekta.bountiful.client.EditorScreen
import io.ejekta.bountiful.config.BountifulConfigScreen
import io.ejekta.bountiful.content.BountifulContent
import net.minecraft.client.gui.screens.MenuScreens
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent

/**
 * Client-only NeoForge wiring. Only touched when the mod is running on a client, so a dedicated
 * server never loads the screen classes.
 */
object BountifulNeoForgeClient {

    fun register(modEventBus: IEventBus, container: ModContainer) {
        modEventBus.addListener(this::initClient)
        modEventBus.addListener(this::onRegisterMenuScreens)
        modEventBus.addListener(this::onItemGroups)

        // Puts Bountiful's settings behind the config button on its entry in the mod list.
        container.registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _, parent -> BountifulConfigScreen(parent) }
        }
    }

    private fun initClient(evt: FMLClientSetupEvent) {
        evt.enqueueWork {
            Bountybridge.registerItemDynamicTextures()
        }
    }

    private fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {
        event.register(BountifulContent.BOARD_SCREEN_HANDLER, MenuScreens.ScreenConstructor(::BoardScreen))
        event.register(BountifulContent.ANALYZER_SCREEN_HANDLER, MenuScreens.ScreenConstructor(::AnalyzerScreen))
        event.register(BountifulContent.EDITOR_SCREEN_HANDLER, MenuScreens.ScreenConstructor(::EditorScreen))
    }

    private fun onItemGroups(evt: BuildCreativeModeTabContentsEvent) {
        Bountybridge.getItemGroups()[evt.tabKey]?.forEach { item ->
            evt.accept(item())
        }
    }
}
