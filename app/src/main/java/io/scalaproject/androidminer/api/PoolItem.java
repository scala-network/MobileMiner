// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import io.scalaproject.androidminer.Config;
import io.scalaproject.androidminer.api.providers.*;

public class PoolItem {

    private int mId = 0;
    private String mPool, mPort, mApiUrl, mPoolUrl, mPoolIP, mStatsURL, mStartUrl, mKey;
    private int mPoolType = 0;

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

    public void setId(int id) {
        this.mId = id;
    }

    public int getId() {
        return this.mId;
    }

    public String getKey() {
        return this.mKey;
    }

    public String getPool() {
        String custom_pool = Config.read("custom_pool");
        if(this.mPoolType == 0) {
            return custom_pool;
        }

        return this.mPool;
    }

    public String getPort() {
        String custom_port = Config.read("custom_port");

        if(this.mPoolType == 0){
            return custom_port;
        }

        if(custom_port.equals("") || custom_port.equals(this.mPort)) {
            return this.mPort;
        }

        return custom_port;
    }

    public String getApiUrl() { return this.mApiUrl;}

    public String getPoolUrl() {
        return this.mPoolUrl;
    }

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
}
