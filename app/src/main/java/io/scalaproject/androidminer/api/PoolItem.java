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

import io.scalaproject.androidminer.Config;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.api.providers.*;

public class PoolItem {

    private String mPool, mPort, mApiUrl, mUrl, mIP, mStatsURL, mKey;
    private int mPoolType = 0;
    private Bitmap icon;
    private ArrayList<String> mPorts;

    private boolean mIsUserDefined = false;

    public PoolItem() {

    }

    public boolean isUserDefined() {
        return mIsUserDefined;
    }
    public void setUserDefined(boolean isUserDefined) {
        mIsUserDefined = isUserDefined;
    }

    private boolean isSelected = false;
    public void setIsSelected(boolean selected) { isSelected = selected; }
    public boolean isSelected() { return isSelected; }

    private String mSelectedPort = "";
    public void setSelectedPort(String port) { mSelectedPort = port; }
    public String getSelectedPort() { return mSelectedPort.isEmpty() ? mPort : mSelectedPort; }

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

    public ArrayList<String> getPorts () { return mPorts; }

    public PoolItem(PoolItem poolItem) {
        this.mKey = poolItem.getKey();
        this.mUrl = poolItem.getPoolUrl();
        this.mPort = poolItem.getPort();
        this.mPoolType = poolItem.getPoolType();
        this.icon = poolItem.getIcon();
    }

    public PoolItem(String key, String pool, String port, ArrayList<String> ports, int poolType, String poolUrl, String poolIP) {
        this.mKey = key;
        this.mPool = pool;
        this.mPort = port;
        this.mPorts = ports;
        this.mUrl = poolUrl;
        this.mIP = poolIP;
        this.mPoolType = poolType;

        switch (mPoolType) {
            case 1:
                this.mStatsURL = poolUrl + "/#/dashboard";
                this.mApiUrl = poolUrl + "/api";
                break;
            case 2:
                this.mStatsURL = poolUrl + "/#my_stats";
                this.mApiUrl = poolUrl + "/api";
            case 3:
                this.mStatsURL = poolUrl + "/#stats";
                this.mApiUrl = poolUrl + "/api";
                break;
            default:
                break;
        }
    }

    public PoolItem(String key, String pool, String port, ArrayList<String> ports, int poolType, String poolUrl, String poolIP, String apiUrl, String statsUrl, String startUrl) {
        this.mKey = key;
        this.mPool = pool;
        this.mUrl = poolUrl;
        this.mIP = poolIP;
        this.mPort = port;
        this.mPorts = ports;
        this.mApiUrl = apiUrl;
        this.mStatsURL = statsUrl;
        this.mPoolType = poolType;

        switch (mPoolType) {
            case 1:
                if(startUrl.isEmpty()) {
                    this.mStatsURL = poolUrl + "/#/dashboard";
                }
                if(apiUrl.isEmpty()) {
                    this.mApiUrl = poolUrl + "/api";
                }
                break;
            case 2:
                if(startUrl.isEmpty()) {
                    this.mStatsURL = poolUrl + "/api/stats";
                }
                if(apiUrl.isEmpty()) {
                    this.mApiUrl = poolUrl + "/api";
                }
                break;
            case 3:
                if(startUrl.isEmpty()) {
                    this.mStatsURL = poolUrl + "/#stats";
                }
                if(apiUrl.isEmpty()) {
                    this.mApiUrl = poolUrl + "/api";
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
            this.mUrl = a[1];
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
        if (!mKey.isEmpty() && !mUrl.isEmpty() && !mSelectedPort.isEmpty()) {
            sb.append(mKey).append(":").append(mUrl).append(":").append(mSelectedPort);
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

    public void setKey(String key) {
        this.mKey = key;
    }

    public String getKey() {
        return this.mKey;
    }

    public String getPool() {
        return this.mPool;
    }

    public void setPool(String pool) {
        this.mPool = pool;
    }

    public String getDefaultPort() {
        return mPort;
    }

    public String getPort() {
        String custom_port = Config.read(Config.CONFIG_CUSTOM_PORT);

        if(custom_port.isEmpty() || custom_port.equals(this.mPort)) {
            return this.mPort;
        }

        return custom_port;
    }

    public String getApiUrl() { return this.mApiUrl;}

    public String getPoolUrl() {
        return this.mUrl;
    }

    public void setPoolUrl(String url) {
        this.mUrl = url;
    }

    public String getPoolIP() {
        return this.mIP;
    }

    public String getStatsURL() {
        return this.mStatsURL;
    }

    public int getPoolType() {
        return this.mPoolType;
    }

    public String getPoolTypeName() {
        switch (this.mPoolType) {
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

    public String getWalletURL(String walletAddress) {
        switch (this.mPoolType) {
            case 1: // nodejs-pool
                return this.mUrl;
            case 2: // cryptonote-nodejs-pool
                return this.mUrl;
            case 3: // scala-pool
                return this.mUrl + "?wallet=" + walletAddress + "#worker_stats";
            default:
                return this.mUrl;
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
        this.mUrl = anotherPool.getPoolUrl();
        this.mPort = anotherPool.getPort();
    }

    static public final Comparator<PoolItem> PoolComparator = new Comparator<PoolItem>() {
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
