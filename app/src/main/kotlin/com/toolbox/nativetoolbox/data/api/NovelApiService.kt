package com.toolbox.nativetoolbox.data.api

import com.toolbox.nativetoolbox.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface NovelApiService {

    @GET("/api/detail")
    suspend fun getBookDetail(
        @Query("book_id") bookId: String
    ): NovelDetail

    @GET("/api/directory")
    suspend fun getChapterDirectory(
        @Query("book_id") bookId: String
    ): ChapterDirectory

    @GET("/api/content")
    suspend fun getChapterContent(
        @Query("item_id") itemId: String
    ): ChapterContent

    @GET("/api/search")
    suspend fun searchBooks(
        @Query("key") keyword: String
    ): SearchResponse
}
