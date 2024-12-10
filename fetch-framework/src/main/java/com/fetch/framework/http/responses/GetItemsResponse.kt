package com.fetch.framework.http.responses

import com.fetch.framework.http.responses.internal.EmptyStateInfo
import com.google.gson.annotations.SerializedName

data class GetItemsResponse<T : EmptyStateInfo>(
    val items: List<T> = emptyList()
) : EmptyStateInfo {

    override fun isEmpty(): Boolean = this == EMPTY

    companion object {

        /**
         * An empty object instance for [GetItemsResponse].
         *
         * If the API were to respond back with a successful response but with an empty body,
         * clients will get back an [EMPTY] object for [GetItemsResponse].
         */
        val EMPTY = GetItemsResponse<Item>()
    }
}

data class Item(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("listId") val listId: Long? = null,
    @SerializedName("name") val name: String? = null,
) : EmptyStateInfo {

    override fun isEmpty(): Boolean = this == EMPTY

    companion object {

        /**
         * An empty object instance for [Item].
         *
         * If the API were to respond back with a successful response but with an empty body,
         * clients will get back an [EMPTY] object for [Item].
         */
        val EMPTY = Item()
    }
}
