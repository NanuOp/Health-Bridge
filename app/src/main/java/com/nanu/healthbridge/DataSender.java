package com.nanu.healthbridge;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DataSender {
    private final String serverIp;
    private final int serverPort;
    private final OkHttpClient client;
    private final Gson gson;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface SendCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    public DataSender(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public void sendHealthData(HealthPayload payload, SendCallback callback) {
        String json = gson.toJson(payload);
        String url = String.format("http://%s:%d/health", serverIp, serverPort);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-Device", "FireBoltt-Crusader")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                callback.onSuccess(response.body().string());
            } else {
                callback.onFailure("Server error: " + response.code());
            }
        } catch (IOException e) {
            callback.onFailure("Network error: " + e.getMessage());
        }
    }

    public void sendRecoveryData(String json, SendCallback callback) {
        String url = String.format("http://%s:%d/recovery", serverIp, serverPort);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-Device", "FireBoltt-Crusader")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                callback.onSuccess(response.body().string());
            } else {
                callback.onFailure("Server error: " + response.code());
            }
        } catch (IOException e) {
            callback.onFailure("Network error: " + e.getMessage());
        }
    }

    public boolean ping() {
        String url = String.format("http://%s:%d/ping", serverIp, serverPort);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }
}
