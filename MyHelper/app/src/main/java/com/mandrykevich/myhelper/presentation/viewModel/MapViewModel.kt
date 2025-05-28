package com.mandrykevich.myhelper.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val searchManager: SearchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private var searchSession: Session? = null

    private val _searchResults = MutableStateFlow<List<GeoObject>>(emptyList())
    val searchResults: StateFlow<List<GeoObject>> = _searchResults

    fun performSearch(query: String, visibleRegion: Geometry) {
        searchSession?.cancel()
        searchSession = searchManager.submit(
            query,
            visibleRegion,
            SearchOptions(),
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val geoObjects = response.collection.children.mapNotNull { it.obj }
                    _searchResults.value = geoObjects
                }

                override fun onSearchError(error: Error) {
                    // Обработка ошибки
                }
            }
        )
    }

    fun processSearchResults(response: Response) {
        viewModelScope.launch {
            val geoObjects = response.collection.children.mapNotNull { it.obj }
            _searchResults.value = geoObjects
        }
    }
}
