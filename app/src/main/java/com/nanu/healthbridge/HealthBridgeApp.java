package com.nanu.healthbridge;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.topstep.fitcloud.sdk.v2.FcSDK;
import com.topstep.wearkit.base.ProcessLifecycleManager;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Application subclass that initializes the FitCloud SDK.
 * The SDK requires a Builder pattern with Application + ProcessLifecycleObserver.
 */
public class HealthBridgeApp extends Application {

    private static FcSDK fcSDK;
    private static HealthBridgeApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Handle RxJava Undeliverable exceptions (common with BLE disconnects)
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if (e instanceof java.io.IOException || e instanceof java.net.SocketException) {
                return;
            }
            if (e instanceof InterruptedException) {
                return;
            }
            android.util.Log.e("HealthBridgeApp", "Undeliverable exception received", e);
        });

        // Create a ProcessLifecycleManager (required by FcSDK.Builder)
        AppProcessLifecycleManager lifecycleManager = new AppProcessLifecycleManager();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleManager);

        // Build the FcSDK singleton
        fcSDK = new FcSDK.Builder(this, lifecycleManager).build();

        // The connector must be initialized on the main thread (SDK requirement)
        fcSDK.getConnector();
    }

    public static HealthBridgeApp getInstance() {
        return instance;
    }

    /**
     * Get the initialized FcSDK instance.
     */
    public static FcSDK getFcSDK() {
        return fcSDK;
    }

    /**
     * Lifecycle observer that tracks app foreground/background state.
     */
    private static class AppProcessLifecycleManager extends ProcessLifecycleManager
            implements DefaultLifecycleObserver {

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            setForeground(true);
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            setForeground(false);
        }
    }
}
