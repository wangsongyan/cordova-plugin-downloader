package org.apache.cordova.downloader;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import net.kmeboa.R;

@SuppressLint("NewApi")
public class DownloadCompleteReceiver extends BroadcastReceiver {
    private DownloadManager manager ;
    @Override
    public void onReceive(Context context, Intent intent) {
        manager =(DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
        if(intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)){
            //ͨ获取下载文件的id
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Query query = new Query();
            query.setFilterById(downloadId);
            Cursor myDownload = manager.query(query);
            if (myDownload.moveToFirst()) {
                int fileNameIdx = myDownload.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                String fileName = myDownload.getString(fileNameIdx);
 
                int urlIndex = myDownload.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                final String url = myDownload.getString(urlIndex);
                final Context tempContext = context;
                Log.d("Receiver", url);

                //存储下载文件的位置
                SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(),Context.MODE_PRIVATE);
                String fid = sharedPreferences.getString(String.valueOf(downloadId),null);
                if(null != fid){
                    sharedPreferences.edit().putString(fid,fileName).commit();
                    sharedPreferences.edit().remove(String.valueOf(downloadId));
                }

                NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                Notification notification = new Notification();
                String tickerText = "文件下载成功！"; // 通知提示
                // 显示时间
                long when = System.currentTimeMillis();

                notification.icon = R.drawable.icon;// 设置通知的图标
                notification.tickerText = tickerText; // 显示在状态栏中的文字
                notification.when = when; // 设置来通知时的时间
                notification.sound = Uri.parse("android.resource://com.sun.alex/raw/dida"); // 自定义声音
                //notification.flags = Notification.FLAG_NO_CLEAR; // 点击清除按钮时就会清除消息通知,但是点击通知栏的通知时不会消失
                //notification.flags = Notification.FLAG_ONGOING_EVENT; // 点击清除按钮不会清除消息通知,可以用来表示在正在运行
                notification.flags |= Notification.FLAG_AUTO_CANCEL; // 点击清除按钮或点击通知后会自动消失
                //notification.flags |= Notification.FLAG_INSISTENT; // 一直进行，比如音乐一直播放，知道用户响应
                notification.defaults = Notification.DEFAULT_SOUND; // 调用系统自带声音
                notification.defaults = Notification.DEFAULT_SOUND;// 设置默认铃声
                notification.defaults = Notification.DEFAULT_VIBRATE;// 设置默认震动
                notification.defaults = Notification.DEFAULT_ALL; // 设置铃声震动
                notification.defaults = Notification.DEFAULT_ALL; // 把所有的属性设置成默认


                File file = new File(fileName);

                Intent i = new Intent();
                i.setAction("android.intent.action.VIEW");
                i.addCategory("android.intent.category.DEFAULT");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                String type = DownloadUtil.getMIMEType(file);
                i.setDataAndType(Uri.fromFile(file),type);

                // 获取PendingIntent,点击时发送该Intent
                PendingIntent pIntent = PendingIntent.getActivity(context, 0,i, 0);
                // 设置通知的标题和内容
                notification.setLatestEventInfo(context, "下载完成",fileName, pIntent);
                // 发出通知
                nManager.notify(0, notification);
            }
        }
    }

}

