package com.lib.download.contact;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 95 on 2015/11/11.
 */
public class FileInfo implements Parcelable {

    private String url;
    private String name;
    private long length;
    private int status;
    private long loadSize;
    private long speed;

    public FileInfo(int status, String url, String name, long length) {
        this.status = status;
        this.url = url;
        this.name = name;
        this.length = length;
        this.loadSize = 0;
        this.speed = 0;
    }

    public FileInfo(String url, String name) {
        this.url = url;
        this.name = name;
        this.status = 0;
        this.length = 0;
        this.loadSize = 0;
        this.speed = 0;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public long getLoadSize() {
        return loadSize;
    }

    public void setLoadSize(long size) {
        this.loadSize = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", length=" + length +
                ", status=" + status +
                ", loadSize=" + loadSize +
                ", speed=" + speed +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.name);
        dest.writeLong(this.length);
        dest.writeInt(this.status);
        dest.writeLong(this.loadSize);
        dest.writeLong(this.speed);
    }

    protected FileInfo(Parcel in) {
        this.url = in.readString();
        this.name = in.readString();
        this.length = in.readLong();
        this.status = in.readInt();
        this.loadSize = in.readLong();
        this.speed = in.readLong();
    }

    public static final Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
        public FileInfo createFromParcel(Parcel source) {
            return new FileInfo(source);
        }

        public FileInfo[] newArray(int size) {
            return new FileInfo[size];
        }
    };
}
