package com.galeria.defensores.data

import com.galeria.defensores.models.Character




object CharacterRepository {
    private const val PREFIX = "char_"

    // Needs Context to access filesDir. 
    // Ideally we inject Context, but for this refactor we can use the Application Context 
    // if we had access to it, or pass it in. 
    // Since this is an Object, passing Context to every method is the cleanest way without Dagger/Hilt.
    // However, existing calls don't pass Context.
    // We can hold a reference to applicationContext via a simplified init method call from MainActivity 
    // or just assume we modify calls (which is a lot).
    // Let's use a "lateinit var context" initialized in MainActivity/SessionManager.
    
    private lateinit var appContext: android.content.Context
    private lateinit var database: com.galeria.defensores.data.database.AppDatabase

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        database = com.galeria.defensores.data.database.AppDatabase.getDatabase(appContext)
    }
    
    // Safety check helper
    private fun getContext(): android.content.Context? {
        return if (::appContext.isInitialized) appContext else null
    }

    suspend fun getCharacters(tableId: String? = null): List<Character> {
        if (!::database.isInitialized) return emptyList()
        val entities = if (tableId == null) {
            database.characterDao().getAll()
        } else {
            database.characterDao().getByTable(tableId)
        }
        return entities.map { it.toCharacter() }
    }

    suspend fun getCharactersForUser(userId: String): List<Character> {
        if (!::database.isInitialized) return emptyList()
        val entities = database.characterDao().getByOwner(userId)
        return entities.map { it.toCharacter() }
    }

    suspend fun getCharacter(id: String): Character? {
        if (!::database.isInitialized) return null
        return database.characterDao().getById(id)?.toCharacter()
    }

    suspend fun saveCharacter(character: Character) {
        if (!::database.isInitialized) return
        database.characterDao().insert(com.galeria.defensores.data.database.entities.CharacterEntity.fromCharacter(character))
    }

    suspend fun deleteCharacter(id: String): Boolean {
        if (!::database.isInitialized) return false
        database.characterDao().deleteById(id)
        return true
    }

    suspend fun unlinkCharactersFromTable(tableId: String) {
        val characters = getCharacters(tableId)
        for (char in characters) {
            val updatedChar = char.copy(tableId = "")
            saveCharacter(updatedChar)
        }
    }
}
