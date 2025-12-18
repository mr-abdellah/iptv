package com.example.iptv.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences =
            context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow<Set<Int>>(loadFavorites())
    val favorites: StateFlow<Set<Int>> = _favorites.asStateFlow()

    private fun loadFavorites(): Set<Int> {
        return prefs.getStringSet("favorite_channels", emptySet())
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet()
    }

    fun addFavorite(channelId: Int) {
        val currentFavorites = _favorites.value.toMutableSet()
        currentFavorites.add(channelId)
        saveFavorites(currentFavorites)
        _favorites.value = currentFavorites
    }

    fun removeFavorite(channelId: Int) {
        val currentFavorites = _favorites.value.toMutableSet()
        currentFavorites.remove(channelId)
        saveFavorites(currentFavorites)
        _favorites.value = currentFavorites
    }

    fun toggleFavorite(channelId: Int) {
        if (isFavorite(channelId)) {
            removeFavorite(channelId)
        } else {
            addFavorite(channelId)
        }
    }

    fun isFavorite(channelId: Int): Boolean {
        return _favorites.value.contains(channelId)
    }

    private fun saveFavorites(favorites: Set<Int>) {
        prefs.edit()
                .putStringSet("favorite_channels", favorites.map { it.toString() }.toSet())
                .apply()
    }
}
