package com.netease.demoApp.plugins;

import org.json.JSONException;

import com.netease.JSBridge.LDJSCallbackContext;
import com.netease.JSBridge.LDJSParams;
import com.netease.JSBridge.LDJSPlugin;


public class LDPNavCtrl extends LDJSPlugin {
    public static final String TAG = "LDPAppInfo";

    /**
     * Constructor.
     */
    public LDPNavCtrl() {
    }


    public boolean execute(String realMethod, LDJSParams args, LDJSCallbackContext callbackContext) throws JSONException {
        if (realMethod.equals("openLinkInNewWebView")) {
            callbackContext.success(1); //int 代替true
        }


        else {
            return false;
        }
        return true;
    }
}
