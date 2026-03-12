package com.galeria.defensores.domain.usecases

import com.galeria.defensores.models.Character
import com.galeria.defensores.models.ResourceDefinition
import com.galeria.defensores.models.RuleSystem

/**
 * Use Case to calculate the maximum value of a character resource (PV, PM, etc.)
 * based on the formula defined in the RuleSystem.
 */
class GetResourceMaxUseCase {
    
    operator fun invoke(res: ResourceDefinition, char: Character, ruleSystem: RuleSystem): Int {
        val formula = res.formula.trim().uppercase()
        if (formula.isEmpty()) return 10 // Default fallback

        // Replace known attributes with values
        var expression = formula
            .replace("PDF", char.poderFogo.toString())
            .replace("F", char.forca.toString())
            .replace("H", char.habilidade.toString())
            .replace("R", char.resistencia.toString())
            .replace("A", char.armadura.toString())

        // Dynamic Attributes from RuleSystem
        ruleSystem.attributes.forEach { attr ->
            val key = attr.key.uppercase()
            if (expression.contains(key)) {
                val valAttr = char.attributeValues[attr.key] ?: 0
                expression = expression.replace(key, valAttr.toString())
            }
        }
        
        // Replace X operator (common in 3DeT notation)
        expression = expression.replace("X", "*")
        
        var result = 0
        try {
            // Evaluate simple expression (very basic: A op B)
            if (expression.contains("*")) {
                val parts = expression.split("*")
                if (parts.size >= 2) {
                    val a = parts[0].trim().toIntOrNull() ?: 0
                    val b = parts[1].trim().toIntOrNull() ?: 0
                    result = a * b
                }
            } else if (expression.contains("+")) {
                val parts = expression.split("+")
                if (parts.size >= 2) {
                    val a = parts[0].trim().toIntOrNull() ?: 0
                    val b = parts[1].trim().toIntOrNull() ?: 0
                    result = a + b
                }
            } else if (expression.contains("-")) {
                 val parts = expression.split("-")
                 if (parts.size >= 2) {
                     val a = parts[0].trim().toIntOrNull() ?: 0
                     val b = parts[1].trim().toIntOrNull() ?: 0
                     result = a - b
                 }
            } else if (expression.contains("/")) {
                  val parts = expression.split("/")
                  if (parts.size >= 2) {
                      val a = parts[0].trim().toIntOrNull() ?: 0
                      val b = parts[1].trim().toIntOrNull() ?: 0
                      if (b != 0) result = a / b
                  }
            } else {
                // Try direct number
                result = expression.trim().toIntOrNull() ?: 0
            }
        } catch (e: Exception) {
            android.util.Log.e("FormulaError", "Failed to parse formula: $formula", e)
        }
        
        // Rule: minimum of 1 for any resource max
        return result.coerceAtLeast(1)
    }
}
