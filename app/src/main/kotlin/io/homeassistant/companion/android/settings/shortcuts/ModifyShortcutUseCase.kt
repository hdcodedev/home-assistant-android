package io.homeassistant.companion.android.settings.shortcuts

import io.homeassistant.companion.android.settings.shortcuts.data.AppShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.HomeShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.settings.shortcuts.data.entities.isValid
import javax.inject.Inject

internal sealed interface ShortcutModification {
    data class Create(val kind: ShortcutKind, val draft: ShortcutDraft) : ShortcutModification
    data class Update(val kind: ShortcutKind, val id: String, val draft: ShortcutDraft) : ShortcutModification
    data class Remove(val kind: ShortcutKind, val id: String) : ShortcutModification
}

internal class ModifyShortcutUseCase @Inject constructor(
    private val appShortcutsRepository: AppShortcutsRepository,
    private val homeShortcutsRepository: HomeShortcutsRepository,
) {
    suspend operator fun invoke(modification: ShortcutModification): ShortcutResult<Unit> {
        if (!areShortcutsSupported()) {
            return ShortcutResult.Error(ShortcutError.AndroidVersionNotSupported)
        }

        return when (modification) {
            is ShortcutModification.Create -> create(modification)
            is ShortcutModification.Update -> update(modification)
            is ShortcutModification.Remove -> remove(modification)
        }
    }

    private suspend fun create(modification: ShortcutModification.Create): ShortcutResult<Unit> {
        val draft = modification.draft
        if (!draft.destination.isValid) {
            return ShortcutResult.Error(ShortcutError.Unknown)
        }

        return when (modification.kind) {
            ShortcutKind.APP -> appShortcutsRepository.create(draft = draft)
            ShortcutKind.HOME -> {
                if (!homeShortcutsRepository.canPinShortcuts()) {
                    ShortcutResult.Error(ShortcutError.HomeShortcutPinningNotSupported)
                } else {
                    homeShortcutsRepository.create(draft)
                }
            }
        }
    }

    private suspend fun update(modification: ShortcutModification.Update): ShortcutResult<Unit> {
        val draft = modification.draft
        if (!draft.destination.isValid) {
            return ShortcutResult.Error(ShortcutError.Unknown)
        }

        return when (modification.kind) {
            ShortcutKind.APP -> appShortcutsRepository.update(
                id = modification.id,
                draft = draft,
            )

            ShortcutKind.HOME -> {
                if (!homeShortcutsRepository.canPinShortcuts()) {
                    ShortcutResult.Error(ShortcutError.HomeShortcutPinningNotSupported)
                } else {
                    homeShortcutsRepository.update(
                        id = modification.id,
                        draft = draft,
                    )
                }
            }
        }
    }

    private suspend fun remove(modification: ShortcutModification.Remove): ShortcutResult<Unit> {
        return when (modification.kind) {
            ShortcutKind.APP -> appShortcutsRepository.delete(modification.id)
            ShortcutKind.HOME -> homeShortcutsRepository.disable(modification.id)
        }
    }
}
