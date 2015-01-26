package com.netease.JSBridge;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import com.netease.JSBridge.LDJSPlugin;

/***
 * 本地插件管理器
 * @author panghui
 *
 */
public class LDJSPluginManager {
	private static final String TAG = "LDJSPluginManager" ;
	public static final String JsBridgeCoreFileName = "LDJSBridge.js";
	/***
	 * 记录某个插件对应的接口信息，提供对外开放给API的调用action名和native实际action的对应
	 * @author panghui
	 *
	 */
	public class LDJSExportDetail {
		public String showAction; //JSAPI调用的action方法
		public String realAction; //插件本地的action真实名字

		public LDJSExportDetail(String theShowAction, String theRealAction){
			this.showAction = theShowAction;
			this.realAction = theRealAction;
		}
	}


	/***
	 * 本地插件的信息
	 * @author panghui
	 *
	 */
	public class LDJSPluginInfo {
		public String pluginName;
		public String pluginClass;
		public HashMap<String, LDJSExportDetail> exports = new HashMap<String, LDJSExportDetail>();
		public LDJSPlugin instance;

		public LDJSPluginInfo(){
			this.instance = null;
		}

		/***
		 * 返回showAction对应api接口的详细信息
		 * @param showAction
		 * @return
		 */
		public LDJSExportDetail getDetailByShowAction(String showAction){
			return exports.get(showAction);
		}
	}



	private String updateUrl = null; //核心框架JS的下载地址
	private boolean isUpdate = false; //是否检查和下载远程最新的核心框架JS
	private LDJSActivityInterface activityInterface = null;
	private WebView webView = null;
	private Context context;
	private HashMap<String, LDJSPluginInfo> pluginMap = new HashMap<String,LDJSPluginInfo>();

	public LDJSPluginManager(String configFile,Context theContext, LDJSActivityInterface theActivityInterface, WebView theWebView){
		context = theContext;
		activityInterface = theActivityInterface;
		webView = theWebView;

		try {
			this.resetWithConfigFile(configFile);
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
           InputStreamReader inputReader = new InputStreamReader(context.getAssets().open(file) );
           BufferedReader bufReader = new BufferedReader(inputReader);
           String line="";
           String Result="";
           while((line = bufReader.readLine()) != null)
               Result += line;

           JSONObject dict = new JSONObject(Result);
           if(dict != null && dict.length() > 0){
        	   this.updateUrl = dict.getString("update");
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
	 * 根据showAction获取插件的实际执行Action名字
	 * @param showAction
	 * @return
	 */
	public String realForShowAction(String showAction){
		String realAction = null;
	     Iterator<String> ite = pluginMap.keySet().iterator();
	     while (ite.hasNext()) {
	    	 LDJSPluginInfo tmp = pluginMap.get(ite.next());
	    	 if(tmp.getDetailByShowAction(showAction) != null){
	    		 realAction = tmp.getDetailByShowAction(showAction).realAction;
	    		 break;
	    	 }
	      }

	     if(realAction == null){
	    	 realAction = showAction;
	     }

	     return realAction;
	}



	/**
	 * 每次初始化bridgeService时，检查线上文件是否有更新，如果更新，下载并替换文件
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void updateCodeBridgeJSCode(){
		if(!this.isUpdate && this.updateUrl != null){
			String onlineJsCode = this.getContentFromUrl(this.updateUrl);
			  InputStreamReader inputReader;
			try {
				inputReader = new InputStreamReader(context.getAssets().open("LDJSBridge.js.text") );
		           BufferedReader bufReader = new BufferedReader(inputReader);
		           String line="";
		           String localJsCode="";
		           while((line = bufReader.readLine()) != null)
		        	   localJsCode += line;

		           if(onlineJsCode.length() != localJsCode.length() || onlineJsCode.equals(localJsCode)){
		        	   //重写asset的核心JS文件
		        	   writeTxtFile(onlineJsCode, this.bridgeCacheDir()+JsBridgeCoreFileName+".text");
		           }

		           this.isUpdate = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			HttpResponse response = httpClient.execute(httppost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
		}catch(Exception e){
			return "";
		}

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
		File cacheBridgeFile = new File(this.bridgeCacheDir(), JsBridgeCoreFileName+".text");
		if(!cacheBridgeFile.exists()){
			//bundleBridgeFile = new
            InputStream is;
			try {
				is = context.getAssets().open(JsBridgeCoreFileName+".text");
	            FileOutputStream fos = new FileOutputStream(new File(cacheBridgeFile.getPath()));
	            byte[] buffer = new byte[2048];
	            int byteCount=0;
	            while((byteCount=is.read(buffer))!=-1) {//循环从输入流读取 buffer字节
	            	System.out.print(buffer.toString());
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


	    String jsBrideCodeStr = readTxtFile(this.bridgeCacheDir()+"/" + JsBridgeCoreFileName+ ".text");
	    return jsBrideCodeStr;
	}


	/**
	 * 在应用安装目录建立Bridge核心JS存储目录
	 * @return
	 */
	private String bridgeCacheDir(){
		File theBridgeCacheDir = new File(context.getExternalCacheDir().getPath(), "_ldbridge_Cache_");
		Log.i(TAG, "theBridgeCacheDir>>>>>"+ theBridgeCacheDir.getPath());
		if(!theBridgeCacheDir.exists()){
			if(!theBridgeCacheDir.mkdir()){
				return "";
			}
		}

		return theBridgeCacheDir.getPath();
	}


	/**
	 * 拷贝文件到输出文件
	 * @param inputFile
	 * @param outputFile
	 * @return
	 */
	  private static boolean copy(File inputFile, File outputFile) {

		    if (!inputFile.exists()) {
		      return false;
		    }
		    if (!outputFile.exists()) {

		      try {
		        outputFile.createNewFile();
		      } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		        return false;
		      }
		    }
		    FileInputStream fin;
		    try {
		      fin = new FileInputStream(inputFile);
		      copy(fin, outputFile);
		    } catch (FileNotFoundException e) {
		      // TODO Auto-generated catch block
		      e.printStackTrace();
		      return false;
		    } catch (IOException e) {
		      // TODO Auto-generated catch block
		      e.printStackTrace();
		      return false;
		    }

		    return true;
	}


	  /**
	   * 从一个读入流拷贝到输出文件
	   * @param is
	   * @param outputFile
	   * @throws IOException
	   */
	  private static void copy(InputStream is, File outputFile) throws IOException {
		    OutputStream os = null;

		    try {
		      os = new BufferedOutputStream(new FileOutputStream(outputFile));
		      byte[] b = new byte[4096];
		      int len = 0;
		      while ((len = is.read(b)) != -1) {
		        os.write(b, 0, len);
		      }
		    } finally {
		      if (is != null) {
		        is.close();
		      }
		      if (os != null) {
		        os.close();
		      }
		    }
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
	           if (!file.exists()) {
	            Log.d("TestFile", "Create the file:" + strFilePath);
	            file.createNewFile();
	           }
	           RandomAccessFile raf = new RandomAccessFile(file, "rw");
	           raf.seek(file.length());
	           raf.write(strContent.getBytes());
	           raf.close();
	      } catch (Exception e) {
	           Log.e("TestFile", "Error on write File.");
	          }
	    }


}
