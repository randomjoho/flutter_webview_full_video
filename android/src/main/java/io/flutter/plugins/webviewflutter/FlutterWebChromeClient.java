package io.flutter.plugins.webviewflutter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;


class FlutterWebChromeClient extends WebChromeClient {

    public interface MyCallBack{
        void send(boolean isFullScreen);
    }

    public MyCallBack myCallBack;
    private static final String TAG = "FlutterWebChromeClient";
    private final MethodChannel methodChannel;
    public boolean isFullscreen = false;
    private CustomViewCallback customViewCallback;
    private View customView;
    private ViewGroup rootView;
    FlutterWebChromeClient(MethodChannel methodChannel, ViewGroup rootView, MyCallBack myCallBack) {
        this.methodChannel = methodChannel;
        this.rootView = rootView;
        this.myCallBack = myCallBack;

    }



    @SuppressWarnings("SuspiciousNameCombination")
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback);
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }
        customView = view;

        Activity activity = WebViewFlutterPlugin.activityRef.get();
        if (activity != null) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            customView.setLayoutParams(new ViewGroup.LayoutParams(
                    getMetricsDisplay(activity).heightPixels,
                    getMetricsDisplay(activity).widthPixels));
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        rootView.addView(customView);
        customViewCallback = callback;
        isFullscreen = true;

        onScreenOrientationChanged(true);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onHideCustomView() {
        if (customView == null) return;
        Activity activity = WebViewFlutterPlugin.activityRef.get();
        if (activity != null) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        rootView.removeView(customView);
        customViewCallback.onCustomViewHidden();
        customView = null;
        isFullscreen = false;
        onScreenOrientationChanged(false);
        super.onHideCustomView();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPermissionRequest(PermissionRequest request) {
        request.grant(request.getResources());
    }

    boolean exitFullscreen() {
        if (isFullscreen) {
            onHideCustomView();
            return true;
        }
        return false;
    }

    private DisplayMetrics getMetricsDisplay(Activity activity) {
        WindowManager manager = activity.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics;
    }

    private void onScreenOrientationChanged(final boolean isFullscreen) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                myCallBack.send(isFullscreen);
            }
        },1500);

        Map<String, Object> args = new HashMap<>();
        args.put("isLandscape", isFullscreen);
        methodChannel.invokeMethod("onScreenOrientationChanged", args);
    }
}
