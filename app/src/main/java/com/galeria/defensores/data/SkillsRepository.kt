package com.galeria.defensores.data

import com.galeria.defensores.models.AdvantageItem

object SkillsRepository {
    private val _skills = SkillsData.defaultSkills.toMutableList()
    
    fun getAllSkills(): List<AdvantageItem> {
        return _skills.toList()
    }

    fun addSkill(skill: AdvantageItem) {
        _skills.add(skill)
    }

    fun updateSkill(skill: AdvantageItem) {
        val index = _skills.indexOfFirst { it.id == skill.id }
        if (index != -1) {
            _skills[index] = skill
        }
    }

    fun loadSystem(system: com.galeria.defensores.models.RuleSystem) {
        _skills.clear()
        if (system.id == "3det_alpha_base" || system.isBaseSystem && system.skills.isEmpty()) {
            _skills.addAll(SkillsData.defaultSkills)
        } else {
             if (system.skills.isNotEmpty()) {
                 val mapped = system.skills.map { item ->
                     AdvantageItem(
                         id = item.id,
                         name = item.name,
                         description = item.description,
                         cost = item.cost
                     )
                 }
                 _skills.addAll(mapped)
             } else {
                 _skills.addAll(SkillsData.defaultSkills)
             }
        }
    }

    fun removeSkill(skill: AdvantageItem) {
        _skills.removeIf { it.id == skill.id }
    }
}
