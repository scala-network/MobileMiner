// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package scala.androidminer;

import java.util.ArrayList;

public class Config {

    public static final Config settings = new Config();

    public static final int defaultPoolIndex = 1;
    public static final String defaultWallet = "";
    public static final String defaultPassword = "";

    public static final String miner_xlarig = "xlarig";

    public static final String asset_xlarig = "xlarig";

    public static final Integer logMaxLength = 50000;
    public static final Integer logPruneLength = 1000;

    private ArrayList<PoolItem> mPools = new ArrayList<PoolItem>();
    private ArrayList<AlgoItem> mAlgos = new ArrayList<AlgoItem>();


    public Config() {

//        mAlgos.add(new AlgoItem("rx/0", miner_xmrig, new ArrayList<MinerItem>() {
//            {
//                add(new MinerItem(miner_xmrig, "rx/0", asset_xmrig));
//            }
//        }));

        mAlgos.add(new AlgoItem("defyx", miner_xlarig, new ArrayList<MinerItem>() {
            {
                add(new MinerItem(miner_xlarig, "defyx", asset_xlarig));
            }
        }));

        //User Defined
        mPools.add(new PoolItem("custom", "custom", "", "", "", "", "", "", ""));

        // Scala Official pool
        mPools.add(new PoolItem(
                        "xla",
                        "Scalaproject.io (Official Pool)",
                        "mine.scalaproject.io:3333",
                        "defyx",
                        "https://pool.scalaproject.io/api",
                        "https://pool.scalaproject.io",
                        "https://pool.scalaproject.io/#my_stats",
                        "https://pool.scalaproject.io/#getting_started",
                        ""
                )
        );

        // Another Scala pool : Miner.Rocks
        mPools.add(new PoolItem(
                        "xla",
                        "Miner.Rocks",
                        "stellite.miner.rocks:5003",
                        "defyx",
                        "https://stellite.miner.rocks/api",
                        "https://stellite.miner.rocks",
                        "https://stellite.miner.rocks/#my_stats",
                        "https://stellite.miner.rocks/#getting_started",
                        ""
                )
        );

        // Another Scala pool : HeroMiners
        mPools.add(new PoolItem(
                        "xla",
                        "HeroMiners",
                        "scala.herominers.com:10130",
                        "defyx",
                        "https://scala.herominers.com/api",
                        "https://scala.herominers.com/",
                        "https://scala.herominers.com/#my_stats",
                        "https://scala.herominers.com/#getting_started",
                        ""
                )
        );
        // Another Scala pool : GNTL
        mPools.add(new PoolItem(
                        "xla",
                        "GNTL",
                        "xla.pool.gntl.co.uk:3333",
                        "defyx",
                        "https://xla.pool.gntl.co.uk/api",
                        "https://xla.pool.gntl.co.uk",
                        "https://xla.pool.gntl.co.uk/#my_stats",
                        "https://xla.pool.gntl.co.uk/#getting_started",
                        ""
                )
        );

        /*
        mPools.add(new PoolItem(
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                )
        );
        */
    }

    public PoolItem[] getPools() {
        return this.mPools.toArray(new PoolItem[mPools.size()]);
    }

    public AlgoItem[] getAlgos() {
        return this.mAlgos.toArray(new AlgoItem[mAlgos.size()]);
    }

}
