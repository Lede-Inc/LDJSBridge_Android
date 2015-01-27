package com.netease.JSBridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import com.netease.JSBridge.LDJSPlugin;

/***
 * 本地插件管理器, 通过配置文件注册本地插件
 * 配置文件放置到主工程环境的assets目录位置，在初始化BridgeService时指定配置文件位置
 * 核心JS文件也放在主工程环境的assets目录，当webview加载完毕之后，会首先加载执行核心JS文件
 * @author panghui
 *
 */
public class LDJSPluginManager {
	private static final String LOG_TAG = "LDJSPluginManager" ;
	public static final String JsBridgeCoreFileName = "LDJSBridge.js.txt"; //根据文件在asset的位置自行配置，默认放在asset的根目录

	/***
	 * 记录某个插件对应的接口信息，提供对外开放给API的调用action名和native实际action的对应
	 * @author panghui
	 *
	 */
	public class LDJSExportDetail {
		public String showMethod; //JSAPI调用的action方法
		public String realMethod; //插件本地的action真实名字

		public LDJSExportDetail(String theShowMethod, String theRealMethod){
			this.showMethod = theShowMethod;
			this.realMethod = theRealMethod;
		}
	}


	/***
	 * 本地插件的信息
	 * @author panghui
	 *
	 */
	public class LDJSPluginInfo {
		public String pluginName; //插件名
		public String pluginClass; //插件对应的实现class
		public HashMap<String, LDJSExportDetail> exports = new HashMap<String, LDJSExportDetail>(); //插件对外开放的接口配置
		public LDJSPlugin instance; //插件实例

		public LDJSPluginInfo(){
			this.instance = null;
		}

		/***
		 * 返回showMethod对应api接口的详细信息
		 * @param showMethod
		 * @return
		 */
		public LDJSExportDetail getDetailByShowMethod(String showMethod){
			return exports.get(showMethod);
		}
	}



	private String updateUrl = null; //核心框架JS的线上更新地址
	private String coreBridgeJSFileName = null; //核心框架JS的文件名
	private boolean isUpdate = false; //是否已经检查和下载远程最新的核心框架JS
	private LDJSActivityInterface activityInterface = null;
	private WebView webView = null;
	private Context context = null;
	private HashMap<String, LDJSPluginInfo> pluginMap = new HashMap<String,LDJSPluginInfo>();

	/**
	 * 初始化插件管理器，必须赋予插件配置文件(放置于Assets目录)，当前执行环境，当前webview，当前webview绑定的activity
	 * @param configFile
	 * @param theContext
	 * @param theActivityInterface
	 * @param theWebView
	 */
	public LDJSPluginManager(String configFile,Context theContext, LDJSActivityInterface theActivityInterface, WebView theWebView){
		context = theContext;
		activityInterface = theActivityInterface;
		webView = theWebView;

		try {
			//根据插件配置文件初始化插件
			resetWithConfigFile(configFile);

			//设置插件完成之后，开启线程检查线上是否有更新
			new Thread(new Runnable() {
				public void run() {
					updateCodeBridgeJSCode();
				}
			}).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * 根据本地的插件配置信息，初始化插件管理器
	 * @param file
	 * @throws IOException
	 */
	private void resetWithConfigFile(String file) throws IOException{
		pluginMap.clear();
		try {
			//读取配置文件
           InputStreamReader inputReader = new InputStreamReader(context.getAssets().open(file) );
           BufferedReader bufReader = new BufferedReader(inputReader);
           String line="";
           String Result="";
           while((line = bufReader.readLine()) != null)
               Result += line;

           //解析配置文件
           JSONObject dict = new JSONObject(Result);
           if(dict != null && dict.length() > 0){
        	   updateUrl = dict.getString("update");
        	   //从updateUrl中获取核心文件的名字
        	   if(updateUrl != null && updateUrl.length() > 0){
        		   int start= updateUrl.lastIndexOf("/");
        		   if(start != -1){
        			   coreBridgeJSFileName = updateUrl.substring(start+1, updateUrl.length());
        		   }
        	   }
        	   if(coreBridgeJSFileName == null) coreBridgeJSFileName = JsBridgeCoreFileName;

        	   JSONArray plugins = dict.getJSONArray("plugins");
        	   if(plugins == null || plugins.length() == 0) return;
        	   for(int i = 0; i < plugins.length(); i++){
        		   JSONObject plugin = plugins.getJSONObject(i);
        		   LDJSPluginInfo info = new LDJSPluginInfo();
        		   info.pluginName = plugin.getString("pluginname");
        		   info.pluginClass = plugin.getString("pluginclass");

        		   //遍历接口信息
        		   JSONArray exports = plugin.getJSONArray("exports");
        		   if(exports != null && exports.length() >0){
        			   for(int j = 0; j < exports.length(); j++){
        				   JSONObject exportInfo = exports.getJSONObject(j);
        				   String showMethod = exportInfo.getString("showmethod");
        				   String realMethod = exportInfo.getString("realmethod");
        				   if(showMethod != null && realMethod != null){
        					   LDJSExportDetail tmp = new LDJSExportDetail(showMethod,realMethod);
        					   info.exports.put(showMethod, tmp);
        				   }
        			   }
        		   }

        		   //初始化插件实例
        		   LDJSPlugin pluginInstance = instantiatePlugin(info.pluginClass);
        		   if(pluginInstance != null && webView != null && activityInterface != null){
        			   pluginInstance.privateInitialize(activityInterface, webView);
        			   info.instance = pluginInstance;
        		   }

        		   pluginMap.put(info.pluginName, info);
        	   }//for
           }//if

       } catch (Exception e) {
           e.printStackTrace();
       }

	}


	/**
	 * 根据插件对应的className生成一个插件实例
	 * @param className
	 * @return
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


	/**
	 * 根据插件名称获取该插件的实例
	 * @param pluginName
	 * @return
	 */
	public LDJSPlugin getPluginInstance(final String pluginName){
		LDJSPluginInfo pluginInfo = pluginMap.get(pluginName);
		if(pluginInfo != null){
			return pluginInfo.instance;
		} else {
			return null;
		}
    }



	/**
	 * 根据showMethod获取插件的实际接口名字
	 * @param showMethod
	 * @return
	 */
	public String realForShowMethod(String showMethod){
		String realMethod = null;
	     Iterator<String> ite = pluginMap.keySet().iterator();
	     while (ite.hasNext()) {
	    	 LDJSPluginInfo tmp = pluginMap.get(ite.next());
	    	 if(tmp.getDetailByShowMethod(showMethod) != null){
	    		 realMethod = tmp.getDetailByShowMethod(showMethod).realMethod;
	    		 break;
	    	 }
	      }

	     if(realMethod == null){
	    	 realMethod = showMethod;
	     }

	     return realMethod;
	}



	/**
	 * 每次初始化bridgeService时，检查线上文件是否有更新，如果更新，下载并替换文件
	 * @throws IOException
	 */
	private void updateCodeBridgeJSCode(){
		if(!this.isUpdate && this.updateUrl != null){
			String onlineJsCode = this.getContentFromUrl(this.updateUrl);
			String localJsCode = localCoreBridgeJSCode();
	        if( (onlineJsCode != null && onlineJsCode.length() > 0) &&
	        	(localJsCode != null && localJsCode.length() > 0) &&
	        		(onlineJsCode.length() != localJsCode.length() || !onlineJsCode.equals(localJsCode))){
	        	//重写asset的核心JS文件
	        	writeTxtFile(onlineJsCode, bridgeCacheDir() + "/" + coreBridgeJSFileName);
	        }

	        this.isUpdate = true;
		}
	}


	/**
	 * 从url上获取内容
	 * @param url
	 * @return
	 */
	private String getContentFromUrl(String url){
		InputStream is = null;
		String result = "";

		try{
			URLConnection conn = null;
	        URL theURL = new URL(url);
	        conn = theURL.openConnection();
	        HttpURLConnection httpConn = (HttpURLConnection) conn;
	        httpConn.setRequestMethod("GET");
	        httpConn.connect();
	        if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
	            is = httpConn.getInputStream();
	        }

		}catch(Exception e){
			return "";
		}

		if(is == null) return "";

		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null){
				sb.append(line+"\n");
			}

			result = sb.toString();
		}catch(Exception e){
			return "";
		}

		return result;
	}


	/**
	 * 将asset对应的核心JS代码拷贝到app对应的数据目录，每次加载核心JS的时候从App该目录加载
	 * 更新核心JS也放在此目录覆盖
	 * @return
	 */
	public String localCoreBridgeJSCode(){
		File cacheBridgeFile = new File(bridgeCacheDir(), coreBridgeJSFileName);
		if(!cacheBridgeFile.exists()){
			try {
				InputStream is;
				is = context.getAssets().open(coreBridgeJSFileName);
	            FileOutputStream fos = new FileOutputStream(new File(cacheBridgeFile.getPath()));
	            byte[] buffer = new byte[2048];
	            int byteCount=0;
	            while((byteCount=is.read(buffer))!=-1) {//循环从输入流读取 buffer字节
	                fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
	            }
	            fos.flush();//刷新缓冲区
	            is.close();
	            fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


	    String jsBrideCodeStr = readTxtFile(bridgeCacheDir() + "/" + coreBridgeJSFileName);
	    return jsBrideCodeStr;
	}


	/**
	 * 在应用安装目录建立Bridge核心JS存储目录
	 * @return
	 */
	private String bridgeCacheDir(){
		File theBridgeCacheDir = new File(context.getExternalCacheDir().getPath(), "_ldbridge_Cache_");
		Log.i(LOG_TAG, "theBridgeCacheDir>>>>>"+ theBridgeCacheDir.getPath());
		if(!theBridgeCacheDir.exists()){
			if(!theBridgeCacheDir.mkdir()){
				return "";
			}
		}

		return theBridgeCacheDir.getPath();
	}


	  /**
	   * 从指定路径文件读取内容
	   * @param strFilePath
	   * @return
	   */
	    private static String readTxtFile(String strFilePath){
	        String path = strFilePath;
	        String content = ""; //文件内容字符串
            //打开文件
            File file = new File(path);
            //如果path是传递过来的参数，可以做一个非目录的判断
            if (file.isDirectory())
            {
                Log.d("TestFile", "The File doesn't not exist.");
            }
            else
            {
                try {
                    InputStream instream = new FileInputStream(file);
                    if (instream != null)
                    {
                        InputStreamReader inputreader = new InputStreamReader(instream);
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        String line;
                        //分行读取
                        while (( line = buffreader.readLine()) != null) {
                            content += line + "\n";
                        }
                        instream.close();
                    }
                }
                catch (java.io.FileNotFoundException e)
                {
                    Log.d("TestFile", "The File doesn't not exist.");
                }
                catch (IOException e)
                {
                     Log.d("TestFile", e.getMessage());
                }
            }
            return content;

	    }



	    /**
	     * 将字符串内容写入指定路径的文件
	     * @param strContent
	     * @param strFilePath
	     */
	    private static void writeTxtFile(String strContent,String strFilePath){
	      try {
	           File file = new File(strFilePath);
	           if (file.exists()) {
	        	  file.delete();
	           }

	           file.createNewFile();
	           RandomAccessFile raf = new RandomAccessFile(file, "rw");
	           raf.seek(file.length());
	           raf.write(strContent.getBytes());
	           raf.close();
	      } catch (Exception e) {
	           Log.e("TestFile", "Error on write File.");
	      }
	    }


}
