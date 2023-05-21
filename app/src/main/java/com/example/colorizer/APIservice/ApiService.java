package com.example.colorizer.APIservice;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {

    private static final String UPLOAD_URL = "https://color-tool.azurewebsites.net/";

    private OkHttpClient client;
    private Gson gson;

    public ApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    public void uploadImage(File imageFile, Callback<UploadResponse> callback) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(MediaType.parse("image/*"), imageFile))
                .build();

        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    UploadResponse uploadResponse = gson.fromJson(responseBody, UploadResponse.class);
                    callback.onResponse(uploadResponse);
                    Log.d("response", responseBody);
                } else {
                    callback.onFailure(new IOException("Unexpected response code: " + response.code()));
                }
            }
        });
    }

    public interface Callback<T> {
        void onResponse(T response);
        void onFailure(IOException e);
    }

    public static class UploadResponse {
        @SerializedName("colorized_image_url")
        private String imageUrl;

        public String getImageUrl() {
            return imageUrl;
        }
    }
}
