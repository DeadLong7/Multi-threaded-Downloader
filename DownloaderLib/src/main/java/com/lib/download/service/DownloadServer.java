package com.lib.download.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.lib.download.DownloadManager;
import com.lib.download.contact.Contact;
import com.lib.download.contact.FileInfo;
import com.lib.download.db.ThreadDAOImpl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 下载服务
 */
public class DownloadServer extends Service {

    // 下载管理实例
    private DownloadManager manager = null;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 获取下载管理实例
        manager = DownloadManager.getInstance();
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(Contact.ACTION_START);
        filter.addAction(Contact.ACTION_PAUSE);
        filter.addAction(Contact.ACTION_UPDATE);
        filter.addAction(Contact.ACTION_FINISHED);
        filter.addAction(Contact.ACTION_FAILED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) {
            // 如果intnet为空，可能是程序被强制关闭
            ThreadDAOImpl.getInstance().updataToPaused();
        } else {
            if(Contact.ACTION_START.equals(intent.getAction())) {
                FileInfo fileInfo = intent.getParcelableExtra(Contact.FILE_INFO_KEY);
                if(ThreadDAOImpl.getInstance().isExists(fileInfo.getUrl())) {
                    // 数据库存在数据，则直接下载，线程数这里传多少无所谓，在下载任务中会处理
                    manager.addDownloadTask(this, fileInfo, DownloadManager.getMaxThreadNumOfTask());
                } else if(fileInfo.getLength() > 0) {
                    // 如果文件长度已知，则可以直接进行下载
                    // 计算需要启动的线程数
                    int threadCount = (int) (fileInfo.getLength() / DownloadManager.getSplitSizeOfThread()) + 1;
                    if(threadCount > DownloadManager.getMaxThreadNumOfTask()) {
                        threadCount = DownloadManager.getMaxThreadNumOfTask();
                    }
                    manager.addDownloadTask(this, fileInfo, threadCount);
                } else {
                    // 先获取文件大小再进行下载
                    DownloadTask.threadPool.execute(new InitThread(fileInfo));
                }
                return super.onStartCommand(intent, flags, startId);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        manager.clearDownloadTasks();
        unregisterReceiver(mReceiver);
    }

    /**
     * 在线程中处理下载任务
     */
    private class InitThread extends Thread {
        private FileInfo fileInfo;

        public InitThread(FileInfo fileInfo) {
            this.fileInfo = fileInfo;
        }

        @Override
        public void run() {
            HttpURLConnection conn = null;
            RandomAccessFile raFile = null;
            try {
                URL url = new URL(fileInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                if(conn.getResponseCode() == 200) {
                    int fileLength = conn.getContentLength();
                    if(fileLength <= 0) {
                        return;
                    }

                    File downloadPath = new File(manager.getDownloadDir());
                    if(!downloadPath.exists()) {
                        downloadPath.mkdir();
                    }
                    File downloadFileTmp = new File(manager.getDownloadDir(), fileInfo.getName() + ".tmp");
                    if(!downloadFileTmp.exists()) {
                        downloadFileTmp.createNewFile();
                    }
                    raFile = new RandomAccessFile(downloadFileTmp, "rw");
                    raFile.setLength(fileLength);
                    fileInfo.setLength(fileLength);
                    // 计算需要启动的线程数
                    int threadCount = fileLength / DownloadManager.getSplitSizeOfThread() + 1;
                    if(threadCount > DownloadManager.getMaxThreadNumOfTask()) {
                        threadCount = DownloadManager.getMaxThreadNumOfTask();
                    }
                    manager.addDownloadTask(DownloadServer.this, fileInfo, threadCount);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(conn != null) {
                        conn.disconnect();
                    }
                    if(raFile != null) {
                        raFile.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 广播接收器
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Contact.ACTION_UPDATE.equals(intent.getAction())) {

            } else if (Contact.ACTION_START.equals(intent.getAction())) {

            } else if (Contact.ACTION_PAUSE.equals(intent.getAction())) {
                FileInfo fileInfo = intent.getParcelableExtra(Contact.FILE_INFO_KEY);
                manager.removeDownloadTask(fileInfo.getUrl());
            } else if (Contact.ACTION_FINISHED.equals(intent.getAction())) {
                FileInfo fileInfo = intent.getParcelableExtra(Contact.FILE_INFO_KEY);
                manager.removeDownloadTask(fileInfo.getUrl());
            } else if (Contact.ACTION_FAILED.equals(intent.getAction())) {
                FileInfo fileInfo = intent.getParcelableExtra(Contact.FILE_INFO_KEY);
                manager.removeDownloadTask(fileInfo.getUrl());
            }
        }
    };
}
