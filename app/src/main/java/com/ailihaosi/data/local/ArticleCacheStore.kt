package com.ailihaosi.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Per-article offline cache under filesDir/article_cache/{id}.json.
 * Favorites are tracked in SharedPreferences and reuse the same cached bodies.
 */
class ArticleCacheStore(context: Context) {
    private val appContext = context.applicationContext
    private val gson = Gson()
    private val dir = File(appContext.filesDir, "article_cache").also { it.mkdirs() }
    private val prefs = appContext.getSharedPreferences("alhs_prefs", Context.MODE_PRIVATE)

    private fun fileFor(id: Int) = File(dir, "$id.json")

    fun save(article: CachedArticle, markFavorite: Boolean? = null): CachedArticle {
        val fav = markFavorite ?: isFavorite(article.id)
        val toSave = article.copy(favorited = fav, cachedAtEpochMs = System.currentTimeMillis())
        fileFor(article.id).writeText(gson.toJson(toSave))
        if (fav) addFavoriteId(article.id)
        return toSave
    }

    fun get(id: Int): CachedArticle? {
        val f = fileFor(id)
        if (!f.exists()) return null
        return runCatching { gson.fromJson(f.readText(), CachedArticle::class.java) }.getOrNull()
    }

    fun isCached(id: Int): Boolean = fileFor(id).exists()

    fun listCached(): List<CachedArticle> =
        dir.listFiles()?.mapNotNull { f ->
            runCatching { gson.fromJson(f.readText(), CachedArticle::class.java) }.getOrNull()
        }?.sortedByDescending { it.cachedAtEpochMs }.orEmpty()

    fun listFavorites(): List<CachedArticle> {
        val ids = favoriteIds()
        return ids.mapNotNull { get(it) }.sortedByDescending { it.cachedAtEpochMs }
    }

    fun isFavorite(id: Int): Boolean = favoriteIds().contains(id)

    fun setFavorite(id: Int, favorite: Boolean, seed: CachedArticle? = null) {
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

    private fun addFavoriteId(id: Int) {
        val ids = favoriteIds().toMutableSet()
        ids.add(id)
        prefs.edit().putStringSet(KEY_FAVS, ids.map { it.toString() }.toSet()).apply()
    }

    fun favoriteIds(): Set<Int> =
        prefs.getStringSet(KEY_FAVS, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()

    /** Delete one cached article. Favorite flag is also cleared. */
    fun remove(id: Int) {
        fileFor(id).delete()
        setFavorite(id, false)
    }

    /** Wipe all cached article bodies. Optionally clear favorites. */
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
