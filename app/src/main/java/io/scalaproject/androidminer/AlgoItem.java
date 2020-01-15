// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import java.util.ArrayList;

public class AlgoItem {

    private int mId = 0;
    private String mAlgo = "";
    private ArrayList<MinerItem> mMiners;
    private String mDefaultMiner  = "";

    public AlgoItem(String algo, String defaultMiner, ArrayList<MinerItem> miners) {
        this.mAlgo = algo;
        this.mId = 0;
        mMiners = miners;
        mDefaultMiner = defaultMiner;
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

    public String getDefaultMiner() {
        return this.mDefaultMiner;
    }

    public ArrayList<MinerItem> getMiners() {
        return this.mMiners;
    }
}
