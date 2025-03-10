package com.mandrykevich.myhelper.presentation.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mandrykevich.myhelper.data.repository.SearchQuery

class SearchViewModel : ViewModel() {
    private val _searchQueries = MutableLiveData<List<SearchQuery>>(emptyList())
    val searchQueries: LiveData<List<SearchQuery>> get() = _searchQueries

    fun addSearchQuery(query: String) {
        val newQuery = SearchQuery(query, System.currentTimeMillis())
        val updatedList = _searchQueries.value.orEmpty() + newQuery
        _searchQueries.value = updatedList
    }
}