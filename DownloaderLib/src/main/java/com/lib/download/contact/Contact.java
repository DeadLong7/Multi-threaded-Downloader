package com.lib.download.contact;

/**
 * Created by 95 on 2015/11/12.
 */
public class Contact {

    public static final String FILE_INFO_KEY = "FileInfoKey";

    // 等待下载
    public static final int DOWNLOAD_WAIT = 0;
    // 开始下载
    public static final int DOWNLOAD_START = 1;
    // 暂停下载
    public static final int DOWNLOAD_PAUSE = 2;
    // 下载失败
    public static final int DOWNLOAD_FAILED = 3;
    // 下载完成
    public static final int DOWNLOAD_FINISHED = 4;
    // 取消下载
    public static final int DOWNLOAD_CANCLE = 5;

    // 广播Action
    public static final String ACTION_START = "ActionStart";
    public static final String ACTION_PAUSE = "ActionStop";
    public static final String ACTION_UPDATE = "ActionUpdate";
    public static final String ACTION_FINISHED = "ActionFinished";
    public static final String ACTION_FAILED = "ActionException";
    public static final String ACTION_CANCLE = "ActionCancle";

    // 默认值
//    public static final int MAX_THREAD_NUM_OF_TASK = 5;    // 一个任务最大的线程数
//    public static final int SPLIT_SIZE_OF_THREAD = 1024 * 1024 * 5; // 拆分文件大小的标准，5MB
}
