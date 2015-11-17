package com.lib.download.db;

import com.lib.download.contact.ThreadInfo;

import java.util.List;

/**
 * 数据库操作接口
 * Created by 95 on 2015/11/11.
 */
public interface ThreadDAO {

    /**
     * 插入线程信息
     * @param threadInfo
     * @return void
     */
    public void insertThread(ThreadInfo threadInfo);
    /**
     * 删除线程信息
     * @param url
     * @return void
     */
    public void deleteThread(String url);
    /**
     * 更新线程下载进度
     * @param threadInfo
     */
    public void updateThread(ThreadInfo threadInfo);
    /**
     * 查询文件的线程信息
     * @param url
     * @return
     * @return List<ThreadInfo>
     */
    public List<ThreadInfo> getThreads(String url);
    /**
     * 线程信息是否存在
     * @param url
     * @return
     * @return boolean
     */
    public boolean isExists(String url);

    /**
     * 将正在下载和下载失败的线程状态更新为停止
     */
    public void updataToPaused();
}
