package shmutalov.verusminer9000.miner;

public interface IMiningServiceStateListener {
    void onStateChange(Boolean state);
    void onStatusChange(String status, float speed, float max, Integer accepted, Integer difficulty, Integer connection);
}
