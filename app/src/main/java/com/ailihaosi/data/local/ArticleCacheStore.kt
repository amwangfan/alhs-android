package com.ailihaosi.data.local

import android.content.Context
import com.google.gson.Gson
import java.io.File

class ArticleCacheStore(context: Context) {
    private val appContext = context.applicationContext
    private val gson = Gson()
    private val dir = File(appContext.filesDir, "article_cache").also { it.mkdirs() }
    private val prefs = appContext.getSharedPreferences("alhs_prefs", Context.MODE_PRIVATE)

    private fun fileFor(id: Long) = File(dir, "$id.json")

    fun save(article: CachedArticle, markFavorite: Boolean? = null): CachedArticle {
        val fav = markFavorite ?: isFavorite(article.id)
        val toSave = article.copy(favorited = fav, cachedAtEpochMs = System.currentTimeMillis())
        fileFor(article.id).writeText(gson.toJson(toSave))
        if (fav) addFavoriteId(article.id)
        return toSave
    }

    fun get(id: Long): CachedArticle? {
        val f = fileFor(id)
        if (!f.exists()) return null
        return runCatching { gson.fromJson(f.readText(), CachedArticle::class.java) }.getOrNull()
    }

    fun isCached(id: Long): Boolean = fileFor(id).exists()

    fun listCached(): List<CachedArticle> =
        dir.listFiles()?.mapNotNull { f ->
            runCatching { gson.fromJson(f.readText(), CachedArticle::class.java) }.getOrNull()
        }?.sortedByDescending { it.cachedAtEpochMs }.orEmpty()

    fun listFavorites(): List<CachedArticle> {
        val ids = favoriteIds()
        return ids.mapNotNull { get(it) }.sortedByDescending { it.cachedAtEpochMs }
    }

    fun isFavorite(id: Long): Boolean = favoriteIds().contains(id)

    fun setFavorite(id: Long, favorite: Boolean, seed: CachedArticle? = null) {
        val ids = favoriteIds().toMutableSet()
        if (favorite) {
            ids.add(id)
            val existing = get(id) ?: seed
            if (existing != null) {
                save(existing.copy(favorited = true), markFavorite = true)
            } else {
                prefs.edit().putStringSet(KEY_FAVS, ids.map { it.toString() }.toSet()).apply()
            }
        } else {
            ids.remove(id)
            prefs.edit().putStringSet(KEY_FAVS, ids.map { it.toString() }.toSet()).apply()
            get(id)?.let { save(it.copy(favorited = false), markFavorite = false) }
        }
        prefs.edit().putStringSet(KEY_FAVS, ids.map { it.toString() }.toSet()).apply()
    }

    private fun addFavoriteId(id: Long) {
        val ids = favoriteIds().toMutableSet()
        ids.add(id)
        prefs.edit().putStringSet(KEY_FAVS, ids.map { it.toString() }.toSet()).apply()
    }

    fun favoriteIds(): Set<Long> =
        prefs.getStringSet(KEY_FAVS, emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet().orEmpty()

    fun remove(id: Long) {
        fileFor(id).delete()
        setFavorite(id, false)
    }

    fun clearAll(clearFavorites: Boolean = true) {
        dir.listFiles()?.forEach { it.delete() }
        if (clearFavorites) {
            prefs.edit().remove(KEY_FAVS).apply()
        }
    }

    fun cacheBytes(): Long =
        dir.listFiles()?.sumOf { it.length() } ?: 0L

    fun cacheCount(): Int = dir.listFiles()?.size ?: 0

    companion object {
        private const val KEY_FAVS = "favorite_ids"

        @Volatile private var instance: ArticleCacheStore? = null
        fun get(context: Context): ArticleCacheStore =
            instance ?: synchronized(this) {
                instance ?: ArticleCacheStore(context).also { instance = it }
            }
    }
}
