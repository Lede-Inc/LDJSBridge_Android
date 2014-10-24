package com.netease.JSBridge;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.os.Debug;
import android.util.Log;
import android.webkit.WebView;

import com.netease.JSBridge.LDJSService;
import com.netease.JSBridge.LDJSParams;
import com.netease.JSBridge.LDJSPlugin;





/**
 * 完成对url参数得获取，存入消息队列中，从消息队列中取出参数调用对应得插件执行
 * 
 */
@SuppressLint("DefaultLocale") 
public class LDJSCommandQueue {
    private static final String LOG_TAG = "LDJSCommandQueue";
    private static final int SLOW_EXEC_WARNING_THRESHOLD = Debug.isDebuggerConnected() ? 60 : 16;
    
    private LDJSService _jsService;
    private WebView _webView;
    
    public LDJSCommandQueue(LDJSService jsService, WebView theWebView) {
        this._jsService = jsService;
        this._webView = theWebView;
    }
    
    
    
    /**从识别url中加入参数
     * urlstr构成：
     * 1.调用普通接口：
     *   mapp.invoke("ns","method"),
     *   jsbridge://device/getNetworkInfo#7
     *
     * 2.调用有返回值的接口：
     *   mapp.invoke("ns","method",function(){}),
     *
     * 3.调用有异步回调的接口：
     *   mapp.invoke("ns","method",{})
     *   jsbridge://device/setScreenStatus?p={"status":"1","callback":"__MQQ_CALLBACK_12"}#13
     *
     * 4.有多个参数的调用：
     */
    public String fetchCommandsFromUrl(String urlstr) throws JSONException, UnsupportedEncodingException {
        //去掉scheme
    	String str[] = urlstr.split("://", -1);
    	if(str != null && str.length == 2){
    		urlstr = str[1];
    	} else {
    		return "";//url not valid 
    	}
        
        //去掉＃
        String arr_headAndfoot[] = urlstr.split("#", -1);
        String callIndex = "";
        if(arr_headAndfoot != null && arr_headAndfoot.length == 2) {
            callIndex = arr_headAndfoot[arr_headAndfoot.length-1];
        } else {
        	return ""; //url not valid
        }
        
        
        String mcontent = arr_headAndfoot[0];
        String arr_qmark[] = mcontent.split("[?]", -1);
        if(arr_qmark == null || arr_qmark.length <= 0 ){
        	return "";
        }
        
        //切割类和方法, class和method在js中作了urlencode
        String str_classAndmethod = arr_qmark[0];
        String arr_classAndmethod[] = str_classAndmethod.split("/");
        String className = java.net.URLDecoder.decode(arr_classAndmethod[0], "UTF-8");
        String methodName = java.net.URLDecoder.decode(arr_classAndmethod[1], "UTF-8");
        
        
        //如果url有参数，切割传入的参数
        List<String> arr_params = null; 
        HashMap<String,Object> dic_params = null;
        if(arr_qmark.length == 2)
        {
            String str_params = arr_qmark[1];
            Log.d(LOG_TAG, "print>>>>" + str_params);
            
            //分割&参数
            String arr_andMark[] = str_params.split("&"); 
            if(arr_andMark.length > 0){
            	arr_params = new ArrayList<String>();
                for(String str_param : arr_andMark){
                    //分割p参数,每一个参数都进行了urlDecode
                    String arr_qualMark[] = str_param.split("=");
                    if(arr_qualMark.length == 2){
                        String value_param = java.net.URLDecoder.decode(arr_qualMark[1], "UTF-8");
                        arr_params.add(value_param);
                    }//if not equal 2 is not valid
                }//for
            }
            
            
            //遍历参数数组，检查json参数，作二级分析
            if(arr_params.size() > 0){
            	Iterator<String> iterator = arr_params.iterator();
                while (iterator.hasNext()) {
                	String tmpParam = iterator.next();
                	try{
                    	JSONObject tmp_dic = new JSONObject(tmpParam);
                        if(tmp_dic != null){
                            if(tmp_dic.length() > 0 && dic_params == null){
                            	dic_params = new HashMap<String,Object>();
                            }
                            
                            
                            Iterator<?> it = tmp_dic.keys();
                            //拷贝key和对应的value
                            while(it.hasNext()){
                            	String key = (String)it.next();
                                if(key.toLowerCase().equalsIgnoreCase("callback")){
                                    callIndex = tmp_dic.getString(key).toString();
                                } else {
                                	dic_params.put(key, tmp_dic.get(key));
                                }
                                
                            }
                            
                            //处理完删除该参数
                            arr_params.remove(tmpParam);
                        } 
                	}catch(Exception e){
                		Log.d(LOG_TAG, "param is not json object");
                		//不是json对象
                	}
                }
            }
            
        }//if
        
        
        //用参数直接开始执行
        try {
			String r = jsExec(className, methodName, callIndex, arr_params, dic_params);
			return r == null ? "" : r;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return "";
    }   
    
    
    public String jsExec(String service, String action, String callbackId, List<String> arr_params, HashMap<String,Object>dic_params) throws JSONException, IllegalAccessException {
        // If the arguments weren't received, send a message back to JS.  It will switch bridge modes and try again.  See CB-2666.
        // We send a message meant specifically for this case.  It starts with "@" so no other message can be encoded into the same string.
       if(service == null || service.equalsIgnoreCase("")) {
    	   return "@Service null";
       }
       
       if(action == null || action.equalsIgnoreCase("")){
    	   return "@Action null";
       }

        try {
            // Tell the resourceApi what thread the JS is running on.
            pluginExec(service, action, callbackId, arr_params, dic_params);
            String ret = null;
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        } finally {
        }
    }
    
    
    /**
     * Receives a request for execution and fulfills it by finding the appropriate
     * Java class and calling it's execute method.
     *
     * Plugin.exec can be used either synchronously or async. In either case, a JSON encoded
     * string is returned that will indicate if any errors have occurred when trying to find
     * or execute the class denoted by the clazz argument.
     *
     * @param service       String containing the service to run
     * @param action        String containing the action that the class is supposed to perform. This is
     *                      passed to the plugin execute method and it is up to the plugin developer
     *                      how to deal with it.
     * @param callbackId    String containing the id of the callback that is execute in JavaScript if
     *                      this is an async plugin call.
     * @param rawArgs       An Array literal string containing any arguments needed in the
     *                      plugin execute method.
     */
    public void pluginExec(final String service, final String action, final String callbackId, final List<String> arr_params, final HashMap<String,Object>dic_params) {
        LDJSPlugin plugin = _jsService.getCommandInstance(service);
        if (plugin == null) {
            Log.d(LOG_TAG, "exec() call to unknown plugin: " + service);
            return;
        }
        
        LDJSCallbackContext callbackContext = new LDJSCallbackContext(callbackId, this._webView);
        try {
            long pluginStartTime = System.currentTimeMillis();
            LDJSParams args = new LDJSParams(arr_params, dic_params);
            args.printParams();
            boolean wasValidAction = plugin.execute(action, args, callbackContext);
            long duration = System.currentTimeMillis() - pluginStartTime;

            if (duration > SLOW_EXEC_WARNING_THRESHOLD) {
                Log.w(LOG_TAG, "THREAD WARNING: exec() call to " + service + "." + action + " blocked the main thread for " + duration + "ms. Plugin should use CordovaInterface.getThreadPool().");
            }
            if (!wasValidAction) {
                LDJSPluginResult cr = new LDJSPluginResult(LDJSPluginResult.Status.INVALID_ACTION);
                callbackContext.sendPluginResult(cr);
            }
        } catch (JSONException e) {
            LDJSPluginResult cr = new LDJSPluginResult(LDJSPluginResult.Status.JSON_EXCEPTION);
            callbackContext.sendPluginResult(cr);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Uncaught exception from plugin", e);
            callbackContext.error(e.getMessage());
        }
    }
}
