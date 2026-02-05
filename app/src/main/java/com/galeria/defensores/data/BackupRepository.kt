package com.galeria.defensores.data

import android.content.Context
import android.net.Uri
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.Table
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupRepository {

    suspend fun exportCharacter(context: Context, charId: String, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val charJson = LocalFileManager.readJson(context, "char_$charId.json", Character::class.java)
                    ?: return@withContext false
                
                // Write to Uri
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(charJson).toByteArray())
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun exportTable(context: Context, tableId: String, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val table = TableRepository.getTable(tableId) ?: return@withContext false
                val characters = CharacterRepository.getCharacters(tableId)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                        // Add Table JSON
                        val tableEntry = ZipEntry("table.json")
                        zipOut.putNextEntry(tableEntry)
                        val tableJson = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(table)
                        zipOut.write(tableJson.toByteArray())
                        zipOut.closeEntry()

                        // Add Characters folder
                        for (char in characters) {
                            val charEntry = ZipEntry("characters/char_${char.id}.json")
                            zipOut.putNextEntry(charEntry)
                            val charJson = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(char)
                            zipOut.write(charJson.toByteArray())
                            zipOut.closeEntry()
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun exportAll(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val filesDir = context.filesDir
                val files = filesDir.listFiles() ?: return@withContext false

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                        for (file in files) {
                            // Only include our known prefixes or expected files
                            // Prefixes: char_, table_, system_
                            if (file.name.endsWith(".json") && 
                                (file.name.startsWith("char_") || 
                                 file.name.startsWith("table_") || 
                                 file.name.startsWith("system_"))) {
                                
                                val entry = ZipEntry(file.name)
                                zipOut.putNextEntry(entry)
                                FileInputStream(file).use { fis ->
                                    fis.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                            }
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }


    suspend fun importCharacter(context: Context, uri: Uri, tableId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                
                val char = com.google.gson.GsonBuilder().create().fromJson(jsonString, Character::class.java) ?: return@withContext false
                
                // Generate new ID and assign to current table
                val newChar = char.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    tableId = tableId,
                    name = "${char.name} (Import)" // Avoid exact name collision confusion? or keep orig? Let's keep orig but ID diff.
                    // Actually, keeping original name is better for backup restore.
                    // But if it's a template? Let's keep name.
                )
                // Just in case, if name collision is an issue UI-wise, the ID is unique.

                CharacterRepository.saveCharacter(newChar)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importTable(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
                val zipIn = java.util.zip.ZipInputStream(java.io.BufferedInputStream(inputStream))
                
                var entry = zipIn.nextEntry
                var table: Table? = null
                val characters = mutableListOf<Character>()
                
                while (entry != null) {
                    if (entry.name == "table.json") {

                        // Safe way for ZipInputStream:
                        // Read bytes
                        val os = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (zipIn.read(buffer).also { count = it } != -1) {
                            os.write(buffer, 0, count)
                        }
                        val json = os.toString("UTF-8")
                        table = com.google.gson.GsonBuilder().create().fromJson(json, Table::class.java)
                    } else if (entry.name.startsWith("characters/") && entry.name.endsWith(".json")) {
                         val os = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (zipIn.read(buffer).also { count = it } != -1) {
                            os.write(buffer, 0, count)
                        }
                        val json = os.toString("UTF-8")
                        val char = com.google.gson.GsonBuilder().create().fromJson(json, Character::class.java)
                        if (char != null) characters.add(char)
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                
                if (table != null) {
                    // New Table ID
                    val newTableId = java.util.UUID.randomUUID().toString()
                    val newTable = table.copy(
                        id = newTableId,
                        name = "${table.name} (Import)",
                        masterId = SessionManager.currentUser?.id ?: "offline-master" // Assign to current user
                    )
                    
                    // Save Characters with new Table ID
                    characters.forEach { char ->
                        val newChar = char.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            tableId = newTableId
                        )
                        CharacterRepository.saveCharacter(newChar)
                    }
                    
                    TableRepository.addTable(newTable) // Use addTable or saveTable depending on API
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importAll(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
                val zipIn = java.util.zip.ZipInputStream(java.io.BufferedInputStream(inputStream))
                
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val fileName = entry.name
                    // Security / Validity check
                    // We expect files at root level of zip with specific prefixes
                    if (!fileName.contains("..") && 
                        fileName.endsWith(".json") && 
                        (fileName.startsWith("char_") || 
                         fileName.startsWith("table_") || 
                         fileName.startsWith("system_"))) {
                        
                        val outFile = File(context.filesDir, fileName)
                        val fos = FileOutputStream(outFile)
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (zipIn.read(buffer).also { count = it } != -1) {
                            fos.write(buffer, 0, count)
                        }
                        fos.close()
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
