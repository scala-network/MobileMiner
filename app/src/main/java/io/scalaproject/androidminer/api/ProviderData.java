package io.scalaproject.androidminer.api;

public final class ProviderData {
    public class Network{
        public String lastBlockHeight,difficulty,lastRewardAmount,lastBlockTime, hashrate;
    }

    public class Pool{
        public String lastBlockHeight,difficulty,lastRewardAmount,lastBlockTime, hashrate, blocks, minPayout;
        public int type;
    }

    public class Miner{
        public String hashrate, balance, paid, lastShare, blocks;
    }

    public class Coin{
        public String name, symbol;
        public long units, denominationUnit;
    }

    final public Network network = new Network();
    final public Pool pool = new Pool();
    final public Coin coin = new Coin();
    final public Miner miner = new Miner();
    public boolean isNew = true;

}
