import org.junit.Test
import kotlin.test.assertEquals

class CharacterTest {
    @Test
    fun summary_includes_fields() {
        val c = Character(
            name = "Arin",
            age = 25,
            race = "Humano",
            profession = "Guerreiro",
            attributes = mapOf("FOR" to 10),
            skills = listOf("Ataque"),
            equipment = listOf("Espada")
        )
        assertEquals("Arin, the 25-year-old Humano Guerreiro", c.getCharacterSummary())
    }
}