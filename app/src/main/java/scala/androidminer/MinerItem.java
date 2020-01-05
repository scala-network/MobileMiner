// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package scala.androidminer;

public class MinerItem {
    private int mId = 0;
    private String mAlgo = "";
    private String mAssetExtenstion = "";
    private String mMiner = "";

    public MinerItem(String miner, String algo, String assetExtension) {
        this.mAlgo = algo;
        this.mAssetExtenstion = assetExtension;
        this.mId = 0;
        this.mMiner = miner;
    }

    public void setId(int id){
        this.mId = id;
    }

    public int getId(){
        return this.mId;
    }

    public String getAlgo() {
        return this.mAlgo;
    }

    public String getAssetExtension() {
        return this.mAssetExtenstion;
    }

    public String getMiner(){
        return this.mMiner;
    }

}
