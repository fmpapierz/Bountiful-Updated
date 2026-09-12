package io.ejekta.bountiful.forge

import io.ejekta.bountiful.Bountiful
import io.ejekta.bountiful.bridge.Bountybridge
import io.ejekta.bountiful.config.BountifulIO.doContentReload
import io.ejekta.bountiful.content.BountifulCommands
import io.ejekta.bountiful.content.BountifulContent
import io.ejekta.kambrik.bridge.Kambridge
import io.ejekta.kambrik.forge.bridge.KambrikSharedApiForge
import io.ejekta.kambrik.registration.KambrikRegistrar
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.registries.RegisterEvent
import java.util.concurrent.CompletableFuture

/**
 * Bountiful's Forge entrypoint.
 *
 * Kotlin for Forge has no Minecraft 26.2 build, so this is a plain `javafml` mod and the jar
 * carries the Kotlin runtime itself. Forge 26.2 also moved to per-event buses, so game events are
 * subscribed through each event's own `BUS` and mod-lifecycle events through the mod bus group.
 */
@Mod(Bountiful.ID)
class BountifulModForge(context: FMLJavaModLoadingContext) {

    init {
        Bountiful.LOGGER.info("Registering Network Messages..")

        Bountybridge.registerServerMessages()
        Bountybridge.registerClientMessages()
        // Forge builds one channel from everything registered above, so this has to come last.
        (Kambridge as KambrikSharedApiForge).buildChannel(Bountiful.id("main"))

        BountifulContent // trigger init

        LivingDeathEvent.BUS.addListener(this::onEntityKilled)
        ServerAboutToStartEvent.BUS.addListener(this::onServerStarting)
        RegisterCommandsEvent.BUS.addListener(this::registerCommands)
        AddReloadListenerEvent.BUS.addListener(this::onGameReload)

        val modBus = context.modBusGroup
        RegisterEvent.getBus(modBus).addListener(this::registerRegistryContent)
        FMLCommonSetupEvent.getBus(modBus).addListener(this::commonSetup)

        if (FMLEnvironment.dist.isClient) {
            BountifulForgeClient.register(modBus)
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

    private fun onGameReload(evt: AddReloadListenerEvent) {
        evt.addListener(
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
        KambrikRegistrar[BountifulContent].content.forEach { entry ->
            evt.register(entry.registry.key() as ResourceKey<out Registry<Any>>) {
                it.register(
                    Identifier.fromNamespaceAndPath(BountifulContent.getId(), entry.itemId),
                    entry.item.value!!
                )
            }
        }
    }

    private fun commonSetup(evt: FMLCommonSetupEvent) {
        evt.enqueueWork {
            Bountybridge.registerCompostables()
        }
    }
}
