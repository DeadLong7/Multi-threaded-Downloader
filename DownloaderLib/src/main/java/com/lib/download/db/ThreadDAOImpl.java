package com.lib.download.db;

import android.database.sqlite.SQLiteDatabase;

import com.lib.download.contact.Contact;
import com.lib.download.contact.ThreadInfo;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.List;

/**
 * Created by 95 on 2015/11/11.
 */
public class ThreadDAOImpl implements ThreadDAO {

    private SQLiteDatabase db = null;
    private static ThreadDAOImpl instance;

    private ThreadDAOImpl() {
        // 创建数据库
        db = Connector.getDatabase();
    }

    public static ThreadDAOImpl getInstance() {
        if (instance == null) {
            instance = new ThreadDAOImpl();
        }
        return instance;
    }

    @Override
    public void insertThread(ThreadInfo threadInfo) {
        threadInfo.save();
    }

    @Override
    public void deleteThread(String url) {
        DataSupport.deleteAll(ThreadInfo.class, "url = ?", url);
    }

    @Override
    public void updateThread(ThreadInfo threadInfo) {
        threadInfo.updateAll("url = ? and threadId = ?", threadInfo.getUrl(), "" + threadInfo.getThreadId());
    }

    @Override
    public List<ThreadInfo> getThreads(String url) {
        return DataSupport.where("url = ?", url).find(ThreadInfo.class);
    }

    @Override
    public boolean isExists(String url) {
        DataSupport.where("url = ?", url).count(ThreadInfo.class);
        if(DataSupport.where("url = ?", url).count(ThreadInfo.class) > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void updataToPaused() {
        ThreadInfo threadInfo = new ThreadInfo();
        threadInfo.setStatus(Contact.DOWNLOAD_PAUSE);
        threadInfo.updateAll("status = ? or status = ?", ""+Contact.DOWNLOAD_START, ""+Contact.DOWNLOAD_FAILED);
    }
}
