package io.ejekta.kambrik.forge.bridge

import io.ejekta.kambrik.Kambrik
import io.ejekta.kambrik.bridge.BridgePlatform
import io.ejekta.kambrik.bridge.KambrikSharedApi
import io.ejekta.kambrik.ext.register
import io.ejekta.kambrik.message.KambrikMsg
import io.ejekta.kambrik.registration.KambrikAutoRegistrar
import kotlinx.serialization.KSerializer
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.event.network.CustomPayloadEvent
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.network.Channel
import net.minecraftforge.network.ChannelBuilder
import net.minecraftforge.network.PacketDistributor
import java.nio.file.Path

/**
 * Kambrik's platform bridge for Forge.
 *
 * Forge does not have NeoForge's payload registrar: messages go through a channel that has to be
 * built in one pass, so the registrations Bountiful makes at start-up are collected here and
 * [buildChannel] turns them into the real channel once they are all in.
 */
class KambrikSharedApiForge : KambrikSharedApi {

    override val platform: BridgePlatform
        get() = BridgePlatform.FORGE

    override fun isOnClient(): Boolean = FMLEnvironment.dist.isClient

    override fun isOnServer(): Boolean = FMLEnvironment.dist.isDedicatedServer

    private class ForgeMsgData<M : KambrikMsg>(
        serializer: KSerializer<M>,
        val type: CustomPacketPayload.Type<M>
    ) {
        // Kambrik's shared codec is a plain writeUtf/readUtf pair, which is what Fabric and
        // NeoForge use. Forge needs its own: it hands onPacketReceived a payload buffer whose
        // reader index is already at the end, so readUtf fails on the length varint before it
        // reads a single byte of the message ("readerIndex(27) + length(1) exceeds
        // writerIndex(27)") and the client is kicked with a protocol error. That buffer holds
        // exactly this one message, so rewinding it before decoding is the whole fix.
        val streamCodec: StreamCodec<RegistryFriendlyByteBuf, M> = StreamCodec.ofMember(
            { value, buf -> buf.writeUtf(NETWORK_JSON.encodeToString(serializer, value)) },
            { buf ->
                buf.readerIndex(0)
                NETWORK_JSON.decodeFromString(serializer, buf.readUtf())
            }
        )
    }

    private val clientMsgs = mutableListOf<ForgeMsgData<KambrikMsg>>()
    private val serverMsgs = mutableListOf<ForgeMsgData<KambrikMsg>>()

    private var channel: Channel<CustomPacketPayload>? = null

    @Suppress("UNCHECKED_CAST")
    override fun <M : KambrikMsg> registerClientMessage(
        serializer: KSerializer<M>,
        id: CustomPacketPayload.Type<M>
    ): Boolean {
        clientMsgs.add(
            ForgeMsgData(
                serializer as KSerializer<KambrikMsg>,
                id as CustomPacketPayload.Type<KambrikMsg>
            )
        )
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <M : KambrikMsg> registerServerMessage(
        serializer: KSerializer<M>,
        id: CustomPacketPayload.Type<M>
    ): Boolean {
        serverMsgs.add(
            ForgeMsgData(
                serializer as KSerializer<KambrikMsg>,
                id as CustomPacketPayload.Type<KambrikMsg>
            )
        )
        return true
    }

    /** Called once, after every message has been registered. */
    fun buildChannel(channelId: Identifier) {
        if (channel != null) return

        var flow = ChannelBuilder.named(channelId)
            .networkProtocolVersion(PROTOCOL_VERSION)
            .payloadChannel()
            .play()
            .clientbound()

        for (msg in clientMsgs) {
            Kambrik.Logger.info("Registering ClientMsg: ${msg.type.id}")
            flow = flow.add(msg.type, msg.streamCodec) { payload, ctx ->
                ctx.enqueueWork { payload.onClientReceived() }
            }
        }

        flow = flow.serverbound()

        for (msg in serverMsgs) {
            Kambrik.Logger.info("Registering ServerMsg: ${msg.type.id}")
            flow = flow.add(msg.type, msg.streamCodec) { payload, ctx ->
                ctx.enqueueWork { onServerReceived(payload, ctx) }
            }
        }

        channel = flow.build()
    }

    private fun onServerReceived(payload: KambrikMsg, ctx: CustomPayloadEvent.Context) {
        val sender = ctx.sender ?: return
        payload.onServerReceived(KambrikMsg.MsgContext(sender))
    }

    override fun <M : KambrikMsg> sendMsgToClient(msg: M, player: ServerPlayer) {
        channel?.send(msg, PacketDistributor.PLAYER.with(player))
    }

    override fun <M : KambrikMsg> sendMsgToServer(msg: M) {
        channel?.send(msg, PacketDistributor.SERVER.noArg())
    }

    override fun <T : Any> register(
        autoReg: KambrikAutoRegistrar,
        reg: Registry<T>,
        thingId: String,
        obj: T
    ): T {
        // Forge registers through RegisterEvent; see BountifulModForge.
        reg.register(Identifier.fromNamespaceAndPath(autoReg.getId(), thingId), obj)
        return obj
    }

    override fun <T : BlockEntity> createBlockEntityType(
        factory: (pos: BlockPos, state: BlockState) -> T,
        validBlocks: Set<Block>
    ): BlockEntityType<T> {
        return BlockEntityType(
            BlockEntityType.BlockEntitySupplier { pos, state -> factory(pos, state) },
            validBlocks
        )
    }

    override fun getConfigDir(): Path = Path.of("config")

    private companion object {
        const val PROTOCOL_VERSION = 1

        /** The same format Kambrik uses for its own packet codecs. */
        val NETWORK_JSON = Kambrik.Serial.networkingFormat()
    }
}
