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
 * LDJSBridgeService 提供wap页面和本地插件的连接枢纽
 * @author panghui
 *
 */
@SuppressLint("DefaultLocale")
public class LDJSService {
	public static final String LDJSBridgeScheme = "ldjsbridge";

	private static final String LOG_TAG = "LDJSService";
	private String _userAgent = ""; //给webview添加UserAgent信息，用于js进行平台和版本判断
	private boolean loadCodeBridgeJS = false; //是否第一次加载完成
	private boolean initialized = false;

    //pluginsMap 用于注册插件， pluginObject用户存储实例化的插件对象
    private final WebView webView;
    private LDJSCommandQueue commandQueue = null; //用于解析url命令
    private LDJSPluginManager pluginManager = null; //插件管理器
    private LDJSActivityInterface activityInterface;


    /**
     * 初始化LDJSService，完成插件管理器、url解析器的初始化，并写入当前应用版本到urerAgent
     * @param theWebView
     * @param activityInterface
     * @param configFile
     */
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
     * 注意：在anroid平台通过loadUrl执行JS，js字符串种不能有注释，js代码中如果有"//"，写成 '/'+'/',不然去注释过程有问题
     * 自执行的JS代码，一般将器封装到一个function中，然后再调用这个封装function
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
    public void handleURLFromWebview(String urlstring){
    	if(this.webView != null && urlstring.startsWith(LDJSBridgeScheme)){
    		//执行url传过来的bridge命令
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
    	return pluginManager.realForShowMethod(showMethod);
    }



	public boolean isInitialized() {
		return initialized;
	}


	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}
}
