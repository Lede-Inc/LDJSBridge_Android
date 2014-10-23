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
import java.util.List;

public class LDJSParams {
    private List<String>	_arrParams;
    private HashMap<String, String> _dicParams;

    public LDJSParams(List<String> arrParams) {
    	this(arrParams, null);
    }
    
    public LDJSParams(HashMap<String,String> dicParams) {
    	this(null, dicParams);
    }
    
    public LDJSParams(List<String> arrParams, HashMap<String,String> dicParams) {
    	if(arrParams != null){
    		this._arrParams = arrParams;
    	}
    	
    	if(dicParams != null){
    		this._dicParams = dicParams;
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
    
    public String jsonParamForkey(String key){
    	return this.jsonParamForkey(key, "");
    }
    
    public String jsonParamForkey(String key, String defaultValue){
    	String param = _dicParams.get(key);
    	if(param == null || param.equalsIgnoreCase("")){
    		param = defaultValue;
    	}
    	return param;
    }
 }


