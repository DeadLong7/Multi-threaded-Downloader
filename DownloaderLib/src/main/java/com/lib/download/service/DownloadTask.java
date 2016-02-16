package com.lib.download.service;

import android.content.Context;
import android.content.Intent;

import com.lib.download.DownloadListener;
import com.lib.download.DownloadManager;
import com.lib.download.contact.DownloadContact;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

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
    private AtomicLong loadSize = new AtomicLong();
    // 停止下载任务的标志位
    private boolean isStop = false;
    // 取消下载任务的标志位
    private boolean isCancle = false;
    // 线程总数
    private int threadCount;
    // 保存所有活动的线程,线程安全
    private CopyOnWriteArrayList<DownloadThread> listDownloadThreads;
    // 线程返回异常的次数
    private int exceptionCount = 0;
    // 下载监听器
    private DownloadListener downloadListener;
    // 已下载数据大小和时间、次数，用来计算下载速度
    private long calcLoadSize = 0;
    private long calcTime = 0;
    private int calcCount = 2;

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
        listDownloadThreads = new CopyOnWriteArrayList<DownloadThread>();
        // 必须先初始化为0
        loadSize.set(0);

        if (listThreadInfos.size() == 0) {
            // 数据库不存在数据
            calcLoadSize = 0;
            long blockSize = fileInfo.getLength() / threadCount;
            for (int i = 0; i < threadCount; i++) {
                threadInfo = new ThreadInfo(i, fileInfo.getUrl(), i * blockSize, blockSize, 0, DownloadContact.DOWNLOAD_DOWNLOADING);
                if (i == threadCount - 1) {
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
            fileInfo.setStatus(DownloadContact.DOWNLOAD_START);
            downloadListener.onDownloadStart(fileInfo);
            // 监听回调
            if (downloadListener != null) {
                sendStatusBroadcast(fileInfo);
            }
        } else {
            // 数据库存在数据
            long fileLength = 0;
            threadCount = 0;
            for (ThreadInfo info : listThreadInfos) {
                // 数据库存在数据则先保存整体文件的已下载长度
                loadSize.getAndAdd(info.getLoadSize());
                // 计算文件总长度
                fileLength += info.getSize();
                threadCount++;
                // 未下载完的才启动下载
                if (info.getStatus() != DownloadContact.DOWNLOAD_FINISHED) {
                    info.setStatus(DownloadContact.DOWNLOAD_DOWNLOADING);
                    threadDao.updateThread(info);
                    downloadThread = new DownloadThread(info);
                    listDownloadThreads.add(downloadThread);
                    threadPool.execute(downloadThread);
                }
            }
            calcLoadSize = loadSize.get();
            fileInfo.setLoadSize(calcLoadSize);
            fileInfo.setLength(fileLength);
            fileInfo.setStatus(DownloadContact.DOWNLOAD_START);
            sendStatusBroadcast(fileInfo);
            // 监听回调
            if (downloadListener != null) {
                downloadListener.onDownloadStart(fileInfo);
                downloadListener.onDownloadResumed(fileInfo);
            }
        }

        if (listDownloadThreads.size() == 0) {
            // 没有下载线程，发送下载失败广播
            fileInfo.setStatus(DownloadContact.DOWNLOAD_FAILED);
            sendStatusBroadcast(fileInfo);
            // 监听回调
            if (downloadListener != null) {
                downloadListener.onDownloadFailed(fileInfo);
            }
        } else {
            // 发送开始下载广播
            fileInfo.setStatus(DownloadContact.DOWNLOAD_DOWNLOADING);
            sendStatusBroadcast(fileInfo);
            calcTime = System.currentTimeMillis();
        }
    }

    /**
     * 发送进度广播
     *
     * @param fileInfo
     */
    public void sendUpdateBroadcast(FileInfo fileInfo) {
        fileInfo.setStatus(DownloadContact.DOWNLOAD_DOWNLOADING);
        Intent intent = new Intent(DownloadContact.ACTION_DOWNLOAD);
        intent.putExtra(DownloadContact.FILE_INFO_KEY, fileInfo);
        context.sendBroadcast(intent);
        // 监听回调
        if (downloadListener != null) {
            downloadListener.onDownloadUpdated(fileInfo);
        }
    }

    /**
     * 发送状态广播
     *
     * @param fileInfo
     */
    public void sendStatusBroadcast(FileInfo fileInfo) {
        Intent intent = new Intent(DownloadContact.ACTION_DOWNLOAD);
        intent.putExtra(DownloadContact.FILE_INFO_KEY, fileInfo);
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
     *
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
        isCancle = true;
        fileInfo.setStatus(DownloadContact.DOWNLOAD_CANCLE);
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
                fileInfo.setStatus(DownloadContact.DOWNLOAD_CANCLE);
                fileInfo.setLoadSize(0);
                // 发送取消广播
                sendStatusBroadcast(fileInfo);
                // 发送更新广播
//                sendUpdateBroadcast(fileInfo);
                // 删除数据库中的记录
                threadDao.deleteThread(fileInfo.getUrl());
                // 监听回调
                if (downloadListener != null) {
                    downloadListener.onDownloadCanceled(fileInfo);
                }
            }
        });
    }

    /**
     * 判断是否下载完成
     *
     * @return
     */
    private synchronized void checkDownloadFinished() {
        if (loadSize.get() == fileInfo.getLength()) {
            // 表示整个文件都已经下载完成
            fileInfo.setStatus(DownloadContact.DOWNLOAD_FINISHED);
            fileInfo.setSpeed(0);
            sendStatusBroadcast(fileInfo);
            // 重命名文件
            File tmpFile = new File(DownloadManager.getDownloadDir(), fileInfo.getName() + ".tmp");
            File appFile;
            if (fileInfo.getName().endsWith(".apk")) {
                appFile = new File(DownloadManager.getDownloadDir(), fileInfo.getName());
            } else {
                appFile = new File(DownloadManager.getDownloadDir(), fileInfo.getName() + ".apk");
            }
            tmpFile.renameTo(appFile);
            // 删除数据库中的记录
            threadDao.deleteThread(fileInfo.getUrl());
            // 监听回调
            if (downloadListener != null) {
                downloadListener.onDownloadSuccessed(fileInfo);
            }
            return;
        }
        if (listDownloadThreads.size() == 0) {
            // 如果没有下载完整且没有下载线程,则发送下载停止广播
            fileInfo.setStatus(DownloadContact.DOWNLOAD_PAUSE);
            sendStatusBroadcast(fileInfo);
            // 监听回调
            if (downloadListener != null) {
                downloadListener.onDownloadPaused(fileInfo);
            }
        }
    }

    /**
     * 重新开启线程下载
     *
     * @param currentThread
     * @param threadInfo
     * @return 如果异常次数不超过MAX_EXCEPTION_NUM次则重启一个线程下载
     */
    private synchronized boolean restartDownload(DownloadThread currentThread, ThreadInfo threadInfo) {
//        Log.e("DownloadTask", "count: " + (exceptionCount + 1));
        if (exceptionCount++ < DownloadManager.getMaxExceptionCount()) {
            // 重启一个线程下载
            DownloadThread newThread = new DownloadThread(threadInfo);
            listDownloadThreads.remove(currentThread);
            listDownloadThreads.add(newThread);
            threadPool.execute(newThread);
            // 监听回调
            if (downloadListener != null) {
                downloadListener.onDownloadRetry(fileInfo);
            }
            return true;
        } else {
            // 超过 MAX_EXCEPTION_COUNT 次则报错退出
            listDownloadThreads.remove(currentThread);
            threadInfo.setStatus(DownloadContact.DOWNLOAD_FAILED);
            threadDao.updateThread(threadInfo);
            fileInfo.setStatus(DownloadContact.DOWNLOAD_FAILED);
            sendStatusBroadcast(fileInfo);
            // 监听回调
            if (downloadListener != null) {
                downloadListener.onDownloadFailed(fileInfo);
            }
            return false;
        }
    }

    /**
     * 计算下载速度并更新进度
     */
    private synchronized void calcDownloadSpeed(long curCalcTime) {
        if (curCalcTime - calcTime > 300 && !isStop) {
            // 3次更新一次下载速度
            if ((++calcCount) > 2) {
                // 下载速度为(b/s)
                long speed = (loadSize.get() - calcLoadSize) * 1000 / (curCalcTime - calcTime);
                fileInfo.setSpeed(speed);
                calcCount = 0;
                updateDatabase();
            }
            calcLoadSize = loadSize.get();
            calcTime = curCalcTime;
            sendUpdateBroadcast(fileInfo);
        }
    }

    /**
     * 检测停止下载状态，并发送广播
     */
    private synchronized void checkStopDownload() {
        // 如果为取消状态则不发送暂停广播
        if (listDownloadThreads.size() == 0 && !isCancle) {
            fileInfo.setStatus(DownloadContact.DOWNLOAD_PAUSE);
            fileInfo.setSpeed(0);
            sendStatusBroadcast(fileInfo);
            // 监听回调
            if (downloadListener != null) {
                downloadListener.onDownloadPaused(fileInfo);
            }
        }
    }

    /**
     * 更新数据库
     */
    private void updateDatabase() {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (DownloadThread thread : listDownloadThreads) {
                    thread.updateThreadInfo();
                }
            }
        });
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

        /**
         * 更新线程数据库，开个接口给外部调用
         */
        public void updateThreadInfo() {
            threadDao.updateThread(threadInfo);
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
                    if (!file.exists()) {
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
                        long time = System.currentTimeMillis();
                        raFile.write(bytes, 0, readLen);
                        loadSize.getAndAdd(readLen);
//                        loadSize += readLen;
                        fileInfo.setLoadSize(loadSize.get());
                        threadLoadSize += readLen;
                        threadInfo.setLoadSize(threadLoadSize);
                        // 500MS执行一次发送进度广播
                        calcDownloadSpeed(time);
                        if (isStop) {
                            // 停止下载
                            threadInfo.setStatus(DownloadContact.DOWNLOAD_PAUSE);
                            threadDao.updateThread(threadInfo);
                            listDownloadThreads.remove(this);
                            checkStopDownload();
                            return;
                        }
                    }

                    if (threadLoadSize != threadInfo.getSize()) {
                        // 如果下载的大小不是需要的大小,则重启一个线程继续下载
                        restartDownload(this, threadInfo);
                    } else {
                        // 到这里表示这个线程已经下载完成
                        threadInfo.setStatus(DownloadContact.DOWNLOAD_FINISHED);
                        threadDao.updateThread(threadInfo);
                        listDownloadThreads.remove(this);
                        // 判断是否已经所有线程都已经下载完成
                        checkDownloadFinished();
                    }
                } else {
                    // 如果返回码有误则重新开启一个线程下载
//                    Log.e("DownloadThread", "code : " + conn.getResponseCode());
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
                    if (raFile != null) {
                        raFile.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
