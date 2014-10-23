package com.netease.JSBridge;

import java.util.concurrent.ExecutorService;
import android.app.Activity;
import android.content.Intent;

import com.netease.JSBridge.LDJSPlugin; 


/**
 * The Activity interface that is implemented by JSServiceActivity.
 * It is used to isolate plugin development, and remove dependency on entire JSService library.
 */
public interface LDJSActivityInterface {

    /**
     * Launch an activity for which you would like a result when it finished. When this activity exits,
     * your onActivityResult() method will be called.
     *
     * @param command     The command object
     * @param intent      The intent to start
     * @param requestCode   The request code that is passed to callback to identify the activity
     */
    abstract public void startActivityForResult(LDJSPlugin command, Intent intent, int requestCode);

    /**
     * Set the plugin to be called when a sub-activity exits.
     *
     * @param plugin      The plugin on which onActivityResult is to be called
     */
    abstract public void setActivityResultCallback(LDJSPlugin plugin);

    /**
     * Get the Android activity.
     *
     * @return the Activity
     */
    public abstract Activity getActivity();
    

    /**
     * Called when a message is sent to plugin.
     *
     * @param id            The message id
     * @param data          The message data
     * @return              Object or null
     */
    public Object onMessage(String id, Object data);
    
    /**
     * Returns a shared thread pool that can be used for background tasks.
     */
    public ExecutorService getThreadPool();
}
