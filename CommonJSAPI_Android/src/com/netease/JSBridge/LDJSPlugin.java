package com.netease.JSBridge;

import org.json.JSONException;

import android.content.Intent;
import android.webkit.WebView;
import android.net.Uri;

import com.netease.JSBridge.LDJSActivityInterface;
import com.netease.JSBridge.LDJSParams;
import com.netease.JSBridge.LDJSCallbackContext;


/**
 * Plugins must extend this class and override one of the execute methods.
 */
public class LDJSPlugin {
    @Deprecated // This is never set.
    public String id;
    public WebView webView;
    public LDJSActivityInterface activityInterface;

    /**
     * Call this after constructing to initialize the plugin.
     * Final because we want to be able to change args without breaking plugins.
     */
    public final void privateInitialize(LDJSActivityInterface activityInterface, WebView theWebView) {
        assert this.activityInterface == null;
        this.activityInterface = activityInterface;
        this.webView = theWebView;
        initialize(activityInterface, webView);
        pluginInitialize();
    }

    /**
     * Called after plugin construction and fields have been initialized.
     * Prefer to use pluginInitialize instead since there is no value in
     * having parameters on the initialize() function.
     */
    public void initialize(LDJSActivityInterface activityInterface, WebView webView) {
    }

    /**
     * Called after plugin construction and fields have been initialized.
     */
    protected void pluginInitialize() {
    }


    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     activityInterface.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     activityInterface.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments, wrapped with some activityInterface helpers.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     */
    public boolean execute(String action, LDJSParams args, LDJSCallbackContext callbackContext) throws JSONException {
        return false;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    public void onPause(boolean multitasking) {
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    public void onResume(boolean multitasking) {
    }

    /**
     * Called when the activity receives a new intent.
     */
    public void onNewIntent(Intent intent) {
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    public void onDestroy() {
    }

    /**
     * Called when a message is sent to plugin.
     *
     * @param id            The message id
     * @param data          The message data
     * @return              Object to stop propagation or null
     */
    public Object onMessage(String id, Object data) {
        return null;
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode		The request code originally supplied to startActivityForResult(),
     * 							allowing you to identify who this result came from.
     * @param resultCode		The integer result code returned by the child activity through its setResult().
     * @param intent				An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    }

    /**
     * By specifying a <url-filter> in config.xml you can map a URL (using startsWith atm) to this method.
     *
     * @param url				The URL that is trying to be loaded in the activityInterface webview.
     * @return					Return true to prevent the URL from loading. Default is false.
     */
    public boolean onOverrideUrlLoading(String url) {
        return false;
    }

    /**
     * Hook for redirecting requests. Applies to WebView requests as well as requests made by plugins.
     */
    public Uri remapUri(Uri uri) {
        return null;
    }

    /**
     * Called when the WebView does a top-level navigation or refreshes.
     *
     * Plugins should stop any long-running processes and clean up internal state.
     *
     * Does nothing by default.
     */
    public void onReset() {
    }
}
