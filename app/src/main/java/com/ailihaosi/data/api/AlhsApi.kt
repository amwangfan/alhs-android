package com.ailihaosi.data.api

import com.ailihaosi.data.model.AnnouncementDto
import com.ailihaosi.data.model.ApiEnvelope
import com.ailihaosi.data.model.AuthSessionDto
import com.ailihaosi.data.model.AuthUserDto
import com.ailihaosi.data.model.BodyDto
import com.ailihaosi.data.model.BootstrapDto
import com.ailihaosi.data.model.PostDto
import com.ailihaosi.data.model.RankingDto
import com.ailihaosi.data.model.RecentItemDto
import com.ailihaosi.data.model.TermRef
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Preview-stack /api/v1 only. No wp-json.
 */
interface AlhsApi {
    @GET("posts")
    suspend fun listPosts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("type") type: String = "post",
        @Query("orderby") orderby: String = "date",
        @Query("order") order: String = "desc",
        @Query("author") author: Int? = null,
        @Query("category") category: Int? = null,
        @Query("search") search: String? = null
    ): Response<ApiEnvelope<List<PostDto>>>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Response<ApiEnvelope<PostDto>>

    @GET("posts/{id}/body")
    suspend fun getPostBody(@Path("id") id: Int): Response<ApiEnvelope<BodyDto>>

    @GET("posts/recent")
    suspend fun recentPosts(@Query("limit") limit: Int = 20): Response<ApiEnvelope<List<RecentItemDto>>>

    @GET("public/bootstrap")
    suspend fun bootstrap(): Response<ApiEnvelope<BootstrapDto>>

    @GET("announcements")
    suspend fun announcements(): Response<ApiEnvelope<List<AnnouncementDto>>>

    @GET("rankings/{mode}")
    suspend fun rankings(
        @Path("mode") mode: String,
        @Query("period") period: String = "month",
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<RankingDto>>

    @GET("categories")
    suspend fun categories(@Query("q") q: String? = null): Response<ApiEnvelope<List<TermRef>>>

    @GET("auth/me")
    suspend fun me(): Response<ApiEnvelope<AuthUserDto>>

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<ApiEnvelope<AuthSessionDto>>

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): Response<ApiEnvelope<AuthSessionDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiEnvelope<AuthSessionDto>>
}

/** static-preview.alhs.xyz public R2 JSON (preferred body source). */
interface StaticApi {
    @GET("v1/articles/{id}.json")
    suspend fun article(@Path("id") id: Int): Response<com.ailihaosi.data.model.StaticArticleDto>
}
