package com.netease.demoApp.plugins;

import org.json.JSONException;
import com.netease.JSBridge.LDJSCallbackContext;
import com.netease.JSBridge.LDJSParams;
import com.netease.JSBridge.LDJSPlugin;


public class LDPUIGlobalCtrl extends LDJSPlugin {
    public static final String TAG = "LDPAppInfo";

    /**
     * Constructor.
     */
    public LDPUIGlobalCtrl() {
    }


    @Override
    public boolean execute(String realMethod, LDJSParams args, LDJSCallbackContext callbackContext) throws JSONException {
        if (realMethod.equals("showActionSheet")) {
            callbackContext.success(1); //int 代替true
        }


        else {
            return false;
        }
        return true;
    }
}
