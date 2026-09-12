package io.ejekta.bountiful.client

import io.ejekta.bountiful.bridge.Bountybridge
import io.ejekta.bountiful.content.BountifulContent
import io.ejekta.kambrik.fabric.bridge.KambrikSharedApiFabric
import io.ejekta.kambrik.message.KambrikMsg
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.screens.MenuScreens

class BountifulClient : ClientModInitializer {

    override fun onInitializeClient() {
        // Kambrik is vendored into Bountiful rather than being a separate mod, so the client half
        // of its Fabric networking bridge is hooked up here instead of from Kambrik's own
        // client entrypoint.
        KambrikSharedApiFabric.clientMessageRegistrar = { id ->
            ClientPlayNetworking.registerGlobalReceiver(id) { payload, _ ->
                (payload as KambrikMsg).onClientReceived()
            }
        }
        KambrikSharedApiFabric.clientMessageSender = { msg ->
            ClientPlayNetworking.send(msg)
        }

        Bountybridge.registerItemDynamicTextures()
        Bountybridge.registerClientMessages()
        MenuScreens.register(BountifulContent.BOARD_SCREEN_HANDLER, ::BoardScreen)
        MenuScreens.register(BountifulContent.ANALYZER_SCREEN_HANDLER, ::AnalyzerScreen)
        MenuScreens.register(BountifulContent.EDITOR_SCREEN_HANDLER, ::EditorScreen)
    }
}
