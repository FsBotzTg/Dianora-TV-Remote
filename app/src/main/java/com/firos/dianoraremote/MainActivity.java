package com.firos.dianoraremote;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Main Activity for Dianora TV Remote.
 *
 * Coordinates WebView UI presentation, hardware IR status verification,
 * first-run user onboarding, and tactile haptic feedback.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "DianoraRemotePrefs";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";
    private static final String ASSET_URL = "file:///android_asset/remote.html";

    private WebView remoteWebView;
    private IrTransmitterManager irTransmitterManager;
    private Vibrator hapticVibrator;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Hardware Abstraction & Haptic Feedback Services
        irTransmitterManager = new IrTransmitterManager(this);
        initializeVibrator();

        // Initialize WebView Interface
        remoteWebView = new WebView(this);
        WebSettings webSettings = remoteWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Bind JavaScript Bridge
        remoteWebView.addJavascriptInterface(new RemoteBridgeInterface(), "Android");
        setContentView(remoteWebView);
        remoteWebView.loadUrl(ASSET_URL);

        // First-Run Hardware Check & Welcome Onboarding
        evaluateFirstRunHardwareCheck();
    }

    /**
     * Initializes platform-appropriate Vibrator service for tactile keypress feedback.
     */
    private void initializeVibrator() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    hapticVibrator = vibratorManager.getDefaultVibrator();
                }
            } else {
                hapticVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }
        } catch (Exception e) {
            Log.w(TAG, "Vibrator service initialization skipped: " + e.getMessage());
        }
    }

    /**
     * Evaluates device hardware on launch and welcomes the user.
     */
    private void evaluateFirstRunHardwareCheck() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = preferences.getBoolean(KEY_FIRST_LAUNCH, true);

        if (isFirstLaunch) {
            preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
            showWelcomeHardwareDialog();
        } else if (!irTransmitterManager.hasHardwareEmitter()) {
            // If the user does not have an IR emitter, remind them unobtrusively on launch
            updateUiStatus();
        }
    }

    /**
     * Displays a Material dialog welcoming the user and reporting IR hardware status.
     */
    private void showWelcomeHardwareDialog() {
        boolean hasIr = irTransmitterManager.hasHardwareEmitter();

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
            .setTitle(hasIr ? R.string.welcome_title : R.string.hardware_check_title)
            .setMessage(hasIr ? R.string.hardware_check_ready_message : R.string.hardware_check_no_ir_message)
            .setCancelable(false)
            .setPositiveButton(R.string.btn_continue, (dialog, which) -> {
                dialog.dismiss();
                updateUiStatus();
            });

        if (!hasIr) {
            dialogBuilder.setNeutralButton(R.string.btn_check_again, (dialog, which) -> {
                // Re-instantiate hardware manager to detect newly connected peripherals
                irTransmitterManager = new IrTransmitterManager(MainActivity.this);
                updateUiStatus();
                if (irTransmitterManager.hasHardwareEmitter()) {
                    Toast.makeText(MainActivity.this, R.string.status_ir_active, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "Still no IR blaster detected. Check connection.", Toast.LENGTH_SHORT).show();
                    showWelcomeHardwareDialog();
                }
            });
        }

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    /**
     * Triggers brief haptic vibration for tactile button confirmation.
     */
    private void performHapticFeedback() {
        if (hapticVibrator == null || !hapticVibrator.hasVibrator()) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hapticVibrator.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                hapticVibrator.vibrate(28);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Synchronizes current hardware state to the HTML/JS interface.
     */
    private void updateUiStatus() {
        boolean isReady = irTransmitterManager.hasHardwareEmitter();
        String statusText = getString(isReady ? R.string.status_ir_active : R.string.status_ir_missing);
        String javascript = String.format(
            "if(window.DianoraRemote && window.DianoraRemote.setStatus){ window.DianoraRemote.setStatus(%b, '%s'); }",
            isReady, statusText
        );

        remoteWebView.post(() -> remoteWebView.evaluateJavascript(javascript, null));
    }

    /**
     * JavaScript Interface Bridge exposed to WebView under window.Android.
     */
    public class RemoteBridgeInterface {

        /**
         * Receives keypress events from the web-based remote control UI.
         *
         * @param keyName Alphanumeric key identifier (e.g., "POWER", "VOL_UP", "OK")
         * @return Human-readable execution status string
         */
        @JavascriptInterface
        @NonNull
        public String sendKey(String keyName) {
            performHapticFeedback();

            Integer commandCode = DianoraKeyMap.getCommandCode(keyName);
            if (commandCode == null) {
                Log.w(TAG, "Unrecognized remote key received: " + keyName);
                return "Unknown key: " + keyName;
            }

            IrTransmitterManager.TransmissionResult result = irTransmitterManager.sendCommand(commandCode);
            return result.getMessage();
        }

        /**
         * Invoked by WebView DOMContentLoaded to query initial hardware status.
         */
        @JavascriptInterface
        public void checkStatus() {
            runOnUiThread(MainActivity.this::updateUiStatus);
        }

        /**
         * Invoked if the user clicks the status indicator badge to re-check hardware.
         */
        @JavascriptInterface
        public void promptHardwareCheck() {
            runOnUiThread(MainActivity.this::showWelcomeHardwareDialog);
        }
    }

    @Override
    protected void onDestroy() {
        if (remoteWebView != null) {
            remoteWebView.destroy();
        }
        super.onDestroy();
    }
}
