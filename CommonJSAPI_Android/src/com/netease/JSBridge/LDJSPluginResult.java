package com.netease.JSBridge;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Base64;

public class LDJSPluginResult {
    private final int status;
    private final int messageType;
    private boolean keepCallback = false;
    private String strMessage;
    private String encodedMessage;

    public LDJSPluginResult(Status status) {
        this(status, LDJSPluginResult.StatusMessages[status.ordinal()]);
    }

    public LDJSPluginResult(Status status, String message) {
        this.status = status.ordinal();
        this.messageType = message == null ? MESSAGE_TYPE_NULL : MESSAGE_TYPE_STRING;
        this.strMessage = message;
    }

    public LDJSPluginResult(Status status, JSONArray message) {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_JSON;
        encodedMessage = message.toString();
    }

    public LDJSPluginResult(Status status, JSONObject message) {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_JSON;
        encodedMessage = message.toString();
    }

    public LDJSPluginResult(Status status, int i) {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_NUMBER;
        this.encodedMessage = ""+i;
    }

    public LDJSPluginResult(Status status, float f) {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_NUMBER;
        this.encodedMessage = ""+f;
    }

    public LDJSPluginResult(Status status, boolean b) {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_BOOLEAN;
        this.encodedMessage = Boolean.toString(b);
    }

    public LDJSPluginResult(Status status, byte[] data) {
        this(status, data, false);
    }

    public LDJSPluginResult(Status status, byte[] data, boolean binaryString) {
        this.status = status.ordinal();
        this.messageType = binaryString ? MESSAGE_TYPE_BINARYSTRING : MESSAGE_TYPE_ARRAYBUFFER;
        this.encodedMessage = Base64.encodeToString(data, Base64.NO_WRAP);
    }
    
    public void setKeepCallback(boolean b) {
        this.keepCallback = b;
    }

    public int getStatus() {
        return status;
    }

    public int getMessageType() {
        return messageType;
    }

    public String getMessage() {
        if (encodedMessage == null) {
            encodedMessage = JSONObject.quote(strMessage);
        }
        return encodedMessage;
    }

    /**
     * If messageType == MESSAGE_TYPE_STRING, then returns the message string.
     * Otherwise, returns null.
     */
    public String getStrMessage() {
        return strMessage;
    }

    public boolean getKeepCallback() {
        return this.keepCallback;
    }

    // Use sendPluginResult instead of sendJavascript.
    public String getJSONString() {
        return "{\"status\":" + this.status + ",\"message\":" + this.getMessage() + ",\"keepCallback\":" + this.keepCallback + "}";
    }

    // Use sendPluginResult instead of sendJavascript.
    public String toCallbackString(String callbackId) {
        // If no result to be sent and keeping callback, then no need to sent back to JavaScript
        if ((status == LDJSPluginResult.Status.NO_RESULT.ordinal()) && keepCallback) {
        	return null;
        }

        // Check the success (OK, NO_RESULT & !KEEP_CALLBACK)
        if ((status == LDJSPluginResult.Status.OK.ordinal()) || (status == LDJSPluginResult.Status.NO_RESULT.ordinal())) {
            return toSuccessCallbackString(callbackId);
        }

        return toErrorCallbackString(callbackId);
    }

    // Use sendPluginResult instead of sendJavascript.
    public String toSuccessCallbackString(String callbackId) {
        return "cordova.callbackSuccess('"+callbackId+"',"+this.getJSONString()+");";
    }

    // Use sendPluginResult instead of sendJavascript.
    public String toErrorCallbackString(String callbackId) {
        return "cordova.callbackError('"+callbackId+"', " + this.getJSONString()+ ");";
    }

    public static final int MESSAGE_TYPE_STRING = 1;
    public static final int MESSAGE_TYPE_JSON = 2;
    public static final int MESSAGE_TYPE_NUMBER = 3;
    public static final int MESSAGE_TYPE_BOOLEAN = 4;
    public static final int MESSAGE_TYPE_NULL = 5;
    public static final int MESSAGE_TYPE_ARRAYBUFFER = 6;
    // Use BINARYSTRING when your string may contain null characters.
    // This is required to work around a bug in the platform :(.
    public static final int MESSAGE_TYPE_BINARYSTRING = 7;

    public static String[] StatusMessages = new String[] {
        "No result",
        "OK",
        "Class not found",
        "Illegal access",
        "Instantiation error",
        "Malformed url",
        "IO error",
        "Invalid action",
        "JSON error",
        "Error"
    };

    public enum Status {
        NO_RESULT,
        OK,
        CLASS_NOT_FOUND_EXCEPTION,
        ILLEGAL_ACCESS_EXCEPTION,
        INSTANTIATION_EXCEPTION,
        MALFORMED_URL_EXCEPTION,
        IO_EXCEPTION,
        INVALID_ACTION,
        JSON_EXCEPTION,
        ERROR
    }
}
