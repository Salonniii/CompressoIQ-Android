package com.example.aiphotocompressor;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // =========================
    // AUTH APIs
    // =========================

    @POST("api/auth/signup")
    Call<Map<String, String>> signup(@Body SignupRequest request);

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/forgot-password")
    Call<Map<String, String>> forgotPassword(@Body ForgotPasswordRequest request);


    // =========================
    // IMAGE APIs (EXISTING)
    // =========================

    @Multipart
    @POST("api/images/upload")
    Call<ImageResponse> uploadImage(
            @Part MultipartBody.Part file,
            @Part("targetSizeKB") RequestBody targetSizeKB,
            @Part("compressionMode") RequestBody compressionMode,
            @Part("outputFormat") RequestBody outputFormat,
            @Part("userId") RequestBody userId
    );

    @GET("api/images/history/{userId}")
    Call<List<ImageResponse>> getUserHistory(@Path("userId") Long userId);

    @DELETE("api/images/delete/{id}")
    Call<DeleteResponse> deleteImage(@Path("id") Long id);

    @GET("api/images/download/{id}")
    Call<ResponseBody> downloadCompressedImage(@Path("id") Long id);


    // =========================
    // DOCUMENT APIs (NEW ✅)
    // =========================

    @Multipart
    @POST("api/documents/upload")
    Call<DocumentResponse> uploadDocument(
            @Part MultipartBody.Part file,
            @Part("targetSizeKB") RequestBody targetSizeKB,
            @Part("userId") RequestBody userId
    );

    @GET("api/documents/download/{id}")
    Call<ResponseBody> downloadDocument(@Path("id") Long id);

    @GET("api/documents/history/{userId}")
    Call<List<DocumentResponse>> getDocumentHistory(@Path("userId") Long userId);

    @DELETE("api/documents/delete/{id}")
    Call<DeleteResponse> deleteDocument(@Path("id") Long id);
}