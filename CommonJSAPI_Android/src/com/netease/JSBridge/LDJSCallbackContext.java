package com.netease.JSBridge;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;
import android.webkit.WebView;

import com.netease.JSBridge.LDJSPluginResult;

public class LDJSCallbackContext {
    private static final String LOG_TAG = "LDJSCallbackContext";

    private String callbackId;
    private WebView webView;
    private boolean finished;
    private int changingThreads;

    public LDJSCallbackContext(String callbackId, WebView webView) {
        this.callbackId = callbackId;
        this.webView = webView;
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    public boolean isChangingThreads() {
        return changingThreads > 0;
    }
    
    public String getCallbackId() {
        return callbackId;
    }

    public void sendPluginResult(LDJSPluginResult pluginResult) {
        synchronized (this) {
            if (finished) {
                Log.w(LOG_TAG, "Attempted to send a second callback for ID: " + callbackId + "\nResult was: " + pluginResult.getMessage());
                return;
            } else {
                finished = !pluginResult.getKeepCallback();
            }
        }
        
        webViewSendPluginResult(pluginResult, callbackId);    
    }
    
    private void webViewSendPluginResult(LDJSPluginResult pluginResult, String _callbackId){
    	//通过回调函数返回
        String result = pluginResult.getMessage();
        String jsCall = "";
        if(Integer.parseInt(_callbackId) > 0){
        	jsCall = "javascript:mapp.execGlobalCallback("+_callbackId+",'"+result+"')";
        } else {
        	jsCall = "javascript:window."+_callbackId+"('"+result+"')";
        }
        webView.loadUrl(jsCall);
    }

    /**
     * Helper for success callbacks that just returns the Status.OK by default
     *
     * @param message           The message to add to the success result.
     */
    public void success(JSONObject message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }

    /**
     * Helper for success callbacks that just returns the Status.OK by default
     *
     * @param message           The message to add to the success result.
     */
    public void success(String message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }

    /**
     * Helper for success callbacks that just returns the Status.OK by default
     *
     * @param message           The message to add to the success result.
     */
    public void success(JSONArray message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }

    /**
     * Helper for success callbacks that just returns the Status.OK by default
     *
     * @param message           The message to add to the success result.
     */
    public void success(byte[] message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }
    
    /**
     * Helper for success callbacks that just returns the Status.OK by default
     *
     * @param message           The message to add to the success result.
     */
    public void success(int message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }

    /**
     * Helper for success callbacks that just returns the Status.OK by default
     */
    public void success() {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK));
    }

    /**
     * Helper for error callbacks that just returns the Status.ERROR by default
     *
     * @param message           The message to add to the error result.
     */
    public void error(JSONObject message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.ERROR, message));
    }

    /**
     * Helper for error callbacks that just returns the Status.ERROR by default
     *
     * @param message           The message to add to the error result.
     */
    public void error(String message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.ERROR, message));
    }

    /**
     * Helper for error callbacks that just returns the Status.ERROR by default
     *
     * @param message           The message to add to the error result.
     */
    public void error(int message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.ERROR, message));
    }
}
