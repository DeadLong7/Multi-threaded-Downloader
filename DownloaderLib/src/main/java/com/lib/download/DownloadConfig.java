package com.lib.download;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
    // 一个任务最大允许的异常重连次数(线程太多容易异常)，超过则下载失败
    private int maxRedirectCount;
    // 文件保存
    private static final String CACHE_FILE = "Download.dat";


    private DownloadConfig() {
        downloadDir = readDownloadDir();
        if (downloadDir == null) {
            downloadDir = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/downloads/";
        }
        maxThreadNumOfTask = 5;
        splitSizeOfThread = 1024 * 1024 * 5;
        maxRedirectCount = 50;
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

    public int getMaxRedirectCount() {
        return maxRedirectCount;
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

        public Builder setDownloadDir(String downloadDir) {
            config.downloadDir = downloadDir;
            config.saveDownloadDir(downloadDir);
            return this;
        }

        public Builder setMaxThreadNumOfTask(int maxThreadNumOfTask) {
            config.maxThreadNumOfTask = maxThreadNumOfTask;
            return this;
        }

        public Builder setSplitSizeOfThread(int splitSizeOfThread) {
            config.splitSizeOfThread = splitSizeOfThread;
            return this;
        }

        public Builder setMaxExceptionCount(int maxExceptionCount) {
            config.maxRedirectCount = maxExceptionCount;
            return this;
        }
    }

    /**
     * 保存下载目录
     * @param path
     */
    private void saveDownloadDir(String path) {
        String cacheDir = Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "Android" + File.separator + "data" + File.separator + "cache";

        File fileDir = new File(cacheDir);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }

        File cacheFile = new File(fileDir, CACHE_FILE);
        // write
        try {
            if (!cacheFile.exists()) {
                cacheFile.createNewFile(); // 文件不存在则创建新文件
            }
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(new String(path).getBytes("utf-8"));
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取下载路径
     * @return
     */
    private String readDownloadDir() {
        String cachePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "Android" + File.separator + "data" + File.separator + "cache"
                + File.separator + CACHE_FILE;
        File filePath = new File(cachePath);
        if (!filePath.exists()) {
            return null;
        }
        String downloadPath = null;
        try {
            FileInputStream fis = new FileInputStream(filePath);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            downloadPath = new String(bytes, "utf-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return downloadPath;
    }
}
