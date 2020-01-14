package scala.androidminer.pools.api;

import scala.androidminer.Config;
import scala.androidminer.pools.PoolItem;
import scala.androidminer.pools.PoolManager;

public abstract class PoolTypeAbstract {

    protected PoolItem mPoolItem;

    public PoolTypeAbstract(PoolItem poolItem){
        mPoolItem = poolItem;
    }

    abstract public String getBlockHeight();

    final public String getWalletAddress(){
        return Config.read("address");
    }

//    abstract public void setFixDifficulty(int diff);

//    abstract public void get

}
