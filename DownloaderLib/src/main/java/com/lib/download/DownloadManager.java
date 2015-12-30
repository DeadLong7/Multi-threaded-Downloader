package com.lib.download;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.lib.download.contact.DownloadContact;
import com.lib.download.contact.FileInfo;
import com.lib.download.db.ThreadDAOImpl;
import com.lib.download.service.DownloadServer;
import com.lib.download.service.DownloadTask;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by long on 2015/11/16.
 */
public class DownloadManager {

    private static DownloadManager instance;
    // 下载任务映射表，一个文件URL对应一个任务
    private static Map<String, DownloadTask> mapTasks = new LinkedHashMap<>();
    // 下载任务监听器
    private static Map<String, DownloadListener> mapTasksListener = new LinkedHashMap<>();
    // 配置器
    private static DownloadConfig config = new DownloadConfig.Builder().build();
    // 随机数
    private static Random mRandom = new Random();
    // 文件和通知ID的映射
    private static Map<String, Integer> mapNotify = new LinkedHashMap<>();


    private DownloadManager() {
    }

    /**
     * 获取实例句柄
     * @return
     */
    public static DownloadManager getInstance() {
        if(instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }

    /**
     * 设置配置参数
     * @param config
     */
    public static void setConfig(DownloadConfig config) {
        DownloadManager.config = config;
    }

    /**
     * 获取下载目录
     * @return
     */
    public static String getDownloadDir() {
        return config.getDownloadDir();
    }

    /**
     * 获取一个任务最大的线程数
     * @return
     */
    public static int getMaxThreadNumOfTask() {
        return config.getMaxThreadNumOfTask();
    }

    /**
     * 获取拆分文件大小的标准
     * @return
     */
    public static int getSplitSizeOfThread() {
        return config.getSplitSizeOfThread();
    }

    /**
     * 获取一个任务最大允许的异常重连次数
     * @return
     */
    public static int getMaxExceptionCount() {
        return config.getMaxRedirectCount();
    }

    /**
     * 判断文件是否已经下载或正在下载
     * @param fileInfo
     * @return
     */
    private static boolean checkFileIsExists(Context context, FileInfo fileInfo) {
        File file;
        if (fileInfo.getName().endsWith(".apk")) {
            file = new File(getDownloadDir(), fileInfo.getName());
        } else {
            file = new File(getDownloadDir(), fileInfo.getName() + ".apk");
        }
        if(file.exists()) {
            // 已经存在则删除重下
            file.delete();
            if (ThreadDAOImpl.getInstance().isExists(fileInfo.getUrl())) {
                // 删除数据库中的记录
                ThreadDAOImpl.getInstance().deleteThread(fileInfo.getUrl());
            }
            return false;
        }
        DownloadTask task = mapTasks.get(fileInfo.getUrl());
        if(task != null) {
            if(task.isTaskStop()) {
                // 如果任务为暂停状态,则直接重新启动下载任务
                task.startDownload();
                return true;
            }
            // 正在下载
            Toast.makeText(context, "正在下载...", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    /**
     * 启动下载服务
     *
     * @param context
     * @param fileUrl
     * @param fileName
     * @param listener
     */
    public static void startDownload(Context context, String fileUrl, String fileName, DownloadListener listener) {
        startDownload(context, fileUrl, fileName, 0, listener);
    }

    /**
     * 启动下载服务
     *
     * @param context
     * @param fileUrl
     * @param fileName
     * @param fileLength
     * @param listener
     */
    public static void startDownload(Context context, String fileUrl, String fileName, int fileLength, DownloadListener listener) {
        FileInfo fileInfo = new FileInfo(0, fileUrl, fileName, fileLength);
        startDownload(context, fileInfo, listener);
    }

    /**
     * 启动下载服务
     *
     * @param context
     * @param fileInfo
     */
    public static void startDownload(Context context, FileInfo fileInfo, DownloadListener listener) {
        if(checkFileIsExists(context, fileInfo)) {
            return;
        }
        fileInfo.setStatus(DownloadContact.DOWNLOAD_WAIT);
        if (listener != null) {
            mapTasksListener.put(fileInfo.getUrl(), listener);
            listener.onDownloadWaiting(fileInfo);
        }
        Intent intentBroadcast = new Intent(DownloadContact.ACTION_DOWNLOAD);
        intentBroadcast.putExtra(DownloadContact.FILE_INFO_KEY, fileInfo);
        context.sendBroadcast(intentBroadcast);

        Intent intent = new Intent(context, DownloadServer.class);
        intent.setAction(DownloadContact.ACTION_START);
        intent.putExtra(DownloadContact.FILE_INFO_KEY, fileInfo);
        context.startService(intent);
    }

    /**
     * 开启带通知功能的下载，注意，该方法暂不适用外部监听器，需接收下载广播来监听
     * @param context
     * @param fileUrl
     * @param fileName
     */
    public static void startDLWithNotification(Context context, String fileUrl, String fileName) {
        FileInfo fileInfo = new FileInfo(0, fileUrl, fileName, 0);
        startDLWithNotification(context, fileInfo);
    }

    /**
     * 开启带通知功能的下载，注意，该方法暂不适用外部监听器，需接收下载广播来监听
     * @param context
     * @param fileInfo
     */
    public static void startDLWithNotification(final Context context, FileInfo fileInfo) {
        final NotificationManager nm = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(android.R.drawable.stat_sys_download);
        final int notifyId;
        if (mapNotify.containsKey(fileInfo.getUrl())) {
            notifyId = mapNotify.get(fileInfo.getUrl());
        } else {
            notifyId = mRandom.nextInt(1024) + 1;
        }

        startDownload(context, fileInfo, new DownloadListener() {
            @Override
            public void onDownloadWaiting(FileInfo fileInfo) {

            }

            @Override
            public void onDownloadStart(FileInfo fileInfo) {
                Log.w("TAG", "onDownloadStart: " + notifyId);
                if (!mapNotify.containsKey(fileInfo.getUrl())) {
                    mapNotify.put(fileInfo.getUrl(), notifyId);
                }
                builder.setColor(DownloadContact.COLOR_NOTIFY_DEFAULT);
                builder.setContentTitle(fileInfo.getName());
                // 点击通知栏则暂停下载
                builder.setContentIntent(setNotifyIntent(context, DownloadContact.ACTION_PAUSE, fileInfo, notifyId));
            }

            @Override
            public void onDownloadUpdated(FileInfo fileInfo) {
                Log.d("TAG", "onDownloadUpdated: " + notifyId);
                builder.setProgress((int) fileInfo.getLength(), (int) fileInfo.getLoadSize(), false);
                nm.notify(notifyId, builder.build());
            }

            @Override
            public void onDownloadPaused(FileInfo fileInfo) {
                Log.w("TAG", "onDownloadPaused: " + notifyId);
                // 点击通知栏则继续下载
                builder.setColor(DownloadContact.COLOR_NOTIFY_PAUSE);
                builder.setContentIntent(setNotifyIntent(context, DownloadContact.ACTION_START, fileInfo, notifyId));
                nm.notify(notifyId, builder.build());
            }

            @Override
            public void onDownloadResumed(FileInfo fileInfo) {

            }

            @Override
            public void onDownloadSuccessed(FileInfo fileInfo) {
                Log.d("TAG", "onDownloadSuccessed");
                mapNotify.remove(fileInfo.getUrl());
                // 点击通知栏则去除通知（未增加安装功能）
                builder.setColor(DownloadContact.COLOR_NOTIFY_FINISH);
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
                builder.setContentIntent(setNotifyIntent(context, DownloadContact.ACTION_CANCLE, fileInfo, notifyId));
                nm.notify(notifyId, builder.build());
            }

            @Override
            public void onDownloadCanceled(FileInfo fileInfo) {
                Log.e("TAG", "onDownloadCanceled: " + notifyId);
                nm.cancel(notifyId);
                mapNotify.remove(fileInfo.getUrl());
            }

            @Override
            public void onDownloadFailed(FileInfo fileInfo) {
                Log.d("TAG", "onDownloadFailed");
                // 点击通知栏则继续下载
                builder.setContentTitle(fileInfo.getName());
                builder.setColor(DownloadContact.COLOR_NOTIFY_FAILED);
                builder.setContentIntent(setNotifyIntent(context, DownloadContact.ACTION_START, fileInfo, notifyId));
                nm.notify(notifyId, builder.build());
            }

            @Override
            public void onDownloadRetry(FileInfo fileInfo) {

            }
        });
    }

    /**
     * 停止下载任务
     *
     * @param fileUrl
     */
    public static void stopDownload(String fileUrl) {
        DownloadTask task = mapTasks.get(fileUrl);
        if (task != null) {
            task.stopDownLoad();
        }
    }

    /**
     * 取消下载任务并删除下载的文件
     *
     * @param fileUrl
     */
    public static void cancleDownload(Context context, String fileUrl, String fileName) {
        DownloadTask task = mapTasks.get(fileUrl);
        if (task != null) {
            task.cancleDownload();
            mapTasks.remove(fileUrl);
            mapTasksListener.remove(fileUrl);
        } else if (ThreadDAOImpl.getInstance().isExists(fileUrl)) {
            // 删除下载文件
            File tmpFile = new File(DownloadManager.getDownloadDir(), fileName + ".tmp");
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            FileInfo fileInfo = new FileInfo(fileUrl, fileName);
            fileInfo.setStatus(DownloadContact.DOWNLOAD_CANCLE);
            fileInfo.setLoadSize(0);
            // 发送取消广播
            Intent intent = new Intent(DownloadContact.ACTION_DOWNLOAD);
            intent.putExtra(DownloadContact.FILE_INFO_KEY, fileInfo);
            context.sendBroadcast(intent);
            // 删除数据库中的记录
            ThreadDAOImpl.getInstance().deleteThread(fileInfo.getUrl());
        }
    }

    /**
     * 添加一个下载任务
     */
    public void addDownloadTask(Context context, FileInfo fileInfo, int threadCount) {
        // 获取对应的监听器
        DownloadListener listener = mapTasksListener.get(fileInfo.getUrl());
        // 新建下载任务并添加到任务列表中
        DownloadTask task = new DownloadTask(context, fileInfo, threadCount, listener);
        mapTasks.put(fileInfo.getUrl(), task);
        task.startDownload();
    }

    /**
     * 移除一个下载任务
     * @param fileUrl
     */
    public void removeDownloadTask(String fileUrl) {
        if(mapTasks.containsKey(fileUrl)) {
            mapTasks.remove(fileUrl);
        }
        if(mapTasksListener.containsKey(fileUrl)) {
            mapTasksListener.remove(fileUrl);
        }
    }

    /**
     * 清除任务队列
     */
    public void clearDownloadTasks() {
        mapTasks.clear();
        mapTasksListener.clear();
    }

    /**
     * 设置通知点击意图
     * @param context
     * @param action
     * @param fileInfo
     * @param notifyId
     * @return
     */
    private static PendingIntent setNotifyIntent(Context context, String action, FileInfo fileInfo, int notifyId) {
        Intent intent = new Intent(action);
        intent.putExtra(DownloadContact.FILE_INFO_KEY, fileInfo);
        intent.putExtra(DownloadContact.NOTIFY_ID_KEY, notifyId);
        // 这样处理会有问题，由于Action相同，存在两个以上时前面通知的Intent将被覆盖,可改成外部传入Intent来控制通知点击操作
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }
}
