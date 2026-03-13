package com.galeria.defensores.domain.usecases

import com.galeria.defensores.models.Character
import com.galeria.defensores.models.CustomRoll
import com.galeria.defensores.models.RollResult
import kotlin.random.Random

/**
 * Use Case to calculate complex custom rolls with multiple components, multipliers, and special rules.
 */
class CalculateCustomRollUseCase @javax.inject.Inject constructor() {

    operator fun invoke(
        char: Character, 
        roll: CustomRoll, 
        diceOverride: List<Int>? = null
    ): RollResult {
        var totalSum = 0
        val parts = mutableListOf<String>()
        var isCriticalSummary = false
        var totalCrits = 0
        val allDice = mutableListOf<Int>()
        
        val combatNameCheck = roll.name.contains("Ataque", ignoreCase = true) || 
                             roll.name.contains("Defesa", ignoreCase = true) || 
                             roll.name.contains("PdF", ignoreCase = true) ||
                             roll.name.contains(" F ", ignoreCase = true)

        var overrideIndex = 0

        // 1. Process Dice Components
        roll.components.forEach { comp ->
            var compTotal = 0
            val rolls = mutableListOf<Int>()
            
            repeat(comp.count) {
                val die = if (diceOverride != null && overrideIndex < diceOverride.size) {
                    diceOverride[overrideIndex++]
                } else {
                    Random.nextInt(comp.faces) + 1
                }
                
                val currentCanCrit = comp.canCrit || combatNameCheck
                val isCrit = currentCanCrit && (
                    (comp.critRangeStart != null && die >= comp.critRangeStart!!) || 
                    (comp.critRangeStart == null && die == comp.faces)
                )
                
                if (isCrit) {
                    totalCrits++
                    isCriticalSummary = true
                }
                
                rolls.add(die)
                allDice.add(die)
                compTotal += die
            }
            
            val finalCompTotal = if (comp.isNegative) -compTotal else compTotal
            totalSum += finalCompTotal + comp.bonus
            
            // Format details string for the UI
            val signPrefix = if (comp.isNegative) "- " else (if (parts.isNotEmpty()) "+ " else "")
            
            val formattedRolls = rolls.map { d ->
                val isMax = (d == comp.faces && comp.canCrit)
                if (isMax) "$d!" else "$d"
            }.joinToString(",")
            
            val diceStr = "${comp.count}d${comp.faces} [$formattedRolls]"
            val bonusStr = if (comp.bonus != 0) {
                 " + ${comp.bonus}"
            } else ""
            
            parts.add("$signPrefix$diceStr$bonusStr")
        }

        // 2. Resolve Attributes & Crits
        fun getAttrValue(attrName: String): Int {
            return when(attrName) {
                "forca" -> char.forca
                "habilidade" -> char.habilidade
                "resistencia" -> char.resistencia
                "armadura" -> char.armadura
                "poderFogo" -> char.poderFogo
                else -> 0
            }
        }
        
        val primaryVal = getAttrValue(roll.primaryAttribute)
        val secondaryVal = getAttrValue(roll.secondaryAttribute)
        
        // Calculate Multiplier
        var critMultiplier = 1
        if (isCriticalSummary) {
             critMultiplier = if (roll.accumulateCrit) 1 + totalCrits else 2
        }
        
        val finalPrimary = primaryVal * critMultiplier
        totalSum += finalPrimary + secondaryVal + roll.globalModifier
 
        // 3. Append Attributes formatted
        if (roll.primaryAttribute != "none" && primaryVal != 0) {
            val pName = roll.primaryAttribute.replaceFirstChar { it.uppercase() }.take(3)
            val critInfo = if (critMultiplier > 1) " x$critMultiplier!" else ""
            val prefix = if (parts.isNotEmpty()) "+ " else ""
            parts.add("$prefix$pName [$primaryVal$critInfo]")
        }
        
        if (roll.secondaryAttribute != "none" && secondaryVal != 0) {
            val sName = roll.secondaryAttribute.replaceFirstChar { it.uppercase() }.take(3)
            val prefix = if (parts.isNotEmpty()) "+ " else ""
            parts.add("$prefix$sName [$secondaryVal]")
        }
        
        // 4. Global Modifier
        if (roll.globalModifier != 0) {
             val prefix = if (roll.globalModifier > 0) (if (parts.isNotEmpty()) "+ " else "") else "- "
             parts.add("$prefix[${kotlin.math.abs(roll.globalModifier)}]")
        }
        
        // 5. Build Final String
        val finalString = "${parts.joinToString(" ").replace("  ", " ").trim()} = $totalSum"

        return RollResult(
            total = totalSum,
            die = 0, 
            attributeUsed = "Custom",
            attributeValue = 0,
            skillValue = 0,
            bonus = roll.globalModifier,
            isCritical = isCriticalSummary,
            timestamp = System.currentTimeMillis(),
            name = "${char.name} - ${roll.name}",
            characterId = char.id,
            details = finalString,
            diceResults = allDice
        )
    }
}
