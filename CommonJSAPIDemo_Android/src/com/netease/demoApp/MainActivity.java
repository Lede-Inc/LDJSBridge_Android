package com.netease.demoApp;

import com.netease.commonjsapidemo_android.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.netease.demoApp.plugins.LDPBaseWebViewActivity;

public class MainActivity extends Activity {
	public static String EXTRA_URL = "WebViewLoadUrl";
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    
    public void openWebViewActivity(View view){
    	Intent intent = new Intent(this, LDPBaseWebViewActivity.class);
    	intent.putExtra(EXTRA_URL, "http://10.232.4.186/LDJSBridge_JS/api.htm");
    	startActivity(intent);
    }
}
