package com.nanu.healthbridge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.nanu.healthbridge.db.HealthDay;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecoveryApiService {

    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService executor;

    public interface InsightCallback {
        void onInsight(String insight);
        void onError(String error);
    }

    public RecoveryApiService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void getRecoveryInsight(HealthDay today, RecoveryCalculator.RecoveryResult result,
                                   InsightCallback callback) {
        
        executor.execute(() -> {
            int h = today.sleepTotalMinutes / 60;
            int m = today.sleepTotalMinutes % 60;
            
            String sleepStartFormatted = result.sleepStartTimeFormatted;
            String sleepEndFormatted = "--";
            if (today.sleepEndTime > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.US);
                sleepEndFormatted = sdf.format(new java.util.Date(today.sleepEndTime));
            }

            String interpretation = "";
            if (today.restingHR > 0) {
                interpretation = (today.restingHR <= result.profile.getHrScoreIdealMax()) ? "within normal range" : "elevated";
            }

            String prompt = String.format(Locale.US,
                "You are a personal health coach analyzing recovery data for %s, a %d-year-old %s athlete (Primary sport: %s).\n\n" +
                "Sleep analysis:\n" +
                " - Duration: %d minutes (%dh %dm)\n" +
                " - Bedtime: %s %s\n" +
                " - Wake time: %s\n" +
                " - Duration score: %d/100\n" +
                " - Timing score: %d/100\n" +
                " - Resting HR during sleep: %d BPM. For a %s athlete, this reading is %s (Normal range: %d-%d BPM).\n\n" +
                "Yesterday's Strain Impact:\n" +
                " - Yesterday's Strain: %d/100\n" +
                " - Strain penalty applied: -%d points\n" +
                " - Sleep mitigation: +%d points\n" +
                " - Net strain impact: %s\n\n" +
                "Overall Recovery Score: %d/100 (%s)\n" +
                "SpO2: %.1f%% avg\n" +
                "Steps Today: %d\n\n" +
                "Important: Do not suggest this data is medical advice. Give exactly 3 sentences:\n" +
                "1. Recovery state based on resting HR, sleep timing, and yesterday's strain\n" +
                "2. Key factor helping or hurting recovery today\n" +
                "3. One specific actionable recommendation for their %s training\n\n" +
                "Use actual numbers. Be direct. No medical claims. NO EMOJIS.",
                result.profile.name, result.profile.age, result.profile.fitnessLevel, result.profile.sport,
                today.sleepTotalMinutes, h, m,
                sleepStartFormatted, result.lateSleepWarning ? "[⚠️ after midnight]" : "",
                sleepEndFormatted,
                result.sleepDurationScore, result.sleepTimingScore,
                today.restingHR, result.profile.fitnessLevel, interpretation, result.profile.getHrScoreIdealMin(), result.profile.getHrScoreIdealMax(),
                today.yesterdayStrainScore,
                result.strainPenaltyApplied,
                result.sleepMitigationApplied,
                result.strainImpactLabel,
                result.finalScore, result.zone,
                today.avgSpO2,
                today.totalSteps,
                result.profile.sport
            );

            // Groq Chat Completion Body
            JsonObject bodyJson = new JsonObject();
            bodyJson.addProperty("model", "llama-3.1-8b-instant");
            
            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);
            
            bodyJson.add("messages", messages);

            RequestBody body = RequestBody.create(
                    gson.toJson(bodyJson),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("Groq Error: " + response.code() + " " + response.message());
                        return;
                    }

                    try {
                        String respStr = response.body().string();
                        JsonObject respJson = gson.fromJson(respStr, JsonObject.class);
                        String text = respJson.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString();
                        
                        callback.onInsight(text.trim());
                    } catch (Exception e) {
                        callback.onError("Parse Error: " + e.getMessage());
                    }
                }
            });
        });
    }
}
