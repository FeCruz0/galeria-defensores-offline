package com.galeria.defensores.data

import android.content.Context
import android.net.Uri
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.Table
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
@Singleton
class BackupRepository @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val tableRepository: TableRepository
) {

    suspend fun exportCharacter(context: Context, charId: String, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val charJson = characterRepository.getCharacter(charId).first()
                    ?: return@withContext false
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(Json { prettyPrint = true }.encodeToString(charJson).toByteArray())
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
                val table = tableRepository.getTableOnce(tableId) ?: return@withContext false
                val characters = characterRepository.getCharacters(tableId).first()
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                        val tableEntry = ZipEntry("table.json")
                        zipOut.putNextEntry(tableEntry)
                        val tableJson = Json { prettyPrint = true }.encodeToString(table)
                        zipOut.write(tableJson.toByteArray())
                        zipOut.closeEntry()

                        for (char in characters) {
                            val charEntry = ZipEntry("characters/char_${char.id}.json")
                            zipOut.putNextEntry(charEntry)
                            val charJson = Json { prettyPrint = true }.encodeToString(char)
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
                
                val char = try { Json { ignoreUnknownKeys = true }.decodeFromString<Character>(jsonString) } catch (e: Exception) { null } ?: return@withContext false
                
                val newChar = char.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    tableId = tableId
                )

                characterRepository.saveCharacter(newChar)
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
                        val os = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (zipIn.read(buffer).also { count = it } != -1) {
                            os.write(buffer, 0, count)
                        }
                        val json = os.toString("UTF-8")
                        table = try { Json { ignoreUnknownKeys = true }.decodeFromString<Table>(json) } catch (e: Exception) { null }
                    } else if (entry.name.startsWith("characters/") && entry.name.endsWith(".json")) {
                        val os = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (zipIn.read(buffer).also { count = it } != -1) {
                            os.write(buffer, 0, count)
                        }
                        val json = os.toString("UTF-8")
                        val char = try { Json { ignoreUnknownKeys = true }.decodeFromString<Character>(json) } catch (e: Exception) { null }
                        if (char != null) characters.add(char)
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                
                if (table != null) {
                    val newTableId = java.util.UUID.randomUUID().toString()
                    val newTable = table.copy(
                        id = newTableId,
                        name = "${table.name} (Import)",
                        masterId = SessionManager.currentUser?.id ?: "offline-master"
                    )
                    
                    characters.forEach { char ->
                        val newChar = char.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            tableId = newTableId
                        )
                        characterRepository.saveCharacter(newChar)
                    }
                    
                    tableRepository.addTable(newTable)
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
