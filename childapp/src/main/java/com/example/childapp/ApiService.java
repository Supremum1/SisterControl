package com.example.childapp;

import com.example.childapp.config.ServerConfig;
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
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/register")
    Call<AuthResponse> register(@Body RegisterRequest body);

    @POST("api/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("api/child/create-code")
    Call<CreateCodeResponse> createCode(@Header("Authorization") String token);

    @GET("api/child/pair-status")
    Call<PairStatusResponse> pairStatus(@Header("Authorization") String token);

    @GET("api/child/connection-info")
    Call<ChildConnectionInfo> childConnectionInfo(@Header("Authorization") String token);

    @POST("api/child/event")
    Call<BasicResponse> sendChildEvent(
            @Header("Authorization") String token,
            @Body ChildEventRequest body
    );

    @GET("api/child/commands")
    Call<CommandsResponse> getCommands(
            @Header("Authorization") String token,
            @Query("limit") int limit
    );

    @POST("api/child/commands/{id}/done")
    Call<BasicResponse> commandDone(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    @POST("api/child/location")
    Call<BasicResponse> sendLocation(
            @Header("Authorization") String token,
            @Body LocationUpdateRequest body
    );

    @POST("api/child/trust-letter")
    Call<TrustLetterResponse> sendTrustLetter(
            @Header("Authorization") String token,
            @Body TrustLetterRequest body
    );

    @GET("api/child/parent-messages")
    Call<ParentMessagesResponse> getParentMessages(
            @Header("Authorization") String token,
            @Query("limit") int limit
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
