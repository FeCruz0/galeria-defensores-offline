package com.galeria.defensores.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.galeria.defensores.data.RuleSystemRepository
import com.galeria.defensores.data.SharedCharacterState
import com.galeria.defensores.domain.usecases.GetResourceMaxUseCase
import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.models.AttributeDefinition
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.CustomRoll
import com.galeria.defensores.models.InventoryItem
import com.galeria.defensores.models.ResourceDefinition
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.Spell
import com.galeria.defensores.models.UniqueAdvantage
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemManagementViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val ruleSystemRepository = mockk<RuleSystemRepository>(relaxed = true)
    private val sharedCharacterState = mockk<SharedCharacterState>(relaxed = true)
    private val getResourceMaxUseCase = mockk<GetResourceMaxUseCase>(relaxed = true)

    private lateinit var viewModel: SystemManagementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock getSystems flow to avoid lazy stateIn blocking/empty issues
        every { ruleSystemRepository.getSystems() } returns flowOf(emptyList())
        
        viewModel = SystemManagementViewModel(
            ruleSystemRepository,
            sharedCharacterState,
            getResourceMaxUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadSystem loads system copy, resets sandbox character, and initializes attributes`() = runTest(testDispatcher) {
        val attribute = AttributeDefinition(id = "1", key = "forca", name = "Força", abbreviation = "F")
        val resource = ResourceDefinition(id = "2", key = "pv", name = "Pontos de Vida", formula = "R * 5")
        val system = RuleSystem(
            id = "sys_1",
            name = "Test System",
            isBaseSystem = true,
            attributes = mutableListOf(attribute),
            resources = mutableListOf(resource)
        )

        every { getResourceMaxUseCase.invoke(resource, any(), any()) } returns 20

        viewModel.loadSystem(system)

        val loaded = viewModel.selectedSystem.value
        assertNotNull(loaded)
        assertEquals("Test System", loaded?.name)
        assertEquals(system.id, loaded?.id)
        assertFalse(viewModel.isDirty.value)

        val sandboxChar = viewModel.sandboxCharacter.value
        assertNotNull(sandboxChar)
        assertEquals("Personagem de Teste", sandboxChar?.name)
        assertEquals(0, sandboxChar?.attributeValues?.get("forca"))
        assertEquals(0, sandboxChar?.forca)
        assertEquals(20, sandboxChar?.resourceValues?.get("pv"))
        assertEquals(20, sandboxChar?.currentPv)

        verify { sharedCharacterState.update(any()) }
    }

    @Test
    fun `updateMetadata marks system dirty when name or description changes`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "sys_1", name = "Original Name", description = "Original Desc", isBaseSystem = false)
        viewModel.loadSystem(system)

        assertFalse(viewModel.isDirty.value)

        viewModel.updateMetadata("New Name", "Original Desc")
        assertTrue(viewModel.isDirty.value)
        assertEquals("New Name", viewModel.selectedSystem.value?.name)
    }

    @Test
    fun `addAttributeDefinition adds definition and updates sandbox character`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "sys_1", name = "System", isBaseSystem = false)
        viewModel.loadSystem(system)

        val newAttr = AttributeDefinition(id = "attr_new", key = "inteligencia", name = "Inteligência", abbreviation = "I")
        viewModel.addAttributeDefinition(newAttr)

        val updatedSystem = viewModel.selectedSystem.value
        assertTrue(updatedSystem!!.attributes.contains(newAttr))
        assertTrue(viewModel.isDirty.value)

        val sandboxChar = viewModel.sandboxCharacter.value
        assertTrue(sandboxChar!!.attributeValues.containsKey("inteligencia"))
        assertEquals(0, sandboxChar.attributeValues["inteligencia"])
    }

    @Test
    fun `updateAttributeDefinition updates definition, updates character keys, and migrates formulas`() = runTest(testDispatcher) {
        val attr = AttributeDefinition(id = "attr_id", key = "forca", name = "Força", abbreviation = "F")
        val resource = ResourceDefinition(id = "res_id", key = "pv", name = "PV", formula = "forca * 5")
        val system = RuleSystem(
            id = "sys_1",
            name = "System",
            isBaseSystem = false,
            attributes = mutableListOf(attr),
            resources = mutableListOf(resource)
        )
        viewModel.loadSystem(system)

        // Set initial sandbox value
        viewModel.updateAttribute("forca", 3)
        assertEquals(3, viewModel.sandboxCharacter.value?.forca)

        val updatedAttr = AttributeDefinition(id = "attr_id", key = "poder", name = "Poder", abbreviation = "P")
        viewModel.updateAttributeDefinition(updatedAttr)

        val updatedSystem = viewModel.selectedSystem.value
        assertEquals("poder", updatedSystem?.attributes?.first()?.key)
        assertEquals("poder * 5", updatedSystem?.resources?.first()?.formula)

        val sandboxChar = viewModel.sandboxCharacter.value
        assertFalse(sandboxChar!!.attributeValues.containsKey("forca"))
        assertEquals(3, sandboxChar.attributeValues["poder"])
        assertTrue(viewModel.isDirty.value)
    }

    @Test
    fun `removeAttributeDefinition removes definition and updates sandbox character`() = runTest(testDispatcher) {
        val attr = AttributeDefinition(id = "attr_id", key = "forca", name = "Força", abbreviation = "F")
        val system = RuleSystem(
            id = "sys_1",
            name = "System",
            isBaseSystem = false,
            attributes = mutableListOf(attr)
        )
        viewModel.loadSystem(system)

        viewModel.removeAttributeDefinition(attr)

        val updatedSystem = viewModel.selectedSystem.value
        assertTrue(updatedSystem!!.attributes.isEmpty())

        val sandboxChar = viewModel.sandboxCharacter.value
        assertFalse(sandboxChar!!.attributeValues.containsKey("forca"))
    }

    @Test
    fun `addResourceDefinition and removeResourceDefinition work correctly`() = runTest(testDispatcher) {
        // Use a system with empty resource list to avoid default resources (pv + pm)
        val system = RuleSystem(
            id = "sys_1",
            name = "System",
            isBaseSystem = false,
            attributes = mutableListOf(),
            resources = mutableListOf()
        )
        viewModel.loadSystem(system)

        val res = ResourceDefinition(id = "res_id", key = "pm", name = "PM", formula = "10")
        viewModel.addResourceDefinition(res)

        assertTrue(viewModel.selectedSystem.value!!.resources.contains(res))

        viewModel.removeResourceDefinition(res)
        assertTrue(viewModel.selectedSystem.value!!.resources.isEmpty())
    }

    @Test
    fun `importSystemJson loads valid system JSON, sets isDirty to true, and resets sandbox`() = runTest(testDispatcher) {
        // abbreviation is a required field in AttributeDefinition (no default)
        val json = """
            {
                "id": "imported_id",
                "name": "Imported System",
                "description": "Desc",
                "attributes": [{"id": "a1", "key": "f", "name": "Força", "abbreviation": "F"}],
                "resources": [{"id": "r1", "key": "pv", "name": "PV", "formula": "10"}]
            }
        """.trimIndent()

        var callbackSuccess = false
        var callbackSystem: RuleSystem? = null

        viewModel.importSystemJson(json) { success, sys ->
            callbackSuccess = success
            callbackSystem = sys
        }

        assertTrue(callbackSuccess)
        assertNotNull(callbackSystem)
        assertEquals("Imported System", callbackSystem?.name)
        assertFalse(callbackSystem!!.isBaseSystem)
        assertTrue(viewModel.isDirty.value)

        val sandboxChar = viewModel.sandboxCharacter.value
        assertNotNull(sandboxChar)
        assertEquals("Personagem de Teste", sandboxChar?.name)
        assertTrue(sandboxChar!!.attributeValues.containsKey("f"))
    }

    @Test
    fun `importSystemJson returns false for invalid or empty systems`() = runTest(testDispatcher) {
        // RuleSystem() defaults include defaultAttributes() + defaultResources(), so "{}" would succeed.
        // Must explicitly pass empty arrays to trigger the isEmpty guard.
        val json = """
            {
                "id": "empty_id",
                "name": "Empty",
                "attributes": [],
                "resources": []
            }
        """.trimIndent()
        var callbackSuccess = true
        var callbackSystem: RuleSystem? = RuleSystem()

        viewModel.importSystemJson(json) { success, sys ->
            callbackSuccess = success
            callbackSystem = sys
        }

        assertFalse(callbackSuccess)
        assertNull(callbackSystem)
    }

    @Test
    fun `saveSystem on base system without new name fails`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "base_1", name = "Base System", isBaseSystem = true)
        viewModel.loadSystem(system)

        var callbackSuccess = true
        var errorMsg: String? = null

        viewModel.saveSystem { success, err ->
            callbackSuccess = success
            errorMsg = err
        }

        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertNotNull(errorMsg)
        coVerify(exactly = 0) { ruleSystemRepository.saveSystem(any()) }
    }

    @Test
    fun `saveSystem with new name on base system saves new custom system copy`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "base_1", name = "Base System", isBaseSystem = true)
        viewModel.loadSystem(system)

        coEvery { ruleSystemRepository.saveSystem(any()) } just Runs

        var callbackSuccess = false
        var errorMsg: String? = null

        viewModel.saveSystem(newName = "My Custom System") { success, err ->
            callbackSuccess = success
            errorMsg = err
        }

        advanceUntilIdle()

        assertTrue(callbackSuccess)
        assertNull(errorMsg)

        val savedSystemSlot = slot<RuleSystem>()
        coVerify(exactly = 1) { ruleSystemRepository.saveSystem(capture(savedSystemSlot)) }

        val saved = savedSystemSlot.captured
        assertEquals("My Custom System", saved.name)
        assertFalse(saved.isBaseSystem)
        assertNotEquals("base_1", saved.id)
    }

    @Test
    fun `revertSystem restores loaded system state`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "sys_1", name = "Original System", isBaseSystem = false)
        viewModel.loadSystem(system)

        viewModel.updateMetadata("Modified System", "Desc")
        assertEquals("Modified System", viewModel.selectedSystem.value?.name)
        assertTrue(viewModel.isDirty.value)

        viewModel.revertSystem()
        assertEquals("Original System", viewModel.selectedSystem.value?.name)
        assertFalse(viewModel.isDirty.value)
    }

    @Test
    fun `deleteSystem deletes custom system and loads default`() = runTest(testDispatcher) {
        val customSystem = RuleSystem(id = "custom_1", name = "Custom", isBaseSystem = false)
        val defaultSystem = RuleSystem(id = "base_default", name = "Base Default", isBaseSystem = true)

        viewModel.loadSystem(customSystem)

        coEvery { ruleSystemRepository.deleteSystem("custom_1") } returns true
        coEvery { ruleSystemRepository.getSystemOrDefault(null) } returns defaultSystem

        var callbackSuccess = false
        var errorMsg: String? = null

        viewModel.deleteSystem { success, err ->
            callbackSuccess = success
            errorMsg = err
        }

        advanceUntilIdle()

        assertTrue(callbackSuccess)
        assertNull(errorMsg)
        coVerify(exactly = 1) { ruleSystemRepository.deleteSystem("custom_1") }
        assertEquals("Base Default", viewModel.selectedSystem.value?.name)
    }

    @Test
    fun `deleteSystem on base system fails`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "base_1", name = "Base System", isBaseSystem = true)
        viewModel.loadSystem(system)

        var callbackSuccess = true
        var errorMsg: String? = null

        viewModel.deleteSystem { success, err ->
            callbackSuccess = success
            errorMsg = err
        }

        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertNotNull(errorMsg)
        coVerify(exactly = 0) { ruleSystemRepository.deleteSystem(any()) }
    }

    @Test
    fun `sandbox character modifications work correctly`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "sys_1", name = "System", isBaseSystem = false)
        viewModel.loadSystem(system)

        viewModel.updateName("Hero")
        viewModel.updateScale(2)
        viewModel.updateExperience(15) // should gain 1 point, 5 remains
        viewModel.updateNotes("Some notes")
        viewModel.updateDamageType("Fogo", false)
        viewModel.updateDamageType("Eletricidade", true)

        val sandboxChar = viewModel.sandboxCharacter.value
        assertNotNull(sandboxChar)
        assertEquals("Hero", sandboxChar?.name)
        assertEquals(2, sandboxChar?.scale)
        assertEquals(5, sandboxChar?.experience)
        assertEquals(1, sandboxChar?.savedPoints)
        assertEquals("Some notes", sandboxChar?.anotacoes)
        assertEquals("Fogo", sandboxChar?.damageTypeForca)
        assertEquals("Eletricidade", sandboxChar?.damageTypePdf)
    }

    @Test
    fun `sandbox character advantage and disadvantage operations`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "sys_1", name = "System", isBaseSystem = false)
        viewModel.loadSystem(system)

        val adv = AdvantageItem(id = "adv_1", name = "Vantagem", description = "Desc", cost = "2")
        viewModel.addAdvantage(adv)
        assertEquals(1, viewModel.sandboxCharacter.value?.vantagens?.size)

        // addAdvantage generates a new random UUID internally; fetch the actual stored ID
        val storedId = viewModel.sandboxCharacter.value!!.vantagens.first().id
        val updatedAdv = AdvantageItem(id = storedId, name = "Vantagem Melhorada", description = "Desc", cost = "2")
        viewModel.updateAdvantage(updatedAdv)
        assertEquals("Vantagem Melhorada", viewModel.sandboxCharacter.value?.vantagens?.first()?.name)

        viewModel.removeAdvantage(updatedAdv)
        assertTrue(viewModel.sandboxCharacter.value!!.vantagens.isEmpty())
    }

    @Test
    fun `custom unique advantages operations work in memory`() = runTest(testDispatcher) {
        val system = RuleSystem(id = "sys_1", name = "System", isBaseSystem = false)
        viewModel.loadSystem(system)

        val ua = UniqueAdvantage("Custom Elf", "Semi-Humanos", 2, "Benefits", "Weaknesses")
        viewModel.addCustomUniqueAdvantage(ua)
        assertEquals(1, viewModel.customUniqueAdvantages.value.size)
        assertEquals("Custom Elf", viewModel.customUniqueAdvantages.value.first().name)

        val updatedUa = UniqueAdvantage("Custom Elf", "Semi-Humanos", 3, "New Benefits", "Weaknesses")
        viewModel.updateCustomUniqueAdvantage(ua, updatedUa)
        assertEquals(3, viewModel.customUniqueAdvantages.value.first().cost)
        assertEquals("New Benefits", viewModel.customUniqueAdvantages.value.first().benefits)

        viewModel.removeCustomUniqueAdvantage(updatedUa)
        assertTrue(viewModel.customUniqueAdvantages.value.isEmpty())
    }

    @Test
    fun `addAttributeDefinition with duplicate key or name throws IllegalArgumentException`() = runTest(testDispatcher) {
        val attr = AttributeDefinition(id = "attr_id", key = "forca", name = "Força", abbreviation = "F")
        val system = RuleSystem(
            id = "sys_1",
            name = "System",
            isBaseSystem = false,
            attributes = mutableListOf(attr),
            resources = mutableListOf()
        )
        viewModel.loadSystem(system)

        // Duplicate key (case-insensitive)
        val duplicateKeyAttr = AttributeDefinition(id = "new_attr", key = "FORCA", name = "Nova Força", abbreviation = "NF")
        try {
            viewModel.addAttributeDefinition(duplicateKeyAttr)
            fail("Expected IllegalArgumentException for duplicate key")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // Duplicate name (case-insensitive)
        val duplicateNameAttr = AttributeDefinition(id = "new_attr", key = "habilidade", name = "força", abbreviation = "NF")
        try {
            viewModel.addAttributeDefinition(duplicateNameAttr)
            fail("Expected IllegalArgumentException for duplicate name")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `addResourceDefinition with duplicate key or name throws IllegalArgumentException`() = runTest(testDispatcher) {
        val res = ResourceDefinition(id = "res_id", key = "pv", name = "Pontos de Vida", formula = "10")
        val system = RuleSystem(
            id = "sys_1",
            name = "System",
            isBaseSystem = false,
            attributes = mutableListOf(),
            resources = mutableListOf(res)
        )
        viewModel.loadSystem(system)

        // Duplicate key with existing resource (case-insensitive)
        val duplicateKeyRes = ResourceDefinition(id = "new_res", key = "PV", name = "Novo PV", formula = "10")
        try {
            viewModel.addResourceDefinition(duplicateKeyRes)
            fail("Expected IllegalArgumentException for duplicate key")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // Duplicate name with existing resource (case-insensitive)
        val duplicateNameRes = ResourceDefinition(id = "new_res", key = "pm", name = "pontos de vida", formula = "10")
        try {
            viewModel.addResourceDefinition(duplicateNameRes)
            fail("Expected IllegalArgumentException for duplicate name")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
