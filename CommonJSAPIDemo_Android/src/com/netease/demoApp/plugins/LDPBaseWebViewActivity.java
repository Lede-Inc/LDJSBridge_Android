package com.netease.demoApp.plugins;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import com.netease.JSBridge.LDJSService;
import com.netease.JSBridge.LDJSActivityInterface;
import com.netease.JSBridge.LDJSLOG;

import com.netease.demoApp.MainActivity;

/**
 * This class is the main Android WebView activity that represents the
 * web function module. It should be extended by the user to load the specific
 * online page that contains the function module.
 *
 */
public class LDPBaseWebViewActivity extends Activity implements LDJSActivityInterface {
    public static String TAG = "LDPBaseWebViewActivity";

    //the showURL of WebView
    public String url;
    protected LDJSService jsBridgeService;

    // The webview for theActivity
    protected LinearLayout root;
    protected WebView _webview;
    //监控iframe跳转链接命令
    protected WebViewClient _webviewClient;
    //用于Webview loadUrl执行JavaScript
    protected WebChromeClient _webviewChromeClient;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LDJSLOG.d(TAG, "LDPBaseWebViewActivity.onCreate()");
        super.onCreate(savedInstanceState);

      //从Intent中取得messge
        Intent intent = getIntent();
        this.url = intent.getStringExtra(MainActivity.EXTRA_URL);

        initActivity();
    }



    /**
     * 初始化Activity,打开网页，注册插件服务
     */
    public void initActivity() {
        //创建webview和显示view
    	createGapView();
    	createViews();

    	//注册插件服务
    	if(jsBridgeService == null){
    		jsBridgeService = new LDJSService(_webview, this, "PluginConfig.json");
    	}

    	//加载请求
    	if(this.url != null && !this.url.equalsIgnoreCase("")){
    		_webview.loadUrl(this.url);
    	}
    }

    /**
     * 初始化webview，如果需要调用JSAPI，必须为Webview注册WebViewClient和WebChromeClient
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void createGapView(){
    	if(_webview == null){
    		_webview = new WebView(LDPBaseWebViewActivity.this, null);

    		//设置允许webview和javascript交互
            _webview.getSettings().setJavaScriptEnabled(true);
            _webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

    		//绑定webviewclient
    		_webviewClient = new WebViewClient(){
 		        @Override
 		        public void onPageStarted(WebView view, String url, Bitmap favicon){
 		        	//在page加载之前，加载核心JS，前端页面可以在document.ready函数中直接调用了；
 		        	jsBridgeService.onWebPageFinished();
 		        }

    			  @Override
    			  public boolean shouldOverrideUrlLoading(WebView view, String url) {
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
    }



    /**
     * 初始化Acitivity view布局
     */
    @SuppressWarnings("deprecation")
    protected void createViews() {
        // This builds the view.  We could probably get away with NOT having a LinearLayout, but I like having a bucket!
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        root = new LinearLayoutSoftKeyboardDetect(this, width, height);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 0.0F));

        _webview.setId(100);
        _webview.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1.0F));

        // Add web view but make it invisible while loading URL
        _webview.setVisibility(View.VISIBLE);
        _webview.setBackgroundColor(Color.YELLOW);

        // need to remove appView from any existing parent before invoking root.addView(appView)
        ViewParent parent = _webview.getParent();
        if ((parent != null) && (parent != root)) {
            LDJSLOG.d(TAG, "removing appView from existing parent");
            ViewGroup parentGroup = (ViewGroup) parent;
            parentGroup.removeView(_webview);
        }
        root.addView((View) _webview);
        setContentView(root);

        root.setBackgroundColor(Color.WHITE);
    }



    /**
     * Load the url into the webview.
     */
    public void loadUrl(String url) {
        if (_webview != null) {
        	_webview.loadUrl(url);
        }
    }



    /**
     * Get the Android activity with override the activityInterface
     */
    @Override
    public Activity getActivity() {
        return this;
    }


	@Override
	public Context getContext() {
		// TODO Auto-generated method stub
		return this.getApplicationContext();
	}
}
