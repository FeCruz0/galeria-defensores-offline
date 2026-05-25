package com.galeria.defensores.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.galeria.defensores.data.RuleSystemRepository
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.data.SharedCharacterState
import com.galeria.defensores.domain.usecases.GetResourceMaxUseCase
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.Table
import com.galeria.defensores.models.AttributeDefinition
import com.galeria.defensores.models.ResourceDefinition
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RuleSystemViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val ruleSystemRepository = mockk<RuleSystemRepository>(relaxed = true)
    private val tableRepository = mockk<TableRepository>(relaxed = true)
    private val sharedCharacterState = mockk<SharedCharacterState>(relaxed = true)
    private val getResourceMaxUseCase = mockk<GetResourceMaxUseCase>(relaxed = true)

    private lateinit var viewModel: RuleSystemViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RuleSystemViewModel(
            ruleSystemRepository,
            tableRepository,
            sharedCharacterState,
            getResourceMaxUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveSystemAs when table id is null saves system and updates live state`() = runTest(testDispatcher) {
        val initialSystem = RuleSystem(id = "base_id", name = "Base System", isBaseSystem = true)
        viewModel.loadRuleSystem(initialSystem)

        // Mock repository saving
        coEvery { ruleSystemRepository.saveSystem(any()) } just Runs

        var callbackSuccess = false
        var callbackSystem: RuleSystem? = null

        viewModel.saveSystemAs("New Custom System") { success, sys ->
            callbackSuccess = success
            callbackSystem = sys
        }

        // Advance dispatcher to execute coroutine in viewModelScope
        advanceUntilIdle()

        // Verify saveSystem was called
        coVerify(exactly = 1) { ruleSystemRepository.saveSystem(any()) }

        // Verify tableRepository.updateTable was not called since currentTableId is null
        coVerify(exactly = 0) { tableRepository.updateTable(any()) }

        // Verify callback results
        assertTrue(callbackSuccess)
        assertNotNull(callbackSystem)
        assertEquals("New Custom System", callbackSystem?.name)
        assertFalse(callbackSystem!!.isBaseSystem)
        assertNotEquals("base_id", callbackSystem!!.id)

        // Verify RuleSystemViewModel updates its active state flow
        assertEquals(callbackSystem, viewModel.ruleSystem.value)
    }

    @Test
    fun `saveSystemAs when table id is present updates table ruleSystemId and saves system`() = runTest(testDispatcher) {
        val initialSystem = RuleSystem(id = "base_id", name = "Base System", isBaseSystem = true)
        viewModel.loadRuleSystem(initialSystem)

        // Load table id
        viewModel.loadDamageTypes("table_123")

        val mockTable = Table(id = "table_123", name = "My Table", ruleSystemId = "base_id")
        coEvery { tableRepository.getTableOnce("table_123") } returns mockTable
        coEvery { tableRepository.updateTable(any()) } returns true
        coEvery { ruleSystemRepository.saveSystem(any()) } just Runs

        var callbackSuccess = false
        var callbackSystem: RuleSystem? = null

        viewModel.saveSystemAs("New Custom System") { success, sys ->
            callbackSuccess = success
            callbackSystem = sys
        }

        advanceUntilIdle()

        // Verify system was saved
        coVerify(exactly = 1) { ruleSystemRepository.saveSystem(any()) }

        // Verify table was retrieved and updated
        coVerify(exactly = 1) { tableRepository.getTableOnce("table_123") }
        val tableSlot = slot<Table>()
        coVerify(exactly = 1) { tableRepository.updateTable(capture(tableSlot)) }

        assertEquals(callbackSystem!!.id, tableSlot.captured.ruleSystemId)

        assertTrue(callbackSuccess)
        assertEquals(callbackSystem, viewModel.ruleSystem.value)
    }

    @Test
    fun `saveSystemAs when exception occurs calls callback with false`() = runTest(testDispatcher) {
        val initialSystem = RuleSystem(id = "base_id", name = "Base System", isBaseSystem = true)
        viewModel.loadRuleSystem(initialSystem)

        coEvery { ruleSystemRepository.saveSystem(any()) } throws RuntimeException("DB Error")

        var callbackSuccess = true
        var callbackSystem: RuleSystem? = RuleSystem()

        viewModel.saveSystemAs("New Custom System") { success, sys ->
            callbackSuccess = success
            callbackSystem = sys
        }

        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertNull(callbackSystem)
    }

    @Test
    fun `importSystemJson when table id is present updates table ruleSystemId and saves system`() = runTest(testDispatcher) {
        val initialSystem = RuleSystem(id = "base_id", name = "Base System", isBaseSystem = true)
        viewModel.loadRuleSystem(initialSystem)

        // Load table id
        viewModel.loadDamageTypes("table_123")

        val mockTable = Table(id = "table_123", name = "My Table", ruleSystemId = "base_id")
        coEvery { tableRepository.getTableOnce("table_123") } returns mockTable
        coEvery { tableRepository.updateTable(any()) } returns true
        coEvery { ruleSystemRepository.saveSystem(any()) } just Runs

        val json = """
            {
                "name": "Imported System",
                "description": "An imported system",
                "attributes": [{"key": "forca", "name": "Força", "abbreviation": "F", "color": "#EF4444", "displayOrder": 0}],
                "resources": [{"key": "pv", "name": "Pontos de Vida", "color": "#EF4444", "formula": "R * 5"}]
            }
        """.trimIndent()

        var callbackSuccess = false
        var callbackSystem: RuleSystem? = null

        viewModel.importSystemJson(json) { success, sys ->
            callbackSuccess = success
            callbackSystem = sys
        }

        advanceUntilIdle()

        // Verify system was saved
        coVerify(exactly = 1) { ruleSystemRepository.saveSystem(any()) }

        // Verify table was retrieved and updated
        coVerify(exactly = 1) { tableRepository.getTableOnce("table_123") }
        val tableSlot = slot<Table>()
        coVerify(exactly = 1) { tableRepository.updateTable(capture(tableSlot)) }

        assertEquals(callbackSystem!!.id, tableSlot.captured.ruleSystemId)

        assertTrue(callbackSuccess)
        assertNotNull(callbackSystem)
        assertEquals("Imported System", callbackSystem?.name)
        assertFalse(callbackSystem!!.isBaseSystem)
        assertNotEquals("base_id", callbackSystem!!.id)
        assertEquals(callbackSystem, viewModel.ruleSystem.value)
    }

    @Test
    fun `importSystemJson when table id is null saves system and updates live state`() = runTest(testDispatcher) {
        val initialSystem = RuleSystem(id = "base_id", name = "Base System", isBaseSystem = true)
        viewModel.loadRuleSystem(initialSystem)

        // Mock repository saving
        coEvery { ruleSystemRepository.saveSystem(any()) } just Runs

        val json = """
            {
                "name": "Imported System",
                "description": "An imported system",
                "attributes": [{"key": "forca", "name": "Força", "abbreviation": "F", "color": "#EF4444", "displayOrder": 0}],
                "resources": [{"key": "pv", "name": "Pontos de Vida", "color": "#EF4444", "formula": "R * 5"}]
            }
        """.trimIndent()

        var callbackSuccess = false
        var callbackSystem: RuleSystem? = null

        viewModel.importSystemJson(json) { success, sys ->
            callbackSuccess = success
            callbackSystem = sys
        }

        advanceUntilIdle()

        // Verify saveSystem was called
        coVerify(exactly = 1) { ruleSystemRepository.saveSystem(any()) }

        // Verify tableRepository.updateTable was not called since currentTableId is null
        coVerify(exactly = 0) { tableRepository.updateTable(any()) }

        // Verify callback results
        assertTrue(callbackSuccess)
        assertNotNull(callbackSystem)
        assertEquals("Imported System", callbackSystem?.name)
        assertFalse(callbackSystem!!.isBaseSystem)
        assertNotEquals("base_id", callbackSystem!!.id)

        // Verify RuleSystemViewModel updates its active state flow
        assertEquals(callbackSystem, viewModel.ruleSystem.value)
    }

    @Test
    fun `importSystemJson with empty attributes and resources returns false`() = runTest(testDispatcher) {
        val initialSystem = RuleSystem(id = "base_id", name = "Base System", isBaseSystem = true)
        viewModel.loadRuleSystem(initialSystem)

        val json = """
            {
                "name": "Empty System",
                "description": "An empty system",
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

        advanceUntilIdle()

        assertFalse(callbackSuccess)
        assertNull(callbackSystem)
    }

    @Test
    fun `addAttributeDefinition with duplicate key or name throws IllegalArgumentException`() = runTest(testDispatcher) {
        val attr = AttributeDefinition(id = "attr_id", key = "forca", name = "Força", abbreviation = "F")
        val initialSystem = RuleSystem(
            id = "sys_1",
            name = "Test System",
            isBaseSystem = false,
            attributes = mutableListOf(attr),
            resources = mutableListOf()
        )
        viewModel.loadRuleSystem(initialSystem)

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
        val initialSystem = RuleSystem(
            id = "sys_1",
            name = "Test System",
            isBaseSystem = false,
            attributes = mutableListOf(),
            resources = mutableListOf(res)
        )
        viewModel.loadRuleSystem(initialSystem)

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
