package com.fetch.interview.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fetch.framework.http.client.interfaces.FetchApiClientInterfaces
import com.fetch.framework.http.provider.FetchApiClientProvider
import com.fetch.framework.http.responses.GetItemsResponse
import com.fetch.framework.http.responses.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityViewModel : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // response as a StateFlow
    private val _response = MutableStateFlow<GetItemsResponse<Item>?>(null)
    val response = _response.asStateFlow()

    // api client
    private val fetchApiClient: FetchApiClientInterfaces = FetchApiClientProvider.getInstance()

    init {
        fetchItems()
    }

    fun fetchItems() {
        viewModelScope.launch {
            _isRefreshing.emit(true)
            // the CoroutineDispatcher that is designed for offloading blocking IO
            // tasks to a shared pool of threads
            withContext(Dispatchers.IO) {
                // update the response StateFlow with the API response
                _response.value = fetchApiClient.getItems()
            }
            _isRefreshing.emit(false)
        }
    }
}
