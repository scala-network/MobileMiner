package shmutalov.verusminer9000.miner;

public interface IMiningServiceStateListener {
    void onStateChange(Boolean state);
    void onStatusChange(String status, double speed, double max, long accepted, long total, double difficulty, int connection);
}
