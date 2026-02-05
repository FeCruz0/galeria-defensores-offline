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

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }
    
    // Safety check helper
    private fun getContext(): android.content.Context? {
        return if (::appContext.isInitialized) appContext else null
    }

    suspend fun getCharacters(tableId: String? = null): List<Character> {
        val context = getContext() ?: return emptyList()
        val allFiles = LocalFileManager.listFiles(context, PREFIX)
        val characters = mutableListOf<Character>()
        
        for (file in allFiles) {
            val char = LocalFileManager.readJson(context, file.name, Character::class.java)
            if (char != null) {
                if (tableId == null || char.tableId == tableId) {
                    characters.add(char)
                }
            }
        }
        return characters
    }

    suspend fun getCharactersForUser(userId: String): List<Character> {
        // In offline mode, current user owns everything basically, 
        // or we filter by the mock ID "offline_user_id". 
        // Let's just return all characters matching the ownerId 
        // (which should be "offline_user_id" for new ones).
        
        val context = getContext() ?: return emptyList()
        val allFiles = LocalFileManager.listFiles(context, PREFIX)
        val characters = mutableListOf<Character>()
        
        for (file in allFiles) {
            val char = LocalFileManager.readJson(context, file.name, Character::class.java)
            if (char != null && char.ownerId == userId) {
                characters.add(char)
            }
        }
        return characters
    }

    suspend fun getCharacter(id: String): Character? {
        val context = getContext() ?: return null
        return LocalFileManager.readJson(context, "$PREFIX$id.json", Character::class.java)
    }

    suspend fun saveCharacter(character: Character) {
        val context = getContext() ?: return
        LocalFileManager.saveJson(context, "$PREFIX${character.id}.json", character)
    }

    suspend fun deleteCharacter(id: String): Boolean {
        val context = getContext() ?: return false
        return LocalFileManager.deleteFile(context, "$PREFIX$id.json")
    }

    suspend fun unlinkCharactersFromTable(tableId: String) {
        val characters = getCharacters(tableId)
        for (char in characters) {
            val updatedChar = char.copy(tableId = "")
            saveCharacter(updatedChar)
        }
    }
}
