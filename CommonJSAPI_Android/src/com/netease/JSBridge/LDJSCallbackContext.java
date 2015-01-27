package com.netease.JSBridge;

import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Log;
import android.webkit.WebView;
import com.netease.JSBridge.LDJSPluginResult;

/**
 * LDJSCallbackContext,用于记录URL调用的回调环境
 * 包括回调的webview和回调函数的index或回调函数名
 * 此外还负责返回结果的封装
 * @author panghui
 *
 */
public class LDJSCallbackContext {
    private static final String LOG_TAG = "LDJSCallbackContext";

    private String callbackId; //回调函数ID
    private WebView webView; //当前插件的执行webView

    public LDJSCallbackContext(String callbackId, WebView webView) {
        this.callbackId = callbackId;
        this.webView = webView;
    }


    /**
     * 将封装好的pluginResult通过回调函数执行
     * @param pluginResult
     */
    public void sendPluginResult(LDJSPluginResult pluginResult) {
        webViewSendPluginResult(pluginResult, callbackId);
    }


    /**
     * 将pluginResult转换成json字符串，通过callbackId调用回调函数执行
     * @param pluginResult
     * @param theCallbackId
     */
    private void webViewSendPluginResult(LDJSPluginResult pluginResult, String theCallbackId){
    	if(pluginResult.getStatus() == 0 || pluginResult.getStatus() == 1){
        	//通过回调函数返回
            String result = pluginResult.getMessage();
            String jsCall = "";
            if(theCallbackId.matches("[\\d]+") && Integer.parseInt(theCallbackId) > 0){
            	jsCall = "javascript:mapp.execGlobalCallback("+theCallbackId+",'"+result+"')";
            } else {
            	jsCall = "javascript:window."+theCallbackId+"('"+result+"')";
            }

            webView.loadUrl(jsCall);
    	} else {
    		webView.loadUrl("javascript:alert('"+pluginResult.getMessage()+"')");
    	}

    	Log.i(LOG_TAG, "webview finish excute command by callbackID" + theCallbackId);
    }


    /**
     * 执行成功，封装JSONObject
     * @param message
     */
    public void success(JSONObject message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }


    /**
     * 执行成功，封装String
     * @param message
     */
    public void success(String message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }


    /**
     * 执行成功，封装JSONArray
     * @param message
     */
    public void success(JSONArray message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }


    /**
     * 执行成功，封装Byte
     * @param message
     */
    public void success(byte[] message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }


    /**
     * 执行成功，封装Int
     * @param message
     */
    public void success(int message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK, message));
    }


    /**
     * 执行成功，只封装执行结果状态
     */
    public void success() {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.OK));
    }


    /**
     * 执行失败，封装JSONObject
     * @param message
     */
    public void error(JSONObject message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.ERROR, message));
    }


    /**
     * 执行失败，封装String
     * @param message
     */
    public void error(String message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.ERROR, message));
    }


    /**
     * 执行失败，封装int
     * @param message
     */
    public void error(int message) {
        sendPluginResult(new LDJSPluginResult(LDJSPluginResult.Status.ERROR, message));
    }
}
