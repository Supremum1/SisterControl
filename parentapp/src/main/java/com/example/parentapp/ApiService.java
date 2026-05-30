package com.example.parentapp;

import com.example.parentapp.config.ServerConfig;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/register")
    Call<AuthResponse> register(@Body RegisterRequest body);

    @POST("api/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("api/parent/pair")
    Call<Map<String, Object>> pair(
            @Header("Authorization") String token,
            @Body PairRequest body
    );

    @GET("api/parent/connections")
    Call<ParentConnectionsResponse> getConnections(@Header("Authorization") String token);

    @GET("api/parent/events")
    Call<EventsResponse> getParentEvents(@Header("Authorization") String token);

    @POST("api/parent/request-gps")
    Call<BasicResponse> requestGps(@Header("Authorization") String token);

    @GET("api/parent/child-location")
    Call<ChildLocationResponse> getChildLocation(@Header("Authorization") String token);

    @POST("api/parent/safe-zone")
    Call<BasicResponse> setSafeZone(
            @Header("Authorization") String token,
            @Body SafeZoneRequest body
    );

    @POST("api/parent/screen-time")
    Call<BasicResponse> setScreenTime(
            @Header("Authorization") String token,
            @Body ScreenTimePolicyRequest body
    );

    @GET("api/parent/screen-time")
    Call<ScreenTimePolicyResponse> getScreenTime(@Header("Authorization") String token);

    @GET("api/parent/screen-time-usage")
    Call<ScreenTimeUsageResponse> getScreenTimeUsage(
            @Header("Authorization") String token,
            @Query("day") String day
    );

    @GET("api/parent/trust-letters")
    Call<TrustLettersResponse> getTrustLetters(
            @Header("Authorization") String token,
            @Query("limit") int limit
    );

    @POST("api/parent/message-to-child")
    Call<ParentMessageToChildResponse> sendMessageToChild(
            @Header("Authorization") String token,
            @Body ParentMessageToChildRequest body
    );

    @DELETE("api/me")
    Call<BasicResponse> deleteMe(@Header("Authorization") String token);

    static ApiService create() {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(log)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ServerConfig.apiBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(ApiService.class);
    }
}
