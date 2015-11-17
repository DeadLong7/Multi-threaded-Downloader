package com.lib.download.contact;

import org.litepal.crud.DataSupport;

/**
 * Created by 95 on 2015/11/11.
 */
public class ThreadInfo extends DataSupport {

    private int threadId;
    private String url;
    private long start;
    private long size;
    private long loadSize;
    private int status;

    public ThreadInfo(int id, String url, long start, long size, long loadSize, int status) {
        this.threadId = id;
        this.url = url;
        this.start = start;
        this.size = size;
        this.loadSize = loadSize;
        this.status = status;
    }

    public ThreadInfo() {
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLoadSize() {
        return loadSize;
    }

    public void setLoadSize(long loadSize) {
        this.loadSize = loadSize;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ThreadInfo{" +
                "threadId=" + threadId +
                ", url='" + url + '\'' +
                ", start=" + start +
                ", size=" + size +
                ", loadSize=" + loadSize +
                ", status=" + status +
                '}';
    }
}
