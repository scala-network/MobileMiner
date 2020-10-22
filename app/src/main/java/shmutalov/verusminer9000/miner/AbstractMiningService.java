package shmutalov.verusminer9000.miner;
import android.app.Service;

public abstract class AbstractMiningService extends Service {
    public abstract MiningConfig newConfig(String address, String password, String workername, int cores, int threads, int intensity);
    public abstract void startMining(MiningConfig config);
    public abstract void stopMining();
    public abstract void pauseMiner();
    public abstract void resumeMiner();
    public abstract void toggleHashrate();
    public abstract String[] getSupportedArchitectures();
    public abstract void setMiningServiceStateListener(IMiningServiceStateListener listener);
    public abstract Boolean getMiningServiceState();
    public abstract String getOutput();
}
