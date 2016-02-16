package com.demo.downloader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lib.download.DownloadListener;
import com.lib.download.DownloadManager;
import com.lib.download.contact.DownloadContact;
import com.lib.download.contact.FileInfo;
import com.lib.download.contact.ThreadInfo;
import com.lib.download.service.DownloadServer;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, DownloadListener {

    private ListView listDownload;
    private Button btnStartOne;
    private Button btnStopOne;
    private Button btnCancleOne;
    private Button btnStartTwo;
    private Button btnStopTwo;
    private Button btnCancleTwo;
    private Button btnStartThree;
    private Button btnStopThree;
    private Button btnCancleThree;
    private Button btnStartFour;
    private Button btnStopFour;
    private Button btnCancleFour;
    private Button btnDeleteDb;

    private void assignViews() {
        listDownload = (ListView) findViewById(R.id.list_download);
        btnStartOne = (Button) findViewById(R.id.btn_start_one);
        btnStopOne = (Button) findViewById(R.id.btn_stop_one);
        btnCancleOne = (Button) findViewById(R.id.btn_cancle_one);
        btnStartTwo = (Button) findViewById(R.id.btn_start_two);
        btnStopTwo = (Button) findViewById(R.id.btn_stop_two);
        btnCancleTwo = (Button) findViewById(R.id.btn_cancle_two);
        btnStartThree = (Button) findViewById(R.id.btn_start_three);
        btnStopThree = (Button) findViewById(R.id.btn_stop_three);
        btnCancleThree = (Button) findViewById(R.id.btn_cancle_three);
        btnStartFour = (Button) findViewById(R.id.btn_start_four);
        btnStopFour = (Button) findViewById(R.id.btn_stop_four);
        btnCancleFour = (Button) findViewById(R.id.btn_cancle_four);
        btnDeleteDb = (Button) findViewById(R.id.btn_delete_db);

        btnStartOne.setOnClickListener(this);
        btnStartTwo.setOnClickListener(this);
        btnStartThree.setOnClickListener(this);
        btnStartFour.setOnClickListener(this);
        btnStopOne.setOnClickListener(this);
        btnStopTwo.setOnClickListener(this);
        btnStopThree.setOnClickListener(this);
        btnStopFour.setOnClickListener(this);
        btnCancleOne.setOnClickListener(this);
        btnCancleTwo.setOnClickListener(this);
        btnCancleThree.setOnClickListener(this);
        btnCancleFour.setOnClickListener(this);
        findViewById(R.id.btn_delete_db).setOnClickListener(this);
    }

    ListAdapter adapter = null;
    private List<FileInfo> listFileInfos;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FileInfo fileInfo = (FileInfo) msg.obj;
            adapter.updataProcess(fileInfo.getUrl(), fileInfo.getLoadSize(), fileInfo.getSpeed(), fileInfo.getLength());
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();

        FileInfo fileInfo1 = new FileInfo("http://www.imooc.com/mobile/mukewang.apk", "imooc.apk");
        FileInfo fileInfo2 = new FileInfo("http://api.gfan.com/market/api/apk?type=WAP&cid=99&uid=-1&pid=vmItBpFSXscI6AwNquamvGUDncvj9Ps7&sid=G+6LoeeD6nDNBHNTaJ+iEA==", "QuanHuang.apk");
        FileInfo fileInfo3 = new FileInfo("http://api.gfan.com/market/api/apk?type=WAP&cid=99&uid=-1&pid=fkLo2yQcZGIFBZz+5E8lJg==&sid=wQLPtMJ2feqH6xZVwr8vtQ==", "huya.apk");
        FileInfo fileInfo4 = new FileInfo("http://api.gfan.com/market/api/apk?type=WAP&cid=99&uid=-1&pid=2LE9UZvKE26s3wJHPWvMXw==&sid=WJ8W80fETN1crzMMQZju4w==", "att.apk");
        listFileInfos = new ArrayList<FileInfo>();
        listFileInfos.add(fileInfo1);
        listFileInfos.add(fileInfo2);
        listFileInfos.add(fileInfo3);
        listFileInfos.add(fileInfo4);
        adapter = new ListAdapter(this);
        listDownload.setAdapter(adapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadContact.ACTION_DOWNLOAD);
        filter.addAction(DownloadContact.ACTION_DOWNLOAD);
        filter.addAction(DownloadContact.ACTION_DOWNLOAD);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainActivity.this, DownloadServer.class));
        unregisterReceiver(mReceiver);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadContact.ACTION_DOWNLOAD.equals(intent.getAction())) {
                FileInfo fileInfo = intent.getParcelableExtra(DownloadContact.FILE_INFO_KEY);
                switch (fileInfo.getStatus()) {
                    case DownloadContact.DOWNLOAD_DOWNLOADING:
                        adapter.updataProcess(fileInfo.getUrl(), fileInfo.getLoadSize(), fileInfo.getSpeed(),
                                fileInfo.getLength());
                        break;
                    case DownloadContact.DOWNLOAD_FINISHED:
                        adapter.updataProcess(fileInfo.getUrl(), fileInfo.getLoadSize(), fileInfo.getSpeed(),
                                fileInfo.getLength());
                        Toast.makeText(MainActivity.this, "Finished", Toast.LENGTH_SHORT).show();
                        break;
                    case DownloadContact.DOWNLOAD_CANCLE:
                        adapter.updataProcess(fileInfo.getUrl(), fileInfo.getLoadSize(), fileInfo.getSpeed(),
                                fileInfo.getLength());
                        Toast.makeText(MainActivity.this, "Cancle", Toast.LENGTH_SHORT).show();
                        break;

                    case DownloadContact.DOWNLOAD_PAUSE:
                        adapter.updataProcess(fileInfo.getUrl(), fileInfo.getLoadSize(), fileInfo.getSpeed(),
                                fileInfo.getLength());
                        Toast.makeText(MainActivity.this, "Pause", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_start_one:
//                DownloadManager.startDownload(this, listFileInfos.get(0), this);
                DownloadManager.startDownload(this, listFileInfos.get(0), this);
                break;
            case R.id.btn_stop_one:
                DownloadManager.stopDownload(listFileInfos.get(0).getUrl());
                break;
            case R.id.btn_cancle_one:
                DownloadManager.cancleDownload(this, listFileInfos.get(0).getUrl(), listFileInfos.get(0).getName());
                break;

            case R.id.btn_start_two:
//                DownloadManager.startDownload(this, listFileInfos.get(1), this);
                DownloadManager.startDLWithNotification(this, listFileInfos.get(1));
                break;
            case R.id.btn_stop_two:
                DownloadManager.stopDownload(listFileInfos.get(1).getUrl());
                break;
            case R.id.btn_cancle_two:
                DownloadManager.cancleDownload(this, listFileInfos.get(1).getUrl(), listFileInfos.get(1).getName());
                break;

            case R.id.btn_start_three:
//                DownloadManager.startDownload(this, listFileInfos.get(2), this);
                DownloadManager.startDLWithNotification(this, listFileInfos.get(2));
                break;
            case R.id.btn_stop_three:
                DownloadManager.stopDownload(listFileInfos.get(2).getUrl());
                break;
            case R.id.btn_cancle_three:
                DownloadManager.cancleDownload(this, listFileInfos.get(2).getUrl(), listFileInfos.get(2).getName());
                break;

            case R.id.btn_start_four:
                DownloadManager.startDownload(this, listFileInfos.get(3), this);
                break;
            case R.id.btn_stop_four:
                DownloadManager.stopDownload(listFileInfos.get(3).getUrl());
                break;
            case R.id.btn_cancle_four:
                DownloadManager.cancleDownload(this, listFileInfos.get(3).getUrl(), listFileInfos.get(3).getName());
                break;

            case R.id.btn_delete_db:
                DataSupport.deleteAll(ThreadInfo.class);
                break;
        }
    }

    @Override
    public void onDownloadWaiting(FileInfo fileInfo) {
        
    }

    @Override
    public void onDownloadStart(FileInfo fileInfo) {
        Log.d("MainActivity", "onDownloadStart: \n" + fileInfo);
    }

    @Override
    public void onDownloadUpdated(FileInfo fileInfo) {
//        Log.w("MainActivity", "onDownloadUpdated: \n" + fileInfo);
        mHandler.obtainMessage(0, fileInfo).sendToTarget();
    }

    @Override
    public void onDownloadPaused(FileInfo fileInfo) {
        Log.d("MainActivity", "onDownloadPaused: \n" + fileInfo);
    }

    @Override
    public void onDownloadResumed(FileInfo fileInfo) {
        Log.d("MainActivity", "onDownloadResumed: \n" + fileInfo);
    }

    @Override
    public void onDownloadSuccessed(FileInfo fileInfo) {
        Log.d("MainActivity", "onDownloadSuccessed: \n" + fileInfo);
    }

    @Override
    public void onDownloadCanceled(FileInfo fileInfo) {
        Log.d("MainActivity", "onDownloadCanceled: \n" + fileInfo);
    }

    @Override
    public void onDownloadFailed(FileInfo fileInfo) {
        Log.d("MainActivity", "onDownloadFailed: \n" + fileInfo);
    }

    @Override
    public void onDownloadRetry(FileInfo fileInfo) {
        Log.d("MainActivity", "onDownloadRetry: \n" + fileInfo);
    }


    class ListAdapter extends BaseAdapter {

        private Context context;

        public ListAdapter(Context context) {
            this.context = context;
        }

        public void updataProcess(String url, long size, long speed, long fileLength) {
//            Log.d("ListAdapter", "url:" + url + " size:" + size + " filelen:" + fileLength);
            for (FileInfo fileInfo : listFileInfos) {
                if (url.equals(fileInfo.getUrl())) {
                    fileInfo.setLoadSize(size);
                    fileInfo.setLength(fileLength);
                    fileInfo.setSpeed(speed);
                    notifyDataSetChanged();
                    break;
                }
            }
        }

        @Override
        public int getCount() {
            return listFileInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return listFileInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final FileInfo fileInfo = listFileInfos.get(position);
            ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.list_download, null);
                viewHolder = new ViewHolder(convertView);
                viewHolder.tvfileName.setText(fileInfo.getName());
                viewHolder.pbprogress.setMax(100);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            long speed;
            if(fileInfo.getSpeed() > 1024) {
                speed = fileInfo.getSpeed() / 1024;
                viewHolder.tvProgress.setText(speed + "kb/s");
            } else {
                speed = fileInfo.getSpeed();
                viewHolder.tvProgress.setText(speed + "b/s");
            }
//            viewHolder.tvProgress.setText(fileInfo.getLoadSize() + " / " + fileInfo.getLength());
            if(fileInfo.getLength() != 0) {
                viewHolder.pbprogress.setProgress((int) (fileInfo.getLoadSize() * 100 / fileInfo.getLength()));
            }

            return convertView;
        }

        public class ViewHolder {
            public final TextView tvfileName;
            public final ProgressBar pbprogress;
            public final TextView tvProgress;
            public final View root;

            public ViewHolder(View root) {
                tvfileName = (TextView) root.findViewById(R.id.tv_fileName);
                pbprogress = (ProgressBar) root.findViewById(R.id.pb_progress);
                tvProgress = (TextView) root.findViewById(R.id.tv_process);
                this.root = root;
            }
        }
    } ;
}
