package com.oohtracker.networking

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface ApiService {
    @Multipart
    @POST("/api/upload")
    fun postImage(
        @Header("authorization") auth: String?,
        @Part("upload") name: RequestBody?,
        @Part("message") message: RequestBody,
        @Part image: MultipartBody.Part?,
    ): Call<ResponseBody?>?
}