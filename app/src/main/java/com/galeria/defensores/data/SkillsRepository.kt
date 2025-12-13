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

    fun removeSkill(skill: AdvantageItem) {
        _skills.removeIf { it.id == skill.id }
    }
}
