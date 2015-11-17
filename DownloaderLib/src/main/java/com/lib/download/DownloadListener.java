package com.lib.download;

import com.lib.download.contact.FileInfo;

/**
 * 下载监听器
 * Created by long on 2015/11/16.
 */
public interface DownloadListener {
    /**
     * 开始下载
     * @param fileInfo
     */
    public void onDownloadStart(FileInfo fileInfo);
    /**
     * 更新下载进度
     * @param fileInfo
     */
    public void onDownloadUpdated(FileInfo fileInfo);
    /**
     * 暂停下载
     * @param fileInfo
     */
    public void onDownloadPaused(FileInfo fileInfo);
    /**
     * 继续下载
     * @param fileInfo
     */
    public void onDownloadResumed(FileInfo fileInfo);
    /**
     * 下载成功
     * @param fileInfo
     */
    public void onDownloadSuccessed(FileInfo fileInfo);
    /**
     * 取消下载
     * @param fileInfo
     */
    public void onDownloadCanceled(FileInfo fileInfo);
    /**
     * 下载失败
     * @param fileInfo
     */
    public void onDownloadFailed(FileInfo fileInfo);
    /**
     * 重试下载
     * @param fileInfo
     */
    public void onDownloadRetry(FileInfo fileInfo);
}
