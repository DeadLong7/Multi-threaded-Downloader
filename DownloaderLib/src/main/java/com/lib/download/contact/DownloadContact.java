package com.lib.download.contact;

/**
 * Created by 95 on 2015/11/12.
 */
public class DownloadContact {

    public static final String FILE_INFO_KEY = "FileInfoKey";

    // 未下载
    public static final int DOWNLOAD_NORMAL = 0;
    // 等待下载
    public static final int DOWNLOAD_WAIT = 1;
    // 正在下载
    public static final int DOWNLOAD_DOWNLOADING = 2;
    // 暂停下载
    public static final int DOWNLOAD_PAUSE = 3;
    // 下载失败
    public static final int DOWNLOAD_FAILED = 4;
    // 下载完成
    public static final int DOWNLOAD_FINISHED = 5;
    // 取消下载
    public static final int DOWNLOAD_CANCLE = 6;
    // 开始下载
    public static final int DOWNLOAD_START = 7;
    // 已经安装
    public static final int DOWNLOAD_INSTALLED = 8;
    // 安装中
    public static final int DOWNLOAD_INSTALLING = 9;


    // 广播Action
    public static final String ACTION_DOWNLOAD = "ActionDownload";
    public static final String ACTION_START = "ActionStart";

    // 默认值
//    public static final int MAX_THREAD_NUM_OF_TASK = 5;    // 一个任务最大的线程数
//    public static final int SPLIT_SIZE_OF_THREAD = 1024 * 1024 * 5; // 拆分文件大小的标准，5MB
}
