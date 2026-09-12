package io.ejekta.bountiful.content.villager

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.ejekta.bountiful.content.item.DecreeItem
import io.ejekta.bountiful.decree.DecreeSpawnCondition
import io.ejekta.bountiful.decree.DecreeSpawnRank
import com.mojang.serialization.Codec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.functions.LootItemFunction

/**
 * Turns the placeholder decree a wandering-trader trade hands out into a real, populated decree.
 *
 * Minecraft 26.2 moved villager and wandering-trader trades into the `villager_trade` datapack
 * registry, so a mod can no longer build an offer in code the way Bountiful used to. The trade
 * itself is now JSON (see `data/bountiful/villager_trade/`), and the part that has to be decided
 * at hand-out time — which decrees the item actually contains — happens here, as an item modifier
 * on that trade.
 */
class DecreeTradeFunction(private val rank: Int) : LootItemFunction {

    override fun codec(): MapCodec<out LootItemFunction> = MAP_CODEC

    override fun apply(stack: ItemStack, context: LootContext): ItemStack {
        return DecreeItem.create(
            DecreeSpawnCondition.WANDERING_TRADER,
            ranked = rank,
            spawnRank = DecreeSpawnRank.CONSTANT
        )
    }

    companion object {
        val MAP_CODEC: MapCodec<DecreeTradeFunction> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.optionalFieldOf("rank", 1).forGetter { it.rank }
            ).apply(instance, ::DecreeTradeFunction)
        }
    }
}
