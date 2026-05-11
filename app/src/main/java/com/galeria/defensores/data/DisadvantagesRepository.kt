package com.galeria.defensores.data

import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.data.GaidenData

object DisadvantagesRepository {
    private val _disadvantages = DisadvantagesData.defaultDisadvantages.toMutableList()
    
    fun getAllDisadvantages(): List<AdvantageItem> {
        return _disadvantages.toList()
    }

    fun addDisadvantage(disadvantage: AdvantageItem) {
        _disadvantages.add(disadvantage)
    }

    fun updateDisadvantage(disadvantage: AdvantageItem) {
        val index = _disadvantages.indexOfFirst { it.id == disadvantage.id }
        if (index != -1) {
            _disadvantages[index] = disadvantage
        }
    }

    fun loadSystem(system: com.galeria.defensores.models.RuleSystem) {
        _disadvantages.clear()
        if (system.id == "3det_alpha_base" || system.isBaseSystem && system.disadvantages.isEmpty()) {
            _disadvantages.addAll(DisadvantagesData.defaultDisadvantages)
        } else {
             if (system.disadvantages.isNotEmpty()) {
                 val mapped = system.disadvantages.map { item ->
                     var isMod = item.isModular
                     var basePt = item.baseCostPt
                     var mods = item.modifiers ?: emptyList()

                     if (!isMod) {
                         // Attempt to upgrade from default disadvantages (legacy JSON)
                         val defaultMatch = DisadvantagesData.defaultDisadvantages.find { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
                         if (defaultMatch != null && defaultMatch.isModular) {
                             isMod = true
                             basePt = defaultMatch.baseCostPt
                             mods = defaultMatch.modifiers
                         } else {
                             // Attempt to upgrade from GaidenData
                             val gaidenDisadvantages = GaidenData.createSystem().disadvantages
                             val gaidenMatch = gaidenDisadvantages.find { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
                             if (gaidenMatch != null && gaidenMatch.isModular) {
                                 isMod = true
                                 basePt = gaidenMatch.baseCostPt
                                 mods = gaidenMatch.modifiers
                             }
                         }
                     }

                     AdvantageItem(
                         id = item.id,
                         name = item.name,
                         description = item.description,
                         cost = item.cost,
                         isModular = isMod,
                         baseCostPt = basePt,
                         modifiers = mods,
                         selectedModifiers = item.selectedModifiers ?: emptyList()
                     )
                 }
                 _disadvantages.addAll(mapped)
             } else {
                 _disadvantages.addAll(DisadvantagesData.defaultDisadvantages)
             }
        }
    }

    fun removeDisadvantage(disadvantage: AdvantageItem) {
        _disadvantages.removeIf { it.id == disadvantage.id }
    }
}
