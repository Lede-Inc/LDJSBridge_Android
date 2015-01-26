package com.netease.JSBridge;

import java.io.UnsupportedEncodingException;
import java.net.URI;
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


    /**
     * 从webView截取URL并执行
     * @param urlstr
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    public void excuteCommandsFromUrl(String urlstr) throws JSONException, UnsupportedEncodingException {
    	URI uri = URI.create(urlstr);
    	String host = uri.getHost();
    	String pathStr = uri.getPath();
    	String query = uri.getQuery();
    	String fragement = uri.getFragment();

    	//获取URL回调函数的index
    	String callIndex = "";
    	if(fragement != null && fragement.length() > 0 &&
    		Integer.parseInt(fragement) > 0){
    		callIndex = fragement;
    	}

    	//获取调用插件名
    	String pluginName = "";
    	if(host != null && host.length() > 0){
    		pluginName = java.net.URLDecoder.decode(host, "UTF-8");
    	}

    	String methodShowName = "";
    	if(pathStr != null && !pathStr.equalsIgnoreCase("")){
    		String[] paths = pathStr.split("/");
    		if(paths != null && paths.length >= 2){
    			methodShowName = java.net.URLDecoder.decode(paths[1], "UTF-8");
    		}
    	}


        //获取通过query对象传进来的参数
        List<String> arr_params = null;
        if(query != null && query.length() > 0){
            //分割&参数
            String arr_andMark[] = query.split("&");
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
        }//


        //遍历参数数组，检查JSON参数读取
        HashMap<String,Object> dic_params = null;
        if(arr_params != null && arr_params.size() > 0){
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
                            }

                            dic_params.put(key, tmp_dic.get(key));
                        }

                        //处理完删除该参数
                        arr_params.remove(tmpParam);
                    }
            	}catch(Exception e){
            		Log.d(LOG_TAG, "param is not json object");
            	}
            }
        }//if


        //用参数直接开始执行
        try {
        	String result = executePending(pluginName, methodShowName, callIndex, arr_params, dic_params);
        	if(result != null){
        		Log.i(LOG_TAG, "the plugin" +pluginName +" execute result: " + result);
        	}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
    }


    /**
     * 执行URL传入的命令
     * @param pluginName
     * @param showMethodName
     * @param callbackId
     * @param arr_params
     * @param dic_params
     * @return
     * @throws JSONException
     * @throws IllegalAccessException
     */
    private String executePending(String pluginName, String showMethodName, String callbackId, List<String> arr_params, HashMap<String,Object>dic_params) throws JSONException, IllegalAccessException {
       if(pluginName == null || pluginName.equalsIgnoreCase("")) {
    	   return "@PluginName null";
       }

       if(showMethodName == null || showMethodName.equalsIgnoreCase("")){
    	   return "@plugin showMethod null";
       }

        try {
            // Tell the resourceApi what thread the JS is running on.
        	LDJSPlugin plugin = _jsService.getPluginInstance(pluginName);
            if (plugin == null) {
                Log.d(LOG_TAG, "exec() call to unknown plugin: " + pluginName);
                return "@pluginName unknown";
            }

            LDJSCallbackContext callbackContext = new LDJSCallbackContext(callbackId, _webView);
            try {
                long pluginStartTime = System.currentTimeMillis();
                LDJSParams args = new LDJSParams(arr_params, dic_params);

                //获取真实的
                String realMethodName = _jsService.realForShowMethod(showMethodName);
                boolean wasValidAction = plugin.execute(realMethodName, args, callbackContext);
                long duration = System.currentTimeMillis() - pluginStartTime;

                if (duration > SLOW_EXEC_WARNING_THRESHOLD) {
                    Log.w(LOG_TAG, "THREAD WARNING: exec() call to " + pluginName + "." + realMethodName + " blocked the main thread for " + duration + "ms. Plugin should use CordovaInterface.getThreadPool().");
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

            String ret = null;
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        } finally {
        }
    }
}
