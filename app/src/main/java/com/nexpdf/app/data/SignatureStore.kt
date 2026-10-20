package com.nexpdf.app.data

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SignatureStore {

    data class Signature(val id: Long, val createdAt: Long, val strokes: List<List<Pair<Float, Float>>>)

    private fun file(context: Context) = File(context.filesDir, "signatures.json")

    suspend fun list(context: Context): List<Signature> = withContext(Dispatchers.IO) {
        runCatching {
            val arr = JSONArray(file(context).readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Signature(
                    id = o.getLong("id"),
                    createdAt = o.getLong("createdAt"),
                    strokes = decodeStrokes(o.getJSONArray("strokes"))
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun save(context: Context, strokes: List<List<Pair<Float, Float>>>): Signature =
        withContext(Dispatchers.IO) {
            val all = list(context).toMutableList()
            val sig = Signature(
                id = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                strokes = strokes
            )
            all += sig
            write(context, all)
            sig
        }

    suspend fun delete(context: Context, id: Long) = withContext(Dispatchers.IO) {
        write(context, list(context).filterNot { it.id == id })
    }

    private fun write(context: Context, list: List<Signature>) {
        val arr = JSONArray()
        list.forEach { s ->
            val o = JSONObject()
                .put("id", s.id)
                .put("createdAt", s.createdAt)
                .put("strokes", encodeStrokes(s.strokes))
            arr.put(o)
        }
        file(context).writeText(arr.toString())
    }

    private fun encodeStrokes(strokes: List<List<Pair<Float, Float>>>): JSONArray {
        val arr = JSONArray()
        strokes.forEach { stroke ->
            val sa = JSONArray()
            stroke.forEach { (x, y) ->
                sa.put(JSONArray().put(x.toDouble()).put(y.toDouble()))
            }
            arr.put(sa)
        }
        return arr
    }

    private fun decodeStrokes(a: JSONArray): List<List<Pair<Float, Float>>> =
        (0 until a.length()).map { j ->
            val pts = a.getJSONArray(j)
            (0 until pts.length()).map { k ->
                val p = pts.getJSONArray(k)
                p.getDouble(0).toFloat() to p.getDouble(1).toFloat()
            }
        }
}
