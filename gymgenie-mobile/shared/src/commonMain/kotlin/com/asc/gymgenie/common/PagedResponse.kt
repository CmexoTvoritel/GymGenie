package com.asc.gymgenie.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    @SerialName("page") val number: Int? = null,
    val size: Int? = null,
    val last: Boolean? = null,
)
