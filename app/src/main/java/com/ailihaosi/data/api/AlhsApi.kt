package com.ailihaosi.data.api

import com.ailihaosi.data.model.AnnouncementDto
import com.ailihaosi.data.model.ApiEnvelope
import com.ailihaosi.data.model.AuthSessionDto
import com.ailihaosi.data.model.AuthUserDto
import com.ailihaosi.data.model.BodyDto
import com.ailihaosi.data.model.BootstrapDto
import com.ailihaosi.data.model.PostDto
import com.ailihaosi.data.model.PublicUserDto
import com.ailihaosi.data.model.RankingDto
import com.ailihaosi.data.model.RecentItemDto
import com.ailihaosi.data.model.TermRef
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AlhsApi {
    @GET("posts")
    suspend fun listPosts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("type") type: String = "post",
        @Query("orderby") orderby: String = "date",
        @Query("order") order: String = "desc",
        @Query("author") author: Long? = null,
        @Query("category") category: Long? = null,
        @Query("tag") tag: Long? = null,
        @Query("series") series: String? = null,
        @Query("search") search: String? = null
    ): Response<ApiEnvelope<List<PostDto>>>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Long): Response<ApiEnvelope<PostDto>>

    @GET("posts/{id}/body")
    suspend fun getPostBody(@Path("id") id: Long): Response<ApiEnvelope<BodyDto>>

    @GET("posts/recent")
    suspend fun recentPosts(@Query("limit") limit: Int = 20): Response<ApiEnvelope<List<RecentItemDto>>>

    @GET("public/bootstrap")
    suspend fun bootstrap(): Response<ApiEnvelope<BootstrapDto>>

    @GET("announcements")
    suspend fun announcements(): Response<ApiEnvelope<List<AnnouncementDto>>>

    @GET("activity-bar")
    suspend fun activityBar(): Response<ApiEnvelope<List<AnnouncementDto>>>

    @GET("rankings/{mode}")
    suspend fun rankings(
        @Path("mode") mode: String,
        @Query("period") period: String = "month",
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<RankingDto>>

    @GET("categories")
    suspend fun categories(@Query("q") q: String? = null): Response<ApiEnvelope<List<TermRef>>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Long): Response<ApiEnvelope<PublicUserDto>>

    @GET("auth/me")
    suspend fun me(): Response<ApiEnvelope<AuthUserDto>>

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<ApiEnvelope<AuthSessionDto>>

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): Response<ApiEnvelope<AuthSessionDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiEnvelope<AuthSessionDto>>
}
