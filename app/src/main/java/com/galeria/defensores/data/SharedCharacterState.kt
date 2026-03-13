package com.galeria.defensores.data

import com.galeria.defensores.models.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared bridge between CharacterViewModel (writer) and RollViewModel + RuleSystemViewModel (readers).
 * Avoids direct ViewModel-to-ViewModel dependencies.
 */
@Singleton
class SharedCharacterState @Inject constructor() {
    private val _character = MutableStateFlow<Character?>(null)
    val character: StateFlow<Character?> = _character.asStateFlow()

    fun update(character: Character?) {
        _character.value = character
    }
}
