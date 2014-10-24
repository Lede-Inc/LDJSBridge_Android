/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.netease.JSBridge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

public class LDJSParams {
    private List<String>	_arrParams;
    private HashMap<String, Object> _dicParams;

    public LDJSParams(List<String> arrParams) {
    	this(arrParams, null);
    }
    
    public LDJSParams(HashMap<String,Object> dicParams) {
    	this(null, dicParams);
    }
    
    public LDJSParams(List<String> arrParams, HashMap<String,Object> dicParams) {
    	if(arrParams != null){
    		this._arrParams = arrParams;
    	}
    	
    	if(dicParams != null){
    		this._dicParams = dicParams;
    	}
    }

    public void printParams(){
    	if(this._arrParams != null) {
    		for(int i = 0; i < this._arrParams.size(); i++){
    			Log.d("LDJSParams", "index:" + i +"\t value:" + this._arrParams.get(i));
    		}
    	}
    	
    	if(this._dicParams != null){
    		Iterator<?> iter = this._dicParams.keySet().iterator();  
    		while (iter.hasNext()) {  
    		    String key = (String)iter.next();  
    		    Object val = (Object)this._dicParams.get(key);  
    		    Log.d("LDJSParams", "key:" + key +"\t value:" + val.toString());
    		} 
    	}
    }
    
    // Pass through the basics to the base args.
    public String get(int index) {
        return this.get(index,"");
    }
    
    public String get(int index, String defaultValue){
    	String param = _arrParams.get(index);
    	if(param == null || param.equalsIgnoreCase("")){
    		param = defaultValue;
    	}
    	return param;
    }
    
    public Object jsonParamForkey(String key){
    	return this.jsonParamForkey(key, null);
    }
    
    public Object jsonParamForkey(String key, Object defaultValue){
    	Object param = _dicParams.get(key);
    	if(param == null){
    		param = defaultValue;
    	}
    	return param;
    }
 }


