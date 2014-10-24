package com.netease.demoApp.plugins;

import org.json.JSONException;
import android.webkit.WebView;

import com.netease.JSBridge.LDJSCallbackContext;
import com.netease.JSBridge.LDJSParams;
import com.netease.JSBridge.LDJSPlugin;
import com.netease.JSBridge.LDJSActivityInterface;


public class LDPAppInfo extends LDJSPlugin {
    public static final String TAG = "LDPAppInfo";
    
    /**
     * Constructor.
     */
    public LDPAppInfo() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param activityInterface The context of the WebView Activity.
     * @param webView The CordovaWebView is running in activity.
     */
    public void initialize(LDJSActivityInterface activityInterface, WebView webView) {
        super.initialize(activityInterface, webView);
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    @Override
    public boolean execute(String action, LDJSParams args, LDJSCallbackContext callbackContext) throws JSONException {
        if (action.equals("isAppInstalled")) {
            callbackContext.success(1); //int 代替true
        }
        
        
        else {
            return false;
        }
        return true;
    }
}
