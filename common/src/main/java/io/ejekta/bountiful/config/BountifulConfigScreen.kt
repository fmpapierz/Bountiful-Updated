package io.ejekta.bountiful.config

import io.ejekta.bountiful.Bountiful
import io.ejekta.bountiful.chaos.ChaosMode
import io.ejekta.bountiful.data.PoolEntry
import net.minecraft.client.Minecraft
import net.minecraft.client.OptionInstance
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.options.OptionsSubScreen
import net.minecraft.network.chat.Component

/**
 * Bountiful's settings screen.
 *
 * Upstream built this with Cloth Config, which has no Forge build for Minecraft 26.2. This is the
 * same set of settings drawn with vanilla widgets instead, so all four loaders show one screen and
 * the mod carries no config-library dependency.
 *
 * The one setting that is not here is `general.dataPathsToExclude`, a list of data paths for pack
 * authors: it stays a field in `config/bountiful/config.json`, which is where a pack author edits
 * it anyway.
 */
class BountifulConfigScreen(lastScreen: Screen) : OptionsSubScreen(
    lastScreen,
    Minecraft.getInstance().options,
    Component.translatable("bountiful.mod.name")
) {

    override fun addOptions() {
        val options = list ?: return
        val config = BountifulIO.configData

        options.addHeader(tr("category.general_board"))
        options.addSmall(
            toggle("board.breakable_boards", config.board.canBreak) { config.board.canBreak = it },
            slider("board.update_frequency", config.board.updateFrequencySecs, 5, 600) {
                config.board.updateFrequencySecs = it
            }
        )
        // Held as tenths so it can be a slider; the config field itself stays a float.
        options.addSmall(
            slider(
                "board.village_gen_frequency",
                (config.board.villageGenFrequency * 10f).toInt(),
                0,
                50,
                label = { value -> Component.literal(String.format("%.1f", value / 10.0)) }
            ) { config.board.villageGenFrequency = it / 10f }
        )

        options.addHeader(tr("category.general_bounty"))
        options.addSmall(
            toggle("bounty.expiry_timers", config.bounty.shouldHaveTimersAndExpire) {
                config.bounty.shouldHaveTimersAndExpire = it
            },
            toggle("bounty.allow_decree_mixing", config.bounty.allowDecreeMixing) {
                config.bounty.allowDecreeMixing = it
            }
        )
        options.addSmall(
            toggle("bounty.reverse_entry_matching_algorithm", config.bounty.reverseMatchingAlgorithm) {
                config.bounty.reverseMatchingAlgorithm = it
            },
            slider(
                "bounty.objective_requirement_multiplier",
                config.bounty.fillerDifficultyModifierPercent,
                -50,
                100,
                label = { value -> tr("bounty.objective_requirement_multiplier.value", value) }
            ) { config.bounty.fillerDifficultyModifierPercent = it }
        )
        options.addSmall(
            slider(
                "bounty.max_number_of_rewards",
                config.bounty.initialCountPreference.max,
                1,
                4,
                label = { value -> tr("bounty.max_number_of_rewards.value", value) }
            ) {
                config.bounty.initialCountPreference =
                    PoolEntry.EntryRange(config.bounty.initialCountPreference.min, it)
            },
            slider("bounty.bonus_time", config.bounty.flatBonusTimePerBountyInSecs, 0, 3600) {
                config.bounty.flatBonusTimePerBountyInSecs = it
            }
        )

        options.addHeader(tr("category.client"))
        options.addSmall(
            toggle("client.completion_toast_messages", config.client.showCompletionToast) {
                config.client.showCompletionToast = it
            }
        )

        options.addHeader(tr("category.debug"))
        options.addSmall(
            toggle("debug.enable_chaos_mode", config.dbg.enabled) { enabled ->
                config.dbg.enabled = enabled
                config.chaosMode = if (enabled) config.chaosMode ?: ChaosMode() else null
            },
            toggle("debug.pack_mode", Bountiful.packMode) { Bountiful.packMode = it }
        )
    }

    /** Writes the edited values back to disk and re-reads them, as the old Cloth screen did. */
    override fun onClose() {
        BountifulIO.reloadConfig()
        super.onClose()
    }

    private fun toggle(path: String, initial: Boolean, onChange: (Boolean) -> Unit): OptionInstance<Boolean> =
        OptionInstance.createBoolean(
            "bountiful.config.$path",
            OptionInstance.cachedConstantTooltip(tr("$path.tooltip")),
            initial
        ) { onChange(it) }

    private fun slider(
        path: String,
        initial: Int,
        min: Int,
        max: Int,
        label: ((Int) -> Component)? = null,
        onChange: (Int) -> Unit
    ): OptionInstance<Int> = OptionInstance(
        "bountiful.config.$path",
        OptionInstance.cachedConstantTooltip(tr("$path.tooltip")),
        { caption, value ->
            val shown = label?.invoke(value) ?: Component.literal(value.toString())
            Component.empty().append(caption).append(": ").append(shown)
        },
        OptionInstance.IntRange(min, max),
        initial.coerceIn(min, max)
    ) { onChange(it) }

    private companion object {
        fun tr(path: String, vararg args: Any): Component =
            Component.translatable("bountiful.config.$path", *args)
    }
}
