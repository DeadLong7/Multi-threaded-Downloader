package com.lib.download;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.lib.download.contact.Contact;
import com.lib.download.contact.FileInfo;
import com.lib.download.db.ThreadDAOImpl;
import com.lib.download.service.DownloadServer;
import com.lib.download.service.DownloadTask;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

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
        return config.getMaxExceptionCount();
    }

    /**
     * 判断文件是否已经下载或正在下载
     * @param fileInfo
     * @return
     */
    private static boolean checkFileIsExists(Context context, FileInfo fileInfo) {
        File file = new File(getDownloadDir(), fileInfo.getName());
        if(file.exists()) {
            // 已经下载
            Toast.makeText(context, "文件已经存在", Toast.LENGTH_SHORT).show();
            return true;
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
        if (listener != null) {
            mapTasksListener.put(fileInfo.getUrl(), listener);
        }
        fileInfo.setStatus(Contact.DOWNLOAD_WAIT);
        Intent intent = new Intent(context, DownloadServer.class);
        intent.setAction(Contact.ACTION_START);
        intent.putExtra(Contact.FILE_INFO_KEY, fileInfo);
        context.startService(intent);
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
//            Log.e("DownloadServer", "ACTION_CANCLE: " + fileUrl);
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
            fileInfo.setStatus(Contact.DOWNLOAD_CANCLE);
            fileInfo.setLoadSize(0);
            // 发送取消广播
            Intent intent = new Intent(Contact.ACTION_CANCLE);
            intent.putExtra(Contact.FILE_INFO_KEY, fileInfo);
            context.sendBroadcast(intent);
            // 发送更新广播
            intent = new Intent(Contact.ACTION_UPDATE);
            intent.putExtra(Contact.FILE_INFO_KEY, fileInfo);
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
}
