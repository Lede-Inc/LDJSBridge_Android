package com.netease.JSBridge;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.webkit.WebView;

import com.netease.JSBridge.LDJSPlugin;
import com.netease.JSBridge.LDJSCommandQueue;


/**
 * LDJSService is exposed to webview to handle the url
 *
 */
@SuppressLint("DefaultLocale") 
public class LDJSService {
	private static final String LOG_TAG = "LDJSService";
	private String _userAgent = "";
	private boolean initialized = false;
	
    //pluginsMap 用于注册插件， pluginObject用户存储实例化的插件对象
    private final WebView webView;
    private HashMap<String, String> pluginsMap = new HashMap<String, String>();
    private HashMap<String, LDJSPlugin> pluginObjects = new HashMap<String, LDJSPlugin>();
    private LDJSCommandQueue _commandQueue = null;
    private LDJSActivityInterface activityInterface;
       
    
    //需要为webview注册jsService
    public LDJSService(WebView theWebView, LDJSActivityInterface activityInterface) {
    	this.webView = theWebView;
    	this.activityInterface = activityInterface;
    	if(_commandQueue == null){
    		_commandQueue = new LDJSCommandQueue(this, this.webView);
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
     * @func 处理从webview发过来的url调用请求
     * @param urlstring		api发过来的url请求
     * @throws JSONException 
     * @throws UnsupportedEncodingException 
     */
    public void handleURLFromWebview(String urlstring) throws UnsupportedEncodingException, JSONException{
    	if(this.webView != null && urlstring.startsWith("jsbridge://")){
    		//将url进行解析执行 to add 
    		_commandQueue.fetchCommandsFromUrl(urlstring);
    	}
    }
    
    /**
     * @func 用户批量注册自定义的plugins
     * @param pluginDic		 用户自定义的插件列表，key为自定义插件名称，value为具体的插件类名
     */
     public void registerPlugins(HashMap<String, String> pluginDic){
    	 if(this.pluginsMap == null){
    		 this.pluginsMap = new HashMap<String, String>();
    	 }
    	 
    	 //将plugin注册到当前service
    	 for(String pluginName :pluginDic.values()){
    		 String pluginClass = pluginDic.get(pluginName);
    		 pluginsMap.put(pluginName, pluginClass);
    	 }
     }
     
     /**
      *@func 用户单个注册自定义的plugin
      *@param pluginName 自定义插件名称(跟js的模块相对应)，
      *@param className  插件类名
      */
    public void registerPlugin(String pluginName, String className){
    	if(pluginName == null || pluginName.equalsIgnoreCase("")) return;
    	if(className == null || className.equalsIgnoreCase("")) return;
    	
    	if(this.pluginsMap == null){
    		this.pluginsMap = new HashMap<String, String>();
    	}
    	
    	pluginsMap.put(pluginName, className);
    }
    
    
    /**
     *@func 用户注销所有自定义插件
     */
    public boolean unRegisterAllPlugins(){
        //删除所有生成的对象
    	if(this.pluginObjects != null){
    		this.pluginObjects.clear();
    	}
        
        //删除所有注册的插件
    	if(this.pluginsMap != null){
    		this.pluginsMap.clear();
    	}
    	
        return true;
    }
    
    
    /**
     *@func 用户单个注销自定义的plugin
     *@param pluginName 自定义插件名称(跟js的模块相对应)，
     *@param className  插件类名
     */
    public void unRegisterPlugin(String pluginName){
        if(pluginName == null || pluginName.equalsIgnoreCase("")) return;
        
        if(this.pluginsMap != null){
            String className = this.pluginsMap.get(pluginName.toLowerCase());
            //如果插件已注册,先删除实例化的对象，再注销插件；
            if(className != null && removePluginwithClassName(className)){
                this.pluginsMap.remove(pluginName.toLowerCase());
            }//if
        }//if
    }
    
    
    
    /**
     *@func 以pluginName返回用户自定义插件类的对象
     *@param pluginName 插件自定义名称，不一定时类名称
     */
	public LDJSPlugin getCommandInstance(final String pluginName){
        String className = this.pluginsMap.get(pluginName.toLowerCase());
        
        //插件没有注册
        if (className == null) {
            return null;
        }
        
        //插件已注册，返回插件实例
        LDJSPlugin obj = this.pluginObjects.get(className);
        if (obj == null) {
            obj = instantiatePlugin(className);
            if (obj != null) {
                addPlugin(obj,className);
            } else {
            	Log.e(LOG_TAG, "LDJSPlugin class "+className+" (pluginName: "+pluginName+") does not exist.", new Throwable());
            }
        }
        return obj;
    }
    
    
    /**
     *@func 生成自定义插件的实例对象
     *@param className 自定义插件的类名称
     */
    private boolean addPlugin(LDJSPlugin plugin, String className){
        //为plugin设置当前webview 和activity的接口
    	plugin.activityInterface = activityInterface;
    	plugin.webView = this.webView;
      
    	this.pluginObjects.put(className, plugin);
    	plugin.pluginInitialize();
        
        return true;
    }


    /**
     *@func 注销某个自定义插件
     *@param className 自定义插件的类名称
     */
    private boolean removePluginwithClassName(String className){
        LDJSPlugin obj = this.pluginObjects.get(className);
        if (obj != null) {
        	this.pluginObjects.remove(className);
            obj.webView = null;
            obj.activityInterface = null;
        }
        return true;
    }
    
    
    
    /**
     * Create a plugin based on class name.
     */
    private LDJSPlugin instantiatePlugin(String className) {
        LDJSPlugin ret = null;
        try {
            Class<?> c = null;
            if ((className != null) && !("".equals(className))) {
                c = Class.forName(className);
            }
            if (c != null & LDJSPlugin.class.isAssignableFrom(c)) {
                ret = (LDJSPlugin) c.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error adding plugin " + className + ".");
        }
        return ret;
    }
    

	public boolean isInitialized() {
		return initialized;
	}
	

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}
    

   
}
