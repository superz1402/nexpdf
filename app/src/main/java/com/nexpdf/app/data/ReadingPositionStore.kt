package com.nexpdf.app.data

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReadingPositionStore {

    data class Position(val key: String, val title: String, val page: Int, val updatedAt: Long)

    private fun file(context: Context) = File(context.filesDir, "reading_positions.json")

    private fun key(uri: String): String = hash(uri)

    private fun hash(s: String): String = java.security.MessageDigest.getInstance("MD5")
        .digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    suspend fun load(context: Context, uri: String, title: String): Int = withContext(Dispatchers.IO) {
        val docs = read(context)
        docs[key(uri)]?.page
            ?: docs.values.firstOrNull { it.title == title }?.page
            ?: 0
    }

    suspend fun save(context: Context, uri: String, title: String, page: Int): Unit =
        withContext(Dispatchers.IO) {
            val docs = read(context).toMutableMap()
            docs[key(uri)] = Position(key(uri), title, page, System.currentTimeMillis())
            file(context).writeText(encode(topEntries(docs)))
        }

    internal fun topEntries(docs: Map<String, Position>): List<Position> =
        docs.values.sortedByDescending { it.updatedAt }.take(100)

    suspend fun clear(context: Context): Unit = withContext(Dispatchers.IO) {
        file(context).delete()
    }

    private fun read(context: Context): Map<String, Position> = runCatching {
        val arr = JSONArray(file(context).readText())
        (0 until arr.length()).associate { i ->
            val o = arr.getJSONObject(i)
            o.getString("key") to Position(
                o.getString("key"),
                o.getString("title"),
                o.getInt("page"),
                o.getLong("updatedAt")
            )
        }
    }.getOrDefault(emptyMap())

    private fun encode(list: List<Position>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("key", p.key)
                    .put("title", p.title)
                    .put("page", p.page)
                    .put("updatedAt", p.updatedAt)
            )
        }
        return arr.toString()
    }
}
