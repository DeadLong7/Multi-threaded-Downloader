package com.lib.download.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.lib.download.DownloadListener;
import com.lib.download.DownloadManager;
import com.lib.download.contact.Contact;
import com.lib.download.contact.FileInfo;
import com.lib.download.contact.ThreadInfo;
import com.lib.download.db.ThreadDAO;
import com.lib.download.db.ThreadDAOImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载任务
 * Created by 95 on 2015/11/12.
 */
public class DownloadTask {

    private Context context;
    // 传入的要下载的文件信息
    private FileInfo fileInfo = null;
    // 数据库句柄
    private ThreadDAO threadDao = null;
    // 已下载的数据大小
    private long loadSize = 0;
    // 停止下载任务的标志位
    private boolean isStop = false;
    // 线程总数
    private int threadCount;
    // 保存所有活动的线程
    private List<DownloadThread> listDownloadThreads;
    // 线程返回异常的次数
    private int exceptionCount = 0;
    // 最大允许的异常重连次数(线程太多容易异常?)，超过则下载失败
//    private static final int MAX_EXCEPTION_COUNT = 50;
    // 下载监听器
    private DownloadListener downloadListener;
    // 已下载数据大小和时间，用来计算下载速度
    private long calcLoadSize = 0;
    private long calcTime = 0;

    // 线程池
    public static ExecutorService threadPool = Executors.newCachedThreadPool();


    public DownloadTask(Context context, FileInfo fileInfo, int count, DownloadListener downloadListener) {
        this.context = context;
        this.fileInfo = fileInfo;
        this.threadCount = count;
        this.downloadListener = downloadListener;
        this.threadDao = ThreadDAOImpl.getInstance();
        this.isStop = false;
    }

    /**
     * 启动下载
     */
    public void startDownload() {
        isStop = false;
        List<ThreadInfo> listThreadInfos = threadDao.getThreads(fileInfo.getUrl());
        ThreadInfo threadInfo = null;
        DownloadThread downloadThread = null;
        listDownloadThreads = new ArrayList<DownloadThread>();

        if (listThreadInfos.size() == 0) {
            // 数据库不存在数据
            loadSize = 0;
            calcLoadSize = 0;
            long blockSize = fileInfo.getLength() / threadCount;
            for (int i = 0; i < threadCount; i++) {
                threadInfo = new ThreadInfo(i, fileInfo.getUrl(), i * blockSize, blockSize, 0, Contact.DOWNLOAD_START);
                if(i == threadCount - 1) {
                    // 除不尽余下的大小添加到最后
                    long remainderSize = fileInfo.getLength() % threadCount;
                    threadInfo.setSize(blockSize + remainderSize);
                }
                // 插入数据库
                threadDao.insertThread(threadInfo);
                // 新建下载线程
                downloadThread = new DownloadThread(threadInfo);
                listDownloadThreads.add(downloadThread);
                // 在线程池中启动下载线程
                threadPool.execute(downloadThread);
            }
            // 监听回调
            if(downloadListener != null) {
                downloadListener.onDownloadStart(fileInfo);
            }
        } else {
            // 数据库存在数据
            long fileLength = 0;
            threadCount = 0;
            for(ThreadInfo info : listThreadInfos) {
                // 数据库存在数据则先保存整体文件的已下载长度
                loadSize += info.getLoadSize();
                // 计算文件总长度
                fileLength += info.getSize();
                threadCount++;
                // 未下载完的才启动下载
                if (info.getStatus() != Contact.DOWNLOAD_FINISHED) {
                    info.setStatus(Contact.DOWNLOAD_START);
                    threadDao.updateThread(info);
                    downloadThread = new DownloadThread(info);
                    listDownloadThreads.add(downloadThread);
                    threadPool.execute(downloadThread);
                }
            }
            calcLoadSize = loadSize;
            fileInfo.setLoadSize(loadSize);
            fileInfo.setLength(fileLength);
            // 监听回调
            if(downloadListener != null) {
                downloadListener.onDownloadResumed(fileInfo);
            }
        }

        if(listDownloadThreads.size() == 0) {
            // 没有下载线程，发送下载失败广播
            fileInfo.setStatus(Contact.DOWNLOAD_FAILED);
            sendStatusBroadcast(fileInfo, Contact.ACTION_FAILED);
            // 监听回调
            if(downloadListener != null) {
                downloadListener.onDownloadFailed(fileInfo);
            }
        } else {
            // 发送开始下载广播
            fileInfo.setStatus(Contact.DOWNLOAD_START);
            sendStatusBroadcast(fileInfo, Contact.ACTION_START);
            calcTime = System.currentTimeMillis();
        }
    }

    /**
     * 发送进度广播
     * @param fileInfo
     */
    public void sendUpdateBroadcast(FileInfo fileInfo) {
        Intent intent = new Intent(Contact.ACTION_UPDATE);
        intent.putExtra(Contact.FILE_INFO_KEY, fileInfo);
        context.sendBroadcast(intent);
        // 监听回调
        if(downloadListener != null) {
            downloadListener.onDownloadUpdated(fileInfo);
        }
    }

    /**
     * 发送状态广播
     * @param fileInfo
     * @param actionStatus
     */
    public void sendStatusBroadcast(FileInfo fileInfo, String actionStatus) {
        Intent intent = new Intent(actionStatus);
        intent.putExtra(Contact.FILE_INFO_KEY, fileInfo);
        context.sendBroadcast(intent);
    }

    /**
     * 暂停下载
     */
    public void stopDownLoad() {
        isStop = true;
    }

    /**
     * 判断任务是否停止
     * @return
     */
    public boolean isTaskStop() {
        return isStop;
    }

    /**
     * 取消下载
     */
    public void cancleDownload() {
        isStop = true;
        fileInfo.setStatus(Contact.DOWNLOAD_CANCLE);
        fileInfo.setLoadSize(0);
        // 清空所有活动中的线程
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                // 等待所有线程都结束
                for (DownloadThread thread : listDownloadThreads) {
                    if (thread.isAlive()) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // 删除下载文件
                File tmpFile = new File(DownloadManager.getDownloadDir(), fileInfo.getName() + ".tmp");
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
                fileInfo.setStatus(Contact.DOWNLOAD_CANCLE);
                fileInfo.setLoadSize(0);
                // 发送取消广播
                sendStatusBroadcast(fileInfo, Contact.ACTION_CANCLE);
                // 发送更新广播
                sendUpdateBroadcast(fileInfo);
                // 删除数据库中的记录
                threadDao.deleteThread(fileInfo.getUrl());
            }
        });
        // 监听回调
        if(downloadListener != null) {
            downloadListener.onDownloadCanceled(fileInfo);
        }
    }

    /**
     * 判断是否下载完成
     * @return
     */
    private void checkDownloadFinished() {
        if (loadSize == fileInfo.getLength()) {
            // 表示整个文件都已经下载完成
            fileInfo.setStatus(Contact.DOWNLOAD_FINISHED);
            sendUpdateBroadcast(fileInfo);
            sendStatusBroadcast(fileInfo, Contact.ACTION_FINISHED);
            // 重命名文件
            File tmpFile = new File(DownloadManager.getDownloadDir(), fileInfo.getName() + ".tmp");
            File appFile = new File(DownloadManager.getDownloadDir(), fileInfo.getName());
            tmpFile.renameTo(appFile);
            // 删除数据库中的记录
            threadDao.deleteThread(fileInfo.getUrl());
            // 监听回调
            if(downloadListener != null) {
                downloadListener.onDownloadSuccessed(fileInfo);
            }
            return;
        }
        if(listDownloadThreads.size() == 0) {
            // 如果没有下载完整且没有下载线程,则发送下载停止广播
            fileInfo.setStatus(Contact.DOWNLOAD_PAUSE);
            sendStatusBroadcast(fileInfo, Contact.ACTION_PAUSE);
            // 监听回调
            if(downloadListener != null) {
                downloadListener.onDownloadPaused(fileInfo);
            }
        }
    }

    /**
     * 重新开启线程下载
     * @param currentThread
     * @param threadInfo
     * @return 如果异常次数不超过MAX_EXCEPTION_NUM次则重启一个线程下载
     */
    private boolean restartDownload(DownloadThread currentThread, ThreadInfo threadInfo) {
        Log.e("DownloadTask", "count: " + (exceptionCount + 1));
        if (exceptionCount++ < DownloadManager.getMaxExceptionCount()) {
            // 重启一个线程下载
            DownloadThread newThread = new DownloadThread(threadInfo);
            listDownloadThreads.remove(currentThread);
            listDownloadThreads.add(newThread);
            threadPool.execute(newThread);
            // 监听回调
            if(downloadListener != null) {
                downloadListener.onDownloadRetry(fileInfo);
            }
            return true;
        } else {
            // 超过 MAX_EXCEPTION_COUNT 次则报错退出
            listDownloadThreads.remove(currentThread);
            threadInfo.setStatus(Contact.DOWNLOAD_FAILED);
            threadDao.updateThread(threadInfo);
            fileInfo.setStatus(Contact.DOWNLOAD_FAILED);
            sendStatusBroadcast(fileInfo, Contact.ACTION_FAILED);
            // 监听回调
            if(downloadListener != null) {
                downloadListener.onDownloadFailed(fileInfo);
            }
            return false;
        }
    }

    /**
     * 计算下载速度并更新进度
     */
    private void calcDownloadSpeed() {
        if(System.currentTimeMillis() - calcTime > 500 && !isStop) {
            long time = System.currentTimeMillis();
            // 下载速度为(?b/s)
            long speed = (loadSize - calcLoadSize) * 1000 / (time - calcTime);
            calcLoadSize = loadSize;
            calcTime = time;
            fileInfo.setSpeed(speed);
            sendUpdateBroadcast(fileInfo);
        }
    }


    /**
     * 下载线程
     */
    private class DownloadThread extends Thread {

        private ThreadInfo threadInfo = null;
        private long threadLoadSize = 0;

        public DownloadThread(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
            this.threadLoadSize = threadInfo.getLoadSize();
        }

        @Override
        public void run() {
            HttpURLConnection conn = null;
            RandomAccessFile raFile = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(threadInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                // 当前下载位置
                long currentPos = threadInfo.getStart() + threadInfo.getLoadSize();
                // 最终下载位置
                long endPos = threadInfo.getStart() + threadInfo.getSize() - 1;
//                Log.d("DownloadThread", "threadId(" + threadInfo.getThreadId() + ") Range[" + currentPos + "~" + endPos + "]");
                conn.setRequestProperty("Range", "bytes=" + currentPos + "-" + endPos);

                if (conn.getResponseCode() == 206 || conn.getResponseCode() == 200) {
                    // 获取目标文件
                    File file = new File(DownloadManager.getDownloadDir(), fileInfo.getName() + ".tmp");
                    if(!file.exists()) {
                        file.createNewFile();
                    }
                    raFile = new RandomAccessFile(file, "rw");
                    // 跳到起始位置
                    raFile.seek(currentPos);
                    sendUpdateBroadcast(fileInfo);

                    inputStream = conn.getInputStream();
                    byte[] bytes = new byte[1024 * 4];
                    int readLen;
                    // 计算所有线程的间隔总时间
//                    Log.w("DownloadThread", "thread[" + fileInfo.getName() + "--" + threadInfo.getThreadId() + "]:{"
//                            + threadLoadSize + " ~ " + threadInfo.getSize() + "}");
                    while ((readLen = inputStream.read(bytes)) != -1) {
                        raFile.write(bytes, 0, readLen);
                        loadSize += readLen;
                        fileInfo.setLoadSize(loadSize);
                        threadLoadSize += readLen;
                        threadInfo.setLoadSize(threadLoadSize);
                        threadDao.updateThread(threadInfo);
                        // 500MS执行一次发送进度广播
                        calcDownloadSpeed();
                        if (isStop) {
                            // 停止下载
                            threadInfo.setStatus(Contact.DOWNLOAD_PAUSE);
                            threadDao.updateThread(threadInfo);
                            fileInfo.setStatus(Contact.DOWNLOAD_PAUSE);
                            fileInfo.setSpeed(0);
                            sendStatusBroadcast(fileInfo, Contact.ACTION_PAUSE);
                            listDownloadThreads.remove(this);
                            // 监听回调
                            if(downloadListener != null) {
                                downloadListener.onDownloadPaused(fileInfo);
                            }
                            return;
                        }
                    }

                    if(threadLoadSize != threadInfo.getSize()) {
                        // 如果下载的大小不是需要的大小,则重启一个线程继续下载
                        restartDownload(this, threadInfo);
                    } else {
                        // 到这里表示这个线程已经下载完成
                        threadInfo.setStatus(Contact.DOWNLOAD_FINISHED);
                        threadDao.updateThread(threadInfo);
                        listDownloadThreads.remove(this);
                        // 判断是否已经所有线程都已经下载完成
                        checkDownloadFinished();
                    }
                } else {
                    // 如果返回码有误则重新开启一个线程下载
                    Log.e("DownloadThread", "code : " + conn.getResponseCode());
                    restartDownload(this, threadInfo);
                }
            } catch (IOException e) {
                // 网络异常,则重启一个线程继续下载
                restartDownload(this, threadInfo);
                e.printStackTrace();
            } finally {
                try {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    if (inputStream != null) {
                        inputStream.close();
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
}
