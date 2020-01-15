package io.scalaproject.androidminer.api;

public final class Data {
    public class Network{
        public String lastBlockHeight,difficulty,lastRewardAmount,lastBlockTime, hashrate;
    }

    public class Pool{
        public String lastBlockHeight,difficulty,lastRewardAmount,lastBlockTime, hashrate, blocks;
    }

    public class Miner{
        public String hashrate, balance, paid, lastShare, blocks;
    }

    public class Coin{
        public String name, symbol;
        public long units, denominationUnit;
    }

    private Network mNetwork = new Network();
    private Pool mPool = new Pool();
    private Coin mCoin = new Coin();
    private Miner mMiner = new Miner();

    public Network getNetwork() {
        return mNetwork;
    }

    public Pool getPool(){
        return mPool;
    }

    public Coin getCoin(){
        return mCoin;
    }
    public Miner getMiner() {return mMiner;}
}
