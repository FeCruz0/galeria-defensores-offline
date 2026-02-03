package com.galeria.defensores.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LocalFileManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private fun getFilesDir(context: Context): File {
        return context.filesDir
    }

    suspend fun <T> saveJson(context: Context, fileName: String, data: T) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(getFilesDir(context), fileName)
                val jsonString = gson.toJson(data)
                file.writeText(jsonString)
                android.util.Log.d("LocalFileManager", "Saved $fileName successfully.")
            } catch (e: Exception) {
                android.util.Log.e("LocalFileManager", "Error saving $fileName", e)
                e.printStackTrace()
            }
        }
    }

    suspend fun <T> readJson(context: Context, fileName: String, classOfT: Class<T>): T? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(getFilesDir(context), fileName)
                if (file.exists()) {
                    val jsonString = file.readText()
                    gson.fromJson(jsonString, classOfT)
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
