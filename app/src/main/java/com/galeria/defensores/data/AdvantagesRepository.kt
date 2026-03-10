package com.galeria.defensores.data

import com.galeria.defensores.models.AdvantageItem

object AdvantagesRepository {
    private val _advantages = AdvantagesData.defaultAdvantages.toMutableList()
    
    fun getAllAdvantages(): List<AdvantageItem> {
        return _advantages.toList()
    }

    fun addAdvantage(advantage: AdvantageItem) {
        _advantages.add(advantage)
    }

    fun updateAdvantage(advantage: AdvantageItem) {
        val index = _advantages.indexOfFirst { it.id == advantage.id }
        if (index != -1) {
            _advantages[index] = advantage
        }
    }

    fun loadSystem(system: com.galeria.defensores.models.RuleSystem) {
        _advantages.clear()
        if (system.id == "3det_alpha_base" || system.isBaseSystem && system.advantages.isEmpty()) {
            _advantages.addAll(AdvantagesData.defaultAdvantages)
        } else {
             // If the system has its own advantages (like Gaiden), utilize them
             // We might want to MERGE with defaults if the system says so, but usually it replaces.
             // For Gaiden, it replaces.
             if (system.advantages.isNotEmpty()) {
                 val gaidenAdvantages = GaidenData.createSystem().advantages
                 val mapped = system.advantages.map { item ->
                     var isMod = item.isModular
                     var basePt = item.baseCostPt
                     var mods = item.modifiers ?: emptyList()

                     if (!isMod) {
                         // Attempt to upgrade from default advantages (legacy JSON)
                         val defaultMatch = AdvantagesData.defaultAdvantages.find { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
                         if (defaultMatch != null && defaultMatch.isModular) {
                             isMod = true
                             basePt = defaultMatch.baseCostPt
                             mods = defaultMatch.modifiers
                         } else {
                             // Attempt to upgrade from GaidenData
                             val gaidenMatch = gaidenAdvantages.find { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
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
                 _advantages.addAll(mapped)
             } else {
                 // Fallback to defaults if empty? Or maybe it really has none?
                 // For now, fallback to defaults to be safe if it's "3DeT Alpha" but custom, 
                 // but if it's a completely new system with empty list, maybe it should be empty.
                 // Let's stick to: if it is explicitly the base system OR has no advantages defined, use defaults.
                 _advantages.addAll(AdvantagesData.defaultAdvantages)
             }
        }
    }

    fun removeAdvantage(advantage: AdvantageItem) {
        _advantages.removeIf { it.id == advantage.id }
    }
}
