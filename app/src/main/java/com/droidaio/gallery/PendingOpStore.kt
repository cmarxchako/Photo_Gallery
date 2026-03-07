package com.droidaio.gallery

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 *  Simple SharedPreferences-backed store for PendingOperation list.
 * Stores JSON array under key "pending_ops_v1".
 * This class manages the storage of pending operations (copy/move/delete) that need to be executed
 * when the device is back online. It uses SharedPreferences to store a JSON array of PendingOperation
 * objects under a specific key. The loadAll() method retrieves the list of pending operations, while
 * saveAll() saves the entire list back to SharedPreferences. The add(), update(), and remove() methods
 * allow adding a new operation, updating an existing one (by matching the id), or removing an operation
 * by its id. This simple implementation is suitable for small lists of pending operations; if the list
 * grows large, we might want to consider a more robust storage solution (e.g. SQLite or Room), but for
 * the expected use case of a few pending operations, SharedPreferences should be sufficient and easy to
 * implement.
 *
 */
class PendingOpStore(private val context : Context) {

    private val prefs = context.getSharedPreferences("pending_ops_store", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY = "pending_ops_v1"

    fun loadAll() : MutableList<PendingOperation> {
        val json = prefs.getString(KEY, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<PendingOperation>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e : Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun saveAll(list : List<PendingOperation>) {
        try {
            val json = gson.toJson(list)
            prefs.edit().putString(KEY, json).apply()
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    fun add(op : PendingOperation) {
        val list = loadAll()
        list.add(op)
        saveAll(list)
    }

    fun update(op : PendingOperation) {
        val list = loadAll()
        val idx = list.indexOfFirst { it.id == op.id }
        if (idx >= 0) {
            list[idx] = op
            saveAll(list)
        } else {
            add(op)
        }
    }

    fun remove(opId : UUID) {
        val list = loadAll()
        val newList = list.filter { it.id != opId }
        saveAll(newList)
    }
}
