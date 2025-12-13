package com.galeria.defensores.data

import com.galeria.defensores.models.AdvantageItem

object SpecializationsRepository {
    private val _specializations = SpecializationsData.defaultSpecializations.toMutableList()
    
    fun getAllSpecializations(): List<AdvantageItem> {
        return _specializations.toList()
    }

    fun addSpecialization(specialization: AdvantageItem) {
        _specializations.add(specialization)
    }

    fun updateSpecialization(specialization: AdvantageItem) {
        val index = _specializations.indexOfFirst { it.id == specialization.id }
        if (index != -1) {
            _specializations[index] = specialization
        }
    }

    fun removeSpecialization(specialization: AdvantageItem) {
        _specializations.removeIf { it.id == specialization.id }
    }
}
