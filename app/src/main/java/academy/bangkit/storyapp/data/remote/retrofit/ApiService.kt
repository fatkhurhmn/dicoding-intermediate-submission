package academy.bangkit.storyapp.data.remote.retrofit

import academy.bangkit.storyapp.data.remote.response.ListStoryResponse
import academy.bangkit.storyapp.data.remote.response.LoginResponse
import academy.bangkit.storyapp.data.remote.response.RegisterResponse
import retrofit2.http.*

interface ApiService {
    @FormUrlEncoded
    @POST("register")
    suspend fun registerUser(
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("password") password: String,
    ): RegisterResponse

    @FormUrlEncoded
    @POST("login")
    suspend fun loginUser(
        @Field("email") email: String,
        @Field("password") password: String
    ): LoginResponse

    @GET("stories")
    suspend fun getAllStories(@Header("Authorization") token: String): ListStoryResponse
}