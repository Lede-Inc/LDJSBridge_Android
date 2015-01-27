package com.netease.JSBridge;

import android.app.Activity;
import android.content.Context;


/**
 * LDJSActivityInterface
 * JSBridgeService执行过程中需要activity所在的上下文，调用JSBridgeService的activity需要实现如下接口
 * 如果插件实现过程中如果需要获取执行环境，根据需求在此接口中途爱那集
 * @author panghui
 *
 */
public interface LDJSActivityInterface {
	/**
	 * 获取调用BridgeService的Activity
	 * @return
	 */
    public abstract Activity getActivity();


    /**
     * 获取调用Bridge的程序上下文
     * @return
     */
    public abstract Context getContext();
}
