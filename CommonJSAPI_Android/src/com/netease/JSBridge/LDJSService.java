package com.netease.JSBridge;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.webkit.WebView;
import com.netease.JSBridge.LDJSCommandQueue;
import com.netease.JSBridge.LDJSPluginManager;


/**
 * LDJSService is exposed to webview to handle the url
 *
 */
@SuppressLint("DefaultLocale")
public class LDJSService {
	private static final String LOG_TAG = "LDJSService";
	private String _userAgent = "";
	private boolean loadCodeBridgeJS = false;
	private boolean initialized = false;

    //pluginsMap 用于注册插件， pluginObject用户存储实例化的插件对象
    private final WebView webView;
    private LDJSCommandQueue commandQueue = null;
    private LDJSPluginManager pluginManager = null;
    private LDJSActivityInterface activityInterface;


    //需要为webview注册jsService
    public LDJSService(WebView theWebView, LDJSActivityInterface activityInterface, String configFile) {
    	this.webView = theWebView;
    	this.activityInterface = activityInterface;

    	if(pluginManager == null){
    		pluginManager = new LDJSPluginManager(configFile, this.activityInterface.getContext(), this.activityInterface, this.webView);
    	}

    	if(commandQueue == null){
    		commandQueue = new LDJSCommandQueue(this, this.webView);
    	}

    	//设置WebView的UserAgent的值
    	if(this.webView != null){
	    	if(_userAgent.equalsIgnoreCase("")){
	    		_userAgent = this.webView.getSettings().getUserAgentString();
	    	}

	    	// 获取packagemanager的实例, getPackageName()是你当前类的包名，0代表是获取版本信息
	        PackageManager packageManager = this.activityInterface.getActivity().getPackageManager();
	        String packageName = this.activityInterface.getActivity().getPackageName();
	        try{
	        	PackageInfo packInfo = packageManager.getPackageInfo(packageName,0);
	        	String version = packInfo.versionName;
		    	String customUserAgent = _userAgent + " _MAPP_/" + version;
		    	this.webView.getSettings().setUserAgentString(customUserAgent);
	        } catch(Exception e){
	        	Log.e(LOG_TAG, "Cant fand the app Version");
	        }
    	}

    	this.setInitialized(true);
	}


    /**
     * 在webviewFinished之后加载核心框架JS
     */
    public void onWebPageFinished(){
    	if(!loadCodeBridgeJS){
			String coreBridgeJsCodeStr = pluginManager.localCoreBridgeJSCode();
			if(webView != null){
				// 多行注释
		        String multiComment = "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/";
		        // 单行注释
		        String singleComment = "//[^\r\n]*+";
				Pattern p = Pattern.compile(multiComment + "|" + singleComment + "|" + "\t|\r|\n");
	            Matcher m = p.matcher(coreBridgeJsCodeStr);
	            coreBridgeJsCodeStr = m.replaceAll("");
				coreBridgeJsCodeStr = "javascript:function onCoreBridgeJS(){"+ coreBridgeJsCodeStr.toString() +"}; onCoreBridgeJS();";
				webView.loadUrl(coreBridgeJsCodeStr);
				loadCodeBridgeJS = true;
			}
		}
    }


    /**
     * @func 处理从webview发过来的url调用请求
     * @param urlstring		api发过来的url请求
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    public void handleURLFromWebview(String urlstring) throws UnsupportedEncodingException, JSONException{
    	if(this.webView != null && urlstring.startsWith("ldjsbridge://")){
    		//将url进行解析执行 to add
    		commandQueue.excuteCommandsFromUrl(urlstring);
    	}
    }


    /**
     * 根据PluginName从插件管理器种获取插件实例
     * @param pluginName
     * @return
     */
    public LDJSPlugin getPluginInstance(String pluginName){
    	return pluginManager.getPluginInstance(pluginName);
    }


    /***
     * 根据对外showmethod获取真实的ActionMethod Name
     * @param showMethod
     * @return
     */
    public String realForShowMethod(String showMethod){
    	return pluginManager.realForShowAction(showMethod);
    }



	public boolean isInitialized() {
		return initialized;
	}


	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}



}
