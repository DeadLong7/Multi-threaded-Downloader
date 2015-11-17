package com.lib.download;

import android.os.Environment;

/**
 * Created by long on 2015/11/17.
 */
public class DownloadConfig {

    // 下载目录
    private String downloadDir;
    // 一个任务最大的线程数
    private int maxThreadNumOfTask;
    // 拆分文件大小的标准
    private int splitSizeOfThread;
    // 一个任务最大允许的异常重连次数(线程太多容易异常?)，超过则下载失败
    private int maxExceptionCount;


    private DownloadConfig() {
        downloadDir = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/downloads/" ;
        maxThreadNumOfTask = 5;
        splitSizeOfThread = 1024 * 1024 * 5;
        maxExceptionCount = 50;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public int getMaxThreadNumOfTask() {
        return maxThreadNumOfTask;
    }

    public int getSplitSizeOfThread() {
        return splitSizeOfThread;
    }

    public int getMaxExceptionCount() {
        return maxExceptionCount;
    }


    /**
     * 构建器
     */
    public static class Builder {
        private DownloadConfig config;

        public Builder() {
            config = new DownloadConfig();
        }

        public DownloadConfig build() {
            return config;
        }

        public void setDownloadDir(String downloadDir) {
            config.downloadDir = downloadDir;
        }

        public void setMaxThreadNumOfTask(int maxThreadNumOfTask) {
            config.maxThreadNumOfTask = maxThreadNumOfTask;
        }

        public void setSplitSizeOfThread(int splitSizeOfThread) {
            config.splitSizeOfThread = splitSizeOfThread;
        }

        public void setMaxExceptionCount(int maxExceptionCount) {
            config.maxExceptionCount = maxExceptionCount;
        }
    }

}
