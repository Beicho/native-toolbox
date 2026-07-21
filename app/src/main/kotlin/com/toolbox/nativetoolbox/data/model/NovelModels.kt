package com.toolbox.nativetoolbox.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelDetail(
    @SerialName("book_id")
    val bookId: String,
    @SerialName("book_name")
    val bookName: String,
    @SerialName("author")
    val author: String,
    @SerialName("category")
    val category: String = "",
    @SerialName("word_number")
    val wordNumber: String = "",
    @SerialName("abstract")
    val abstract: String = ""
)

@Serializable
data class ChapterItem(
    @SerialName("item_id")
    val itemId: String,
    @SerialName("title")
    val title: String,
    @SerialName("book_id")
    val bookId: String = ""
)

@Serializable
data class ChapterDirectory(
    @SerialName("item_list")
    val itemList: List<ChapterItem> = emptyList()
)

@Serializable
data class ChapterContent(
    @SerialName("content")
    val content: String,
    @SerialName("title")
    val title: String = ""
)

@Serializable
data class SearchResult(
    @SerialName("book_id")
    val bookId: String,
    @SerialName("book_name")
    val bookName: String,
    @SerialName("author")
    val author: String,
    @SerialName("category")
    val category: String = "",
    @SerialName("score")
    val score: String = ""
)

@Serializable
data class SearchResponse(
    @SerialName("data")
    val data: List<SearchResult> = emptyList()
)
