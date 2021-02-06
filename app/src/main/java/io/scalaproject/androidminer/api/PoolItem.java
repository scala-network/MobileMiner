// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.scalaproject.androidminer.Config;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.api.providers.*;

public class PoolItem {

    private int mId = 0;
    private String mPool, mPort, mApiUrl, mPoolUrl, mPoolIP, mStatsURL, mStartUrl, mKey;
    private int mPoolType = 0;
    private Bitmap icon;

    private boolean mIsUserDefined = false;
    public boolean isUserDefined() {
        return mIsUserDefined;
    }
    public void setUserDefined(boolean isUserDefined) {
        mIsUserDefined = isUserDefined;
    }

    private boolean isSelected = false;
    public void setIsSelected(boolean selected) { isSelected = selected; }
    public boolean isSelected() { return isSelected; }

    // API Data
    private boolean isValid = false;
    public void setIsValid(boolean valid) { isValid = valid; }
    public boolean isValid() { return isValid; }

    private int mMiners = 0;
    public void setMiners(int miners) { mMiners = miners; }
    public int getMiners() { return mMiners; }

    private float mHr = -1.0f;
    public void setHr(float hr) { mHr = hr; }
    public float getHr() { return mHr; }

    public PoolItem() {
    }

    public PoolItem(PoolItem poolItem) {
        this.mKey = poolItem.getKey();
        this.mPoolUrl = poolItem.getPoolUrl();
        this.mPort = poolItem.getPort();
        this.mPoolType = poolItem.getPoolType();
        this.icon = poolItem.getIcon();
    }

    public PoolItem(String key, String pool, String port, int poolType, String poolUrl, String poolIP) {
        this.mKey = key;
        this.mPool = pool;
        this.mPort = port;
        this.mPoolUrl = poolUrl;
        this.mPoolIP = poolIP;
        this.mPoolType = poolType;

        switch (mPoolType) {
            case 1:
                this.mStatsURL = poolUrl + "/#/dashboard";
                this.mApiUrl = poolUrl + "/api";
                this.mStartUrl = poolUrl + "/#/help/getting_started";
                break;
            case 2:
                this.mStatsURL = poolUrl + "/#my_stats";
                this.mApiUrl = poolUrl + "/api";
                this.mStartUrl = poolUrl + "/#getting_started";
            case 3:
                this.mStatsURL = poolUrl + "/#stats";
                this.mApiUrl = poolUrl + "/api";
                this.mStartUrl = poolUrl + "/#getting_started";
                break;
            default:
                break;
        }
    }

    public PoolItem(String key, String pool, String port, int poolType, String poolUrl, String poolIP, String apiUrl, String statsUrl, String startUrl) {
        this.mKey = key;
        this.mPool = pool;
        this.mPoolUrl = poolUrl;
        this.mPoolIP = poolIP;
        this.mPort = port;
        this.mApiUrl = apiUrl;
        this.mStatsURL = statsUrl;
        this.mStartUrl = startUrl;
        this.mPoolType = poolType;

        switch (mPoolType) {
            case 1:
                if(startUrl.isEmpty()) {
                    this.mStatsURL = poolUrl + "/#/dashboard";
                }
                if(apiUrl.isEmpty()) {
                    this.mApiUrl = poolUrl + "/api";
                }
                if(statsUrl.isEmpty()) {
                    this.mStartUrl = poolUrl + "/#/help/getting_started";
                }
                break;
            case 2:
                if(startUrl.isEmpty()) {
                    this.mStatsURL = poolUrl + "/api/stats";
                }
                if(apiUrl.isEmpty()) {
                    this.mApiUrl = poolUrl + "/api";
                }
                if(statsUrl.isEmpty()) {
                    this.mStartUrl = poolUrl + "/#getting_started";
                }
            case 3:
                if(startUrl.isEmpty()) {
                    this.mStatsURL = poolUrl + "/#stats";
                }
                if(apiUrl.isEmpty()) {
                    this.mApiUrl = poolUrl + "/api";
                }
                if(statsUrl.isEmpty()) {
                    this.mStartUrl = poolUrl + "/#getting_started";
                }
                break;
            default:
                break;
        }
    }

    public PoolItem(String poolString) {
        if ((poolString == null) || poolString.isEmpty())
            throw new IllegalArgumentException("contact is empty");

        String[] a = poolString.split(":");
        if (a.length == 3) {
            this.mKey = a[0];
            this.mPoolUrl = a[1];
            this.mPool = a[1];

            String[] av = a[2].split("@");
            this.mPort = av[0];

            if(av.length == 2) { // there is an icon
                byte[] b = Base64.decode(av[1], Base64.DEFAULT);
                this.icon = Utils.getCroppedBitmap(BitmapFactory.decodeByteArray(b, 0, b.length));
            }
        } else {
            throw new IllegalArgumentException("Invalid pool string format");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!mKey.isEmpty() && !mPoolUrl.isEmpty() && !mPort.isEmpty()) {
            sb.append(mKey).append(":").append(mPoolUrl).append(":").append(mPort);
        }

        if(icon != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.JPEG, 100, baos);

            byte[] compressImage = baos.toByteArray();
            String sEncodedImage = Base64.encodeToString(compressImage, Base64.DEFAULT);
            sb.append("@").append(sEncodedImage);
        }

        return sb.toString();
    }

    static public PoolItem fromString(String poolString) {
        try {
            return new PoolItem(poolString);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isOfficial() { return getKey().toLowerCase().contains("official"); }

    public Bitmap getIcon() {
        return this.icon;
    }
    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public int getId() {
        return this.mId;
    }

    public void setKey(String key) {
        this.mKey = key;
    }

    public String getKey() {
        return this.mKey;
    }

    public String getPool() {
        if(this.mPoolType == 0) {
            return Config.read("custom_pool");
        }

        return this.mPool;
    }

    public void setPool(String pool) {
        if(this.mPoolType == 0) {
            Config.write("custom_pool", pool);
        } else
            this.mPool = pool;
    }

    public String getDefaultPort() {
        return mPort;
    }

    public String getPortRaw() {
        return mPort;
    }

    public String getPort() {
        String custom_port = Config.read("custom_port");

        if(this.mPoolType == 0) {
            return custom_port;
        }

        if(custom_port.isEmpty() || custom_port.equals(this.mPort)) {
            return this.mPort;
        }

        return custom_port;
    }

    public void setPort(String port) {
        this.mPort = port;
    }

    public String getApiUrl() { return this.mApiUrl;}

    public String getPoolUrl() {
        return this.mPoolUrl;
    }

    public void setPoolUrl(String url) {
        this.mPoolUrl = url;
    }

    public void setPoolIP(String ip) { this.mPoolIP = ip; }

    public String getPoolIP() {
        return this.mPoolIP;
    }

    public String getStatsURL() {
        return this.mStatsURL;
    }

    public String getStartUrl() {
        return this.mStartUrl;
    }

    public int getPoolType() {
        return this.mPoolType;
    }

    public void setPoolType(int type) {
        this.mPoolType = type;
    }

    public String getPoolTypeName() {
        switch (this.mPoolType) {
//            case 0:
//                return "custom";
            case 1:
                return "nodejs-pool";
            case 2:
                return "cryptonote-nodejs-pool";
            case 3:
                return "scala-pool";
            default:
                return "unknown";
        }
    }

    public ProviderAbstract getInterface() {
        ProviderAbstract mPoolInterface;
        switch (this.mPoolType) {
            case 1:
                mPoolInterface = new NodejsPool(this);
                break;
            case 2:
                mPoolInterface = new CryptonoteNodejsPool(this);
                break;
            case 3:
                mPoolInterface = new ScalaPool(this);
                break;
            default:
                mPoolInterface = new NoPool(this);
        }

        return  mPoolInterface;
    }

    public void overwriteWith(PoolItem anotherPool) {
        this.mKey = anotherPool.getKey();
        this.mPoolUrl = anotherPool.getPoolUrl();
        this.mPort = anotherPool.getPort();
    }

    static public Comparator<PoolItem> PoolComparator = new Comparator<PoolItem>() {
        @Override
        public int compare(PoolItem o1, PoolItem o2) {
            if(o1.isOfficial())
                return -1;

            if(o2.isOfficial())
                return 1;

            return o1.getKey().compareToIgnoreCase(o2.getKey());
        }
    };
}
