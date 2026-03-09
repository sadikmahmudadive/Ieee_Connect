package com.example.ieeeconnect.data.remote;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface ApiService {
    @POST("fcm/send")
    Call<ResponseBody> sendNotification(
            @HeaderMap Map<String, String> headers,
            @Body Map<String, Object> body
    );
}
