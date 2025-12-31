package com.example.ieeeconnect.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

// Placeholder Retrofit API for Cloudinary/aux services
public interface ApiService {
    @POST("cloudinary/upload")
    Call<UploadResponse> upload(@Body UploadRequest request);
}

