package io.ejekta.bountiful.components

import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
@JvmRecord
data class BountyCompletion(val amounts: Map<String, Int>) {
    @OptIn(ExperimentalUnsignedTypes::class)
    fun modified(func: MutableMap<String, Int>.() -> Unit): BountyCompletion {
        return BountyCompletion(amounts.toMutableMap().apply(func).toMap())
    }
}

