// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package scala.androidminer;

public class PoolItem {

    private int mId = 0;
    private String mCoin = "";
    private String mPool = "";
    private String mAlgo = "";
    private String mApiUrl = "";
    private String mPoolUrl = "";
    private String mStatsURL = "";
    private String mStartUrl = "";
    private String mKey = "";
    private String mApiUrlMerged = "";

    public PoolItem(String key, String coin, String pool, String algo, String apiUrl, String poolUrl, String statsUrl, String startUrl, String apiUrlMerged) {
        this.mKey = key;
        this.mCoin = coin;
        this.mPool = pool;
        this.mAlgo = algo;
        this.mApiUrl = apiUrl;
        this.mPoolUrl = poolUrl;
        this.mStatsURL = statsUrl;
        this.mStartUrl = startUrl;
        this.mApiUrlMerged = apiUrlMerged;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public int getId() {
        return this.mId;
    }

    public String getKey() {
        return this.mKey;
    }

    public String getCoin() {
        return this.mCoin;
    }

    public String getPool() {
        return this.mPool;
    }

    public String getAlgo() {
        return this.mAlgo;
    }

    public String getApiUrl() {
        return this.mApiUrl;
    }

    public String getPoolUrl() {
        return this.mPoolUrl;
    }

    public String getStatsURL() {
        return this.mStatsURL;
    }

    public String getStartUrl() {
        return this.mStartUrl;
    }

    public String getApiUrlMerged() {
        return this.mApiUrlMerged;
    }

}
