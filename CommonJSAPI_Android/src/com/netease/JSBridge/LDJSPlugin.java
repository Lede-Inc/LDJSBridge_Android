package com.netease.JSBridge;

import org.json.JSONException;
import android.webkit.WebView;

import com.netease.JSBridge.LDJSActivityInterface;
import com.netease.JSBridge.LDJSParams;
import com.netease.JSBridge.LDJSCallbackContext;


/***
 * 自定义开发插件的基类，所以自定义的插件必须继承该类，并实现该类的方法
 * @author panghui
 *
 */
public class LDJSPlugin {
    public WebView webView; //插件所在的webview
    public LDJSActivityInterface activityInterface; //插件所在的activity

    /**
     * 通过插件管理器实例化插件的时候调用，其他地方不要调用
     * @param activityInterface
     * @param theWebView
     */
    public final void privateInitialize(LDJSActivityInterface activityInterface, WebView theWebView) {
        assert this.activityInterface == null;
        this.activityInterface = activityInterface;
        this.webView = theWebView;
        pluginInitialize();
    }


    /**
     * 如果需要在插件初始化的时候完成自定义action，可以重载该函数
     * 只有继承类可以重载和调用该函数
     */
    protected void pluginInitialize() {
    }


    /**
     * 根据URLcommand执行插件方法
     *
     * 如果在Webview线程中调用：
     *     activityInterface.getThreadPool().execute(runnable);
     *
     * 如果需要在UI线程中执行：
     *     activityInterface.getActivity().runOnUiThread(runnable);
     *
     * @param realMethod      需要执行的方法
     * @param args            URL传入的参数
     * @param callbackContext URL的回调环境
     * @return                Whether the action was valid.
     */
    public boolean execute(String realMethod, LDJSParams args, LDJSCallbackContext callbackContext) throws JSONException {
        return false;
    }
}
