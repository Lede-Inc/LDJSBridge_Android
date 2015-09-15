# LDJSBridge_Android
===============

**LDJSBridge_Android**的核心目标是完成在Android客户端中**WAP页面和客户端（Native）的深度交互**。 


## 如何集成LDJSBridge_Android
-------------------

1. Android平台可以通过库依赖的方式引入LDJSBridge_Android的包（commonjsapi_android.jar）。
2. 如果需要查看源码，可以将LDJSBridge_Android工程中的“CommonJSAPI_Android”Library工程加入到你本地的android worlspace中,* 
3. 最后在你的项目工程中加入引用该库工程即可。




## LDJSBridge_Android的插件运行机制

本LDJSBridge_IOS是基于Phonegap的Cordova引擎的基础上简化而来, Android平台Webview和JS的交互方式共有三种:

1. ExposedJsApi：js直接调用java对象的方法；（同步）
2. 重载chromeClient的prompt 截获方案；(异步)
3. url截获+webview.loadUrl回调的方案；（异步）

为了和IOS保持一直的JSAPI，只能选用第三套方案；其实现原理和IOS类似（参考[IOS的实现原理](https://github.com/Lede-Inc/LDJSBridge_IOS/blob/master/README.md)）


## 如何开发基于LDJSBridge_Android的Native插件
-------------------------------------

本工程主要是提供LDJSBridge_Android的技术框架，方便各个客户端项目集成开发各自需要的JSAPI。一般各个项目根据产品和运营的WAP需求先制定JSAPI文档

### 定义某个模块插件

在Native部分，定义一个模块插件对应于创建一个插件类, 模块中的每个插件接口在通过传入的action区别。
集成LDJSBridge_Android框架之后，只需要继承框架中的插件基类LDJSPlugin，如下所示：


* 插件接口定义

```
public class LDPDevice extends LDJSPlugin {
	public static final String TAG = "Device";
	/**
 	 * Constructor.
 	 */
	public LDPDevice() {
	}
}
```


* LDJSPlugin 属性方法说明

```
	/***
	 * 自定义开发插件的基类，所以自定义的插件必须继承该类，并实现该类的方法
	 * @author panghui
	 *
	 */
	public class LDJSPlugin {
	  	public String id;
	  		
	  	//在插件初始化的时候，会初始化当前插件所属的webview和controller
		//供插件方法接口 返回处理结果
	    public WebView webView; 
		public LDJSActivityInterface activityInterface;
		
		//所有自定义插件需要重载此方法
		public boolean execute(String action, LDJSParams args, LDJSCallbackContext callbackContext) throws JSONException {
	      		return false;
	  	}
	  		
	}	
```

* 自定义插件接口实现

```
 @Override
public boolean execute(String action, LDJSParams args, LDJSCallbackContext callbackContext) throws JSONException {
   	if (action.equals("getDeviceInfo")) {
       	JSONObject r = new JSONObject();
       	r.put("uuid", LDPDevice.uuid);
       	r.put("version", this.getOSVersion());
       	r.put("platform", this.getPlatform());
       	r.put("model", this.getModel());
       	callbackContext.success(r);
   	}
   	else {
       	return false;
   	}
   	return true;
}
```

## 如何在WebView页面使用自定义的插件
---------------------------------

>
 在Android项目中，当展示WAP页面的时候会用到WebView组件，我们通过在WebView组件所在Activity中注册JSAPIServie服务，通过WebView指定的WebviewClient拦截Webview的URL进行处理。
 
 * 在Webview所在的Activity中初始化一个JSAPIService，并注册该WebView需要使用的插件

```
//注册插件服务
if(jsBridgeService == null){
	jsBridgeService = new LDJSService(_webview, this, "PluginConfig.json");
}
```

 PluginConfig.json文件主要是对JSAPI的插件进行配置，如下所示：
 
 update: 配置在线更新核心JS文件的url
 
 plugins：配置每个插件对应的JS调用名称和类名；exports如果不配置，则类中的方法则为JS调用函数；
 
```
	{
    "update": "",
    "plugins": [
        {
            "pluginname": "device",
            "pluginclass": "LDPDevice",
            "exports": [
                {
                    "showmethod": "getDeviceInfo",
                    "realmethod": "getDeviceInfo"
                }
            ]
        },
        {
            "pluginname": "app",
            "pluginclass": "LDPAppInfo",
            "exports": []
        },
        {
            "pluginname": "nav",
            "pluginclass": "LDPUINavCtrl",
            "exports": []
        },
        {
            "pluginname": "ui",
            "pluginclass": "LDPUIGlobalCtrl",
            "exports": []
        }
    ]
}
```
 
 * 通过WebviewDelegate拦截url请求，处理JSAPI中发送的jsbridge://请求
 
```
	if(_webview == null){
    		_webview = new WebView(LDPBaseWebViewActivity.this, null);
    		//设置允许webview和javascript交互
            _webview.getSettings().setJavaScriptEnabled(true);
            _webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

    		//绑定webviewclient
    		_webviewClient = new WebViewClient(){
    			public void onPageStarted(WebView view, String url, Bitmap favicon){
    				super.onPageStarted(view, url, favicon);
    				isWebviewStarted = true;
    			}

    			public void onPageFinished(WebView view, String url) {
    				super.onPageFinished(view, url);
    		        	//发送事件通知前端
    				if(isWebviewStarted){
    					//在page加载之后，加载核心JS，前端页面可以在document.ready函数中直接调用了；
        				jsBridgeService.onWebPageFinished();
    		        		jsBridgeService.readyWithEventName("LDJSBridgeServiceReady");
    				}
    				isWebviewStarted = false;
    			}

    			  @Override
    			  public boolean shouldOverrideUrlLoading(WebView view, String url) {
    				  	if(url.startsWith("about:")){
    				  		return true;
    				  	}
    					if(url.startsWith(LDJSService.LDJSBridgeScheme)){
    						//处理JSBridge特定的Scheme
							jsBridgeService.handleURLFromWebview(url);
    						return true;
    					}

    					return false;
    			  }
    		};

    		_webview.setWebViewClient(_webviewClient);
    		//绑定chromeClient
    		_webviewChromeClient = new WebChromeClient(){
    			@Override
    		    public boolean onJsAlert(WebView view, String url, String message,
    		            JsResult result) {
    		        return super.onJsAlert(view, url, message, result);
    			}
    		};
    		_webview.setWebChromeClient(_webviewChromeClient);
    	}

```

## 定义NavigationController导航的Wap功能模块
-------------------------------------------

在手机qq里可以看到很多独立的基于WAP页面的功能模块，其实基于JSBridge的JSAPI最大的用处是以这种方式呈现。

* 目前在demo工程中已经初步完成了Device、App、UI导航部分的示例（参看[LDPBaseWebViewActivity.java文件](CommonJSAPIDemo_Android/src/com/netease/demoApp/plugins/LDPBaseWebViewActivity.java)），客户端可以在此基础上根据项目需求进行完善开发：		

## 技术支持
-------------------


>
to be continued ....



庞辉, 电商技术中心，popo：__huipang@corp.netease.com__