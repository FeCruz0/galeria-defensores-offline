package com.galeria.defensores

import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.ModifierOption
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterPointsTest {

    @Test
    fun testModularDisadvantageCalculatesCorrectly() {
        val codeOfHonor = AdvantageItem(
            name = "Código de Honra",
            isModular = true,
            baseCostPt = 0,
            modifiers = listOf(
                ModifierOption("h1", "Code 1", -1),
                ModifierOption("h2", "Code 2", -1),
                ModifierOption("h3", "Code 3", -1)
            ),
            selectedModifiers = listOf("h1", "h2")
        )

        // One item with two modifiers of -1 should cost -2
        assertEquals(-2, codeOfHonor.computedCostPt())

        val character = Character(
            forca = 1, // 1pt
            desvantagens = mutableListOf(codeOfHonor)
        )

        // Total score: 1 (F) + (-2) (Dis) = -1
        assertEquals(-1, character.calculateScore())
    }

    @Test
    fun testMultipleDisadvantagesCalculateCorrectly() {
        val dis1 = AdvantageItem(name = "Dis 1", cost = "-1 ponto")
        val dis2 = AdvantageItem(name = "Dis 2", cost = "-2 pontos")
        
        val character = Character(
            forca = 5,
            desvantagens = mutableListOf(dis1, dis2)
        )
        
        // 5 + (-1) + (-2) = 2
        assertEquals(2, character.calculateScore())
    }

    @Test
    fun testLegacyNonModularDisadvantageUpgradeRegex() {
        val legacyCode = AdvantageItem(
            name = "Código de Honra",
            cost = "-1 ponto (cada)",
            isModular = false
        )
        // Should extract -1
        assertEquals(-1, legacyCode.computedCostPt())
    }

    @Test
    fun testAtaqueEspecialMinimumCost() {
        val ae = AdvantageItem(
            name = "Ataque Especial",
            isModular = true,
            baseCostPt = 1,
            modifiers = listOf(
                ModifierOption("ae_lento", "Lento", -1),
                ModifierOption("ae_perto_morte", "Perto Morte", -2)
            ),
            selectedModifiers = listOf("ae_lento", "ae_perto_morte")
        )
        
        // Base cost: 1. Modifiers: -1, -2. Total = -2.
        // But minimum cost should be 1.
        assertEquals(1, ae.computedCostPt())
    }
}
