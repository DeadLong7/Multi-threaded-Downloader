package com.lib.download.contact;

/**
 * Created by 95 on 2015/11/12.
 */
public class DownloadContact {

    public static final String FILE_INFO_KEY = "FileInfoKey";
    public static final String NOTIFY_ID_KEY = "NotifyIdKey";

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
    public static final String ACTION_PAUSE = "ActionPause";
    public static final String ACTION_CANCLE = "ActionCancle";

    // 通知栏颜色
    public static final int COLOR_NOTIFY_DEFAULT = 0xFF33B5E5;
    public static final int COLOR_NOTIFY_PAUSE = 0xFF888888;
    public static final int COLOR_NOTIFY_FINISH = 0xFF00CC9C;
    public static final int COLOR_NOTIFY_FAILED = 0xFFFF4444;
}
