package io.ejekta.bountiful.neoforge

import io.ejekta.bountiful.Bountiful
import io.ejekta.bountiful.bridge.Bountybridge
import io.ejekta.bountiful.config.BountifulIO.doContentReload
import io.ejekta.bountiful.content.BountifulCommands
import io.ejekta.bountiful.content.BountifulContent
import io.ejekta.kambrik.bridge.Kambridge
import io.ejekta.kambrik.neoforge.bridge.KambrikSharedApiNeoForge
import io.ejekta.kambrik.registration.KambrikRegistrar
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.AddServerReloadListenersEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.registries.RegisterEvent
import java.util.concurrent.CompletableFuture

/**
 * Bountiful's NeoForge entrypoint.
 *
 * Kotlin for Forge has no Minecraft 26.2 build, so this is a plain `javafml` mod: the buses come
 * in through the constructor rather than from the KFF globals, and the Kotlin runtime is bundled
 * into the jar by the build.
 */
@Mod(Bountiful.ID)
class BountifulModNeoForge(modEventBus: IEventBus, container: ModContainer) {

    init {
        Bountiful.LOGGER.info("Registering Network Messages..")

        Bountybridge.registerServerMessages()
        Bountybridge.registerClientMessages()

        BountifulContent // trigger init

        NeoForge.EVENT_BUS.addListener(this::registerCommands)
        NeoForge.EVENT_BUS.addListener(this::onGameReload)
        NeoForge.EVENT_BUS.addListener(this::onEntityKilled)
        NeoForge.EVENT_BUS.addListener(this::onServerStarting)

        modEventBus.addListener(this::registerRegistryContent)
        modEventBus.addListener(this::registerPayloads)
        modEventBus.addListener(this::commonSetup)

        if (FMLEnvironment.getDist().isClient) {
            BountifulNeoForgeClient.register(modEventBus, container)
        }

        Bountybridge.registerCriterionStuff()
    }

    private fun onEntityKilled(evt: LivingDeathEvent) {
        evt.source.entity?.let { attacker ->
            (evt.entity.level() as? ServerLevel)?.let { serverWorld ->
                Bountybridge.handleEntityKills(serverWorld, attacker, evt.entity)
            }
        }
    }

    private fun onServerStarting(evt: ServerAboutToStartEvent) {
        Bountybridge.registerJigsawPieces(evt.server)
    }

    private fun onGameReload(evt: AddServerReloadListenersEvent) {
        evt.addListener(
            Identifier.fromNamespaceAndPath(Bountiful.ID, "reload"),
            PreparableReloadListener { sharedState, _, prepBarrier, reloadExecutor ->
                CompletableFuture.supplyAsync({
                    doContentReload(sharedState.resourceManager())
                    Unit
                }, reloadExecutor).thenCompose { prepBarrier.wait(it) }.thenApply { null }
            }
        )
    }

    private fun registerCommands(evt: RegisterCommandsEvent) {
        BountifulCommands.register(evt.dispatcher, evt.buildContext, evt.commandSelection)
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerRegistryContent(evt: RegisterEvent) {
        // This loader keeps the block-state to point-of-interest map itself and fills it in from
        // the type's matchingStates on add, so only the type goes in here.
        evt.register(Registries.POINT_OF_INTEREST_TYPE) {
            it.register(Bountiful.id("bountyboard"), BountifulContent.newBoardPoiType())
        }
        KambrikRegistrar[BountifulContent].content.forEach { entry ->
            evt.register(entry.registry.key() as ResourceKey<out Registry<Any>>) {
                it.register(
                    Identifier.fromNamespaceAndPath(BountifulContent.getId(), entry.itemId),
                    entry.item.value!!
                )
            }
        }
    }

    /** The vendored Kambrik NeoForge bridge queues payloads until NeoForge asks for them. */
    private fun registerPayloads(evt: RegisterPayloadHandlersEvent) {
        (Kambridge as KambrikSharedApiNeoForge).registerPayloads(evt)
    }

    private fun commonSetup(evt: FMLCommonSetupEvent) {
        evt.enqueueWork {
            Bountybridge.registerCompostables()
        }
    }
}
