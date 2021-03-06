package org.apache.cordova.downloader;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.Toast;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ASUS on 2015/11/4.
 */
public class Downloader extends CordovaPlugin {

    private static final String TAG = "Downloader";

    private static final String PLUGIN_ACTION = "download";

    private Context context;

    private SharedPreferences sharedPreferences;

    private static final String EXTERNAL_DIR = "Deputy";

    /**
     * 获取文件信息成功
     * */
    private static final int GET_FILEINFO_SUCCESS = 200;

    /**
     * 下载文件
     * */
    private static final int DOWNLOAD_FILE = 300;

    /**
     * 打开文件
     * */
    private static final int OPEN_FILE = 400;

    private static long downloadId;

    private DownloadManager manager ;

    private ProgressDialog progressDialog;

    private String session;

    private Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_FILEINFO_SUCCESS:

                    if(progressDialog != null){
                        progressDialog.dismiss();
                    }

                    final Map<String,String> map = (Map<String,String>)msg.obj;

                    if(map == null || map.get("fileName")==null || map.get("url")==null){
                        AlertDialog.Builder builder = new AlertDialog.Builder(Downloader.this.cordova.getActivity());
                        builder.setMessage("获取文件信息失败！");
                        builder.setTitle("提示");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }else {

                        String url = map.get("url");
                        final String fid = url.substring(url.lastIndexOf("/")+1);
                        Log.d(TAG,"fid:"+fid);
                        if(isFileHasDwonloaded(fid)){//已经下载过文件
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setMessage("您已下载过：" + map.get("fileName"));
                            builder.setTitle("提示");
                            builder.setPositiveButton("打开", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Message msg1 = new Message();
                                    msg1.what = OPEN_FILE;
                                    msg1.obj = sharedPreferences.getString(fid,null);
                                    myHandler.sendMessage(msg1);
                                    dialog.dismiss();
                                }
                            });
                            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            builder.create().show();
                        }else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setMessage("确认下载文件？\n文件名：" + map.get("fileName") + "\n文件大小：" + (map.get("length") == null ? "未知大小" : map.get("length")));
                            builder.setTitle("提示");
                            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Message msg1 = new Message();
                                    msg1.what = DOWNLOAD_FILE;
                                    msg1.obj = map;
                                    myHandler.sendMessage(msg1);
                                    dialog.dismiss();
                                }
                            });
                            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            builder.create().show();
                        }
                    }
                    break;
                case DOWNLOAD_FILE:
                    Map<String,String> map1 = (Map<String,String>)msg.obj;
                    download(Downloader.this.cordova.getActivity(),map1.get("url"),map1.get("fileName"));
                    break;
                case OPEN_FILE:
                    openFile(msg.obj.toString());
                    break;
            }
            super.handleMessage(msg);
        }
    };

    //插件初始化时会被调用，初始化context/sharedpreference
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova,webView);
        context = this.cordova.getActivity();
        sharedPreferences = context.getSharedPreferences(context.getPackageName(),Context.MODE_PRIVATE);
    }

    @Override
    public boolean execute(String action, JSONArray args,final CallbackContext callbackContext) throws JSONException {

        if (action.equals(PLUGIN_ACTION)) {
            String url = args.getString(0);
            if(url!=null&&!url.equals("")){
                this.download(context, callbackContext, url);
            }else{
                callbackContext.error("url cannot be null!");
            }
            return true;
        }
        callbackContext.error("method not exist!!");
        return false;
    }

    //当前段调用Downloader.download方法时的处理函数
    private void download(Context context , final CallbackContext callbackContext,String url){
        CookieManager cookieManager = CookieManager.getInstance();
        session = cookieManager.getCookie(url);

        //显示ProgressDialog
        progressDialog = ProgressDialog.show(Downloader.this.cordova.getActivity(), "获取数据中...", "请稍后...", true, false);

        final String tempUrl = url;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String,String> map = getFileName(tempUrl);
                Message msg = new Message();
                msg.what=GET_FILEINFO_SUCCESS;
                msg.obj = map;
                myHandler.sendMessage(msg);
            }
        }).start();
    }

    //根据文件的url和name下载文件
    public void download(Context context,String url,String name){
        manager =(DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING);
        Cursor c = manager.query(query);
        if(c.moveToNext()){
            Toast.makeText(context, "已经在下载...", Toast.LENGTH_SHORT).show();
        }else{
            DownloadManager.Request down = new DownloadManager.Request(Uri.parse(url));
            String host = "";
            try {
                host = new URL(url).getHost();
                Log.d("url",url);
                Log.d("host",host);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String cookieStr = CookieManager.getInstance().getCookie(host);
            if (cookieStr!=null&&!cookieStr.equals("")) {
                Log.d("cookieStr",cookieStr);
                down.addRequestHeader("Cookie", cookieStr + "; AcSe=0");
            }

            down.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
            down.setVisibleInDownloadsUi(true);
            down.setTitle(name);
            down.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            /*if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                down.setDestinationInExternalFilesDir(context, null, "");
                request.setDestinationInExternalPublicDir("download", "DOTA2���Ͽ�.apk");
            }else{
                //down.setDestinationInExternalPublicDir();
            }*/
            isFolderExist(EXTERNAL_DIR);
            down.setDestinationInExternalPublicDir(EXTERNAL_DIR, name);
            downloadId = manager.enqueue(down);

            sharedPreferences.edit().putString(String.valueOf(downloadId),url.substring(url.lastIndexOf("/")+1)).commit();

            Toast.makeText(context, "开始下载...", Toast.LENGTH_SHORT).show();
        }
    }

    //打开文件的方法
    private void openFile(String path){
        File file  = new File(path);
        if(file.exists()){
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            String type = DownloadUtil.getMIMEType(file);
            intent.setDataAndType(Uri.fromFile(file),type);
            context.startActivity(intent);
        }else{
            Toast.makeText(context,"文件不存在！",Toast.LENGTH_SHORT).show();
        }

    }

    //判断文件夹是否存在，不存在则创建
    private boolean isFolderExist(String dir) {
        File folder = Environment.getExternalStoragePublicDirectory(dir);
        return (folder.exists() && folder.isDirectory()) ? true : folder.mkdirs();
    }

    //根据response头获取文件名和大小
    public  Map<String,String> getFileName(String url) {
        Map<String,String> map = new HashMap<String, String>();
        String filename = "";
        String length = "";
        boolean isok = false;
        HttpURLConnection conn = null;
        try {
            URL myURL = new URL(url);
            conn = (HttpURLConnection)myURL.openConnection();
            if (conn == null) {
                return null;
            }
            conn.setRequestProperty("Cookie", session);
            //conn.connect();

            for (Map.Entry<String, List<String>> header : conn.getHeaderFields().entrySet()) {
                if(header.getKey()==null) continue;
                if (header.getKey().equals("File-Size")) {
                    length = header.getValue().get(0);
                    map.put("length", DownloadUtil.convertFileSize(Integer.parseInt(length)));
                } else if (header.getKey().equals("Content-Disposition")) {
                    String content = header.getValue().get(0);
                    content = content.substring(content.indexOf("filename") + "filename".length());
                    filename = content.substring(content.indexOf("=") + 1);
                    map.put("fileName", filename);
                }
            }
        }catch (Exception e){

        }
        map.put("url",url);
        return map;
    }

    //判断文件是否下载过
    private boolean isFileHasDwonloaded(String fid){
        if(null != fid){
            String fileName = sharedPreferences.getString(fid,null);
            Log.d(TAG,"fileName:"+fileName);
            if(fileName == null){
                return false;
            }else if(new File(fileName).exists()){
                return true;
            }else {
                return false;
            }
        }else{
            return false;
        }
    }

}
