package com.galeria.defensores.domain.usecases

import com.galeria.defensores.models.Character
import com.galeria.defensores.models.RollResult
import com.galeria.defensores.models.RollType
import kotlin.random.Random

/**
 * Use Case to calculate the result of a standard 3DeT dice roll (Attribute + Skill + 1d6 + Bonus).
 */
class CalculateStandardRollUseCase {
    
    operator fun invoke(
        char: Character,
        type: RollType,
        bonus: Int,
        attrVal: Int,
        skillVal: Int,
        diceOverride: Int? = null,
        getAttributeName: (String) -> String
    ): RollResult {
        // Determine Die Result
        val die = diceOverride ?: (Random.nextInt(6) + 1)
        val isCritical = die == 6
        val effectiveAttr = if (isCritical) attrVal * 2 else attrVal
        val total = effectiveAttr + skillVal + die + bonus
        
        val displayAttr = when(type) {
             RollType.ATTACK_F -> getAttributeName("forca")
             RollType.ATTACK_PDF -> getAttributeName("poderFogo")
             RollType.DEFENSE -> getAttributeName("armadura")
             RollType.INITIATIVE -> "Iniciativa"
             else -> "Atributo"
        }

        val attrDetail = if (isCritical) "$displayAttr [$attrVal x2!]" else "$displayAttr [$attrVal]"
        val bonusDetail = if (bonus != 0) " + Bônus [$bonus]" else ""
        val dieDetail = if (isCritical) "1d6 [6!]" else "1d6 [$die]"
        
        val details = "$attrDetail + Hab [$skillVal] + $dieDetail$bonusDetail = $total"

        return RollResult(
            total = total,
            die = die,
            attributeUsed = displayAttr,
            attributeValue = attrVal,
            skillValue = skillVal,
            bonus = bonus,
            isCritical = isCritical,
            timestamp = System.currentTimeMillis(),
            name = char.name,
            characterId = char.id,
            details = details,
            diceResults = listOf(die)
        )
    }
}
