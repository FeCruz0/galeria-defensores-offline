package com.galeria.defensores.data

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LocalFileManager {
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun getFilesDir(context: Context): File {
        return context.filesDir
    }

    suspend inline fun <reified T> saveJson(context: Context, fileName: String, data: T) {
        val jsonString = try { json.encodeToString(data) } catch (e: Exception) { null }
        withContext(Dispatchers.IO) {
            try {
                if (jsonString != null) {
                    val file = File(getFilesDir(context), fileName)
                    file.writeText(jsonString)
                    android.util.Log.d("LocalFileManager", "Saved $fileName successfully.")
                }
            } catch (e: Exception) {
                android.util.Log.e("LocalFileManager", "Error saving $fileName", e)
                e.printStackTrace()
            }
        }
    }

    suspend inline fun <reified T> readJson(context: Context, fileName: String): T? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(getFilesDir(context), fileName)
                if (file.exists()) {
                    val jsonString = file.readText()
                    json.decodeFromString<T>(jsonString)
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("LocalFileManager", "Error reading $fileName", e)
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun deleteFile(context: Context, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(getFilesDir(context), fileName)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun listFiles(context: Context, prefix: String): List<File> {
        return withContext(Dispatchers.IO) {
            try {
                val dir = getFilesDir(context)
                dir.listFiles { _, name -> name.startsWith(prefix) && name.endsWith(".json") }?.toList() ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
