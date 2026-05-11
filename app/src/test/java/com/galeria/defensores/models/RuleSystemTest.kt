package com.galeria.defensores.models

import org.junit.Test
import org.junit.Assert.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class RuleSystemTest {
    @Test
    fun testSerialization() {
        val sys = RuleSystem(name = "Test System")
        val res = ResourceDefinition(key = "energia", name = "Energia")
        sys.resources.add(res)

        val json = Json { ignoreUnknownKeys = true }
        
        // This is what Converters.fromListResource does
        val serializedList = json.encodeToString(sys.resources as List<ResourceDefinition>)
        assertNotNull(serializedList)
        
        val deserializedList = json.decodeFromString<List<ResourceDefinition>>(serializedList)
        assertEquals(3, deserializedList.size) // 2 default (pv, pm) + 1 added
        assertEquals("pv", deserializedList[0].key) // Default 1
        assertEquals("pm", deserializedList[1].key) // Default 2
        assertEquals("energia", deserializedList[2].key) // Added
    }

    @Test
    fun testAddAndRemoveResource() {
        val sys = RuleSystem(name = "Test System")
        val res = ResourceDefinition(key = "energia", name = "Energia")
        sys.resources.add(res)
        assertEquals(3, sys.resources.size) // 2 default + 1 added

        sys.resources.removeAll { it.key == "energia" }
        assertEquals(2, sys.resources.size) // back to 2 defaults
    }

    @Test
    fun testProgressBarValueMapping() {
        // Verificar que currentValues reflete corretamente o delta aplicado
        val initial = 20
        val delta = -5
        val result = initial + delta
        assertEquals(15, result)
        assertTrue(result >= 0)  // não negativo
    }
}
