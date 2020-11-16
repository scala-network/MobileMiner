/*
 *  Monero Miner App (c) 2018 Uwe Post
 *  based on the XMRig Monero Miner https://github.com/xmrig/xmrig
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 * /
 */
// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000.miner;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import shmutalov.verusminer9000.Config;
import shmutalov.verusminer9000.Tools;
import shmutalov.verusminer9000.api.PoolItem;
import shmutalov.verusminer9000.api.ProviderManager;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

/**
 * ccminer executable based veruscoin mining service
 */
public class VerusBinMiningService extends AbstractMiningService {

    private static final String LOG_TAG = "MiningSvc";

    private static final String miner_ccminer = "ccminer";
    private static final String algo = "verus";
    private static final String[] SUPPORTED_ARCHITECTURES = {"armeabi-v7a", "arm64-v8a", "x86_64"};

    private Process process;
    private String configTemplate;
    private String privatePath;
    private OutputReaderThread outputHandler;
    private InputReaderThread inputHandler;
    private ProcessMonitor procMon;
    private PowerManager.WakeLock wl;
    private long accepted = 0;
    private long total = 0;
    private double difficulty = 0.0;
    private int connection = 0;
    private double speed = 0.0f;
    private double max = 0.0f;
    private String lastAssetPath;
    private String lastOutput = "";
    private MiningConfig lastConfig;
    private static RequestQueue reqQueue;

    private static final String JSON_GEOIPLOOKUP_API_URL = "https://json.geoiplookup.io/";

    public class VerusBinMiningServiceBinder extends AbstractMiningServiceBinder {
        @Override
        public AbstractMiningService getService() {
            return VerusBinMiningService.this;
        }
    }

    @Override
    public String[] getSupportedArchitectures() {
        return SUPPORTED_ARCHITECTURES;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        privatePath = getFilesDir().getAbsolutePath();
        Tools.deleteDirectoryContents(new File(privatePath));

        reqQueue = Volley.newRequestQueue(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private IMiningServiceStateListener listener = null;

    @Override
    public void setMiningServiceStateListener(IMiningServiceStateListener listener) {
        if (this.listener != null) this.listener = null;
        this.listener = listener;
    }

    Boolean mMiningServiceState = false;

    private void raiseMiningServiceStateChange(Boolean state) {
        mMiningServiceState = state;
        if (listener != null) listener.onStateChange(state);
    }

    private void raiseMiningServiceStatusChange(String status, double speed, double max, long accepted, long total, double difficulty, int connection) {
        if (listener != null) listener.onStatusChange(status, speed, max, accepted, total, difficulty, connection);
    }

    @Override
    public Boolean getMiningServiceState() {
        return mMiningServiceState;
    }

    private void copyMinerFiles() {
        String abi = Tools.getABI();
        String assetPath = "";
        String libraryPath = "";
        String configPath = "";

        Log.i(LOG_TAG, "MINING SERVICE ABI: " + abi);

        String assetExtension = miner_ccminer;

        if (Arrays.asList(SUPPORTED_ARCHITECTURES).contains(abi)) {
            assetPath = assetExtension + "/" + abi;
            libraryPath = "lib" + "/" + abi;
            configPath = assetExtension + "/config.json";
        } else {
            Log.i(LOG_TAG, "NO ASSET PATH");
        }

        Log.i(LOG_TAG, "ASSET PATH: " + assetPath);
        Log.i(LOG_TAG, "LAST ASSET PATH: " + lastAssetPath);
        Log.i(LOG_TAG, "ABI: " + abi);

        if (!assetPath.equals(lastAssetPath)) {
            Tools.deleteDirectoryContents(new File(privatePath));
            Tools.copyDirectoryContents(this, libraryPath, privatePath);
            Tools.copyDirectoryContents(this, assetPath, privatePath);
            configTemplate = Tools.loadConfigTemplate(this, configPath);
            Tools.logDirectoryFiles(new File(privatePath));
            lastAssetPath = assetPath;
        }
    }

    private static String createCpuConfig(int cores, int threads, int intensity) {
        StringBuilder cpuConfig = new StringBuilder();

        for (int core = 0; core < cores; core++) {
            for (int thread = 0; thread < threads; thread++) {
                if (!cpuConfig.toString().equals("")) {
                    cpuConfig.append(",");
                }
                cpuConfig
                        .append("[")
                        .append(intensity)
                        .append(",")
                        .append(core)
                        .append("]");
            }
        }

        return "[" + cpuConfig + "]";
    }

    @Override
    public MiningConfig newConfig(String address, String password, String workername, int cores, int threads, int intensity) {
        MiningConfig config = new MiningConfig();
        PoolItem pi = ProviderManager.getSelectedPool();

        config.algo = algo;
        config.username = address;
        config.cores = cores;
        config.threads = threads;
        config.intensity = intensity;
        config.password = password;
        config.workername = workername;
        config.assetExtension = miner_ccminer;

        assert pi != null;
        config.poolHost = pi.getPool();
        config.poolPort = pi.getPort();

        config.cpuConfig = createCpuConfig(cores, threads, intensity);

        return config;
    }

    @Override
    public void onDestroy() {
        stopMining();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new VerusBinMiningServiceBinder();
    }

    @Override
    public void stopMining() {
        if (outputHandler != null) {
            outputHandler.interrupt();
            outputHandler = null;
        }

        if (inputHandler != null) {
            inputHandler.interrupt();
            inputHandler = null;
        }

        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    private static String getIpByHost(PoolItem pi) {
        String hostIP = "";

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(JSON_GEOIPLOOKUP_API_URL + pi.getPool(), new JSONObject(), future, future);
        reqQueue.add(request);

        try {
            JSONObject response = future.get(5, TimeUnit.SECONDS); // Sync call
            hostIP = response.optString("ip");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        if(hostIP.isEmpty())
            hostIP = pi.getPoolIP();

        return hostIP + ":" + pi.getPort();
    }

    @Override
    public void startMining(MiningConfig config) {
        lastConfig = config;
        stopMining();
        new startMiningAsync().execute(lastConfig);
    }

    class startMiningAsync extends AsyncTask<MiningConfig, Void, String> {
        private MiningConfig config;

        protected String doInBackground(MiningConfig... config) {

            try {
                this.config = config[0];

                PoolItem pi = ProviderManager.getSelectedPool();
                assert pi != null;

                this.config.pool = pi.getPool();
                this.config.poolHost = pi.getPool();
                this.config.poolPort = pi.getPort();

                return "success";
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            copyMinerFiles();
            startMiningProcess(this.config);
        }
    }

    private void startMiningProcess(MiningConfig config) {
        Log.i(LOG_TAG, "starting...");

        if (process != null) {
            process.destroy();
            process = null;
        }

        if (wl != null) {
            if (wl.isHeld()) {
                wl.release(); //Wakelock
            }
            wl = null;
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PARTIAL_WAKE_LOCK, "app:sleeplock");
        wl.acquire(10*60*1000L /*10 minutes*/);

        try {
            Tools.writeConfig(configTemplate, config, privatePath);

            String[] args = {
                    "./" + miner_ccminer,
                    "--no-banner",
                    "--no-color",
                    "-a", algo,
                    "-o", config.poolHost + ":" + config.poolPort,
                    "-u", config.workername.isEmpty() ? config.username : config.username + "." + config.workername,
                    "-p", config.password,
                    "-t", String.valueOf(config.cores)
            };

            Log.i(LOG_TAG, TextUtils.join(" ", args));

            ProcessBuilder pb = new ProcessBuilder(args);

            pb.directory(new File(privatePath));

            pb.environment().put("LD_LIBRARY_PATH", privatePath);

            pb.redirectErrorStream();

            accepted = 0;
            difficulty = 0;
            connection = 0;
            speed = -1.0f;
            max = -1.0f;
            lastOutput = "";

            process = pb.start();

            outputHandler = new VerusBinMiningService.OutputReaderThread(process.getInputStream());
            outputHandler.start();

            inputHandler = new VerusBinMiningService.InputReaderThread(process.getOutputStream());
            inputHandler.start();

            if (procMon != null) {
                procMon.interrupt();
                procMon = null;
            }
            procMon = new ProcessMonitor(process);
            procMon.start();

        } catch (Exception e) {
            Log.e(LOG_TAG, "exception:", e);
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            process = null;
        }
    }

    public double getSpeed() {
        return speed;
    }

    public long getAccepted() {
        return accepted;
    }

    @Override
    public String getOutput() {

        if (outputHandler != null && outputHandler.getOutput() != null) {
            lastOutput =  outputHandler.getOutput().toString();
        }

        return lastOutput;
    }

    @Override
    public void pauseMiner() {
        stopMining();
        //if (inputHandler != null) {
            //inputHandler.sendInput("p");
        //}
    }

    @Override
    public void resumeMiner() {
        if (lastConfig != null) {
            startMining(lastConfig);
        }
        //if (inputHandler != null) {
            //inputHandler.sendInput("r");
        //}
    }

    @Override
    public void toggleHashrate() {
        //if (inputHandler != null) {
            //inputHandler.sendInput("h");
        //}
    }

    private class ProcessMonitor extends Thread {
        final Process proc;

        ProcessMonitor(Process proc) {
            this.proc = proc;
        }

        public void run() {
            try {

                raiseMiningServiceStateChange(true);
                if (proc != null) {
                    proc.waitFor();
                    Log.i(LOG_TAG, "process exit: " + proc.exitValue());
                }
                raiseMiningServiceStateChange(false);

            } catch (Exception e) {
                // assume problem with process and not running
                raiseMiningServiceStateChange(false);
                Log.e(LOG_TAG, "exception:", e);
            }
        }
    }

    private class OutputReaderThread extends Thread {
        private final InputStream inputStream;
        private final StringBuilder output = new StringBuilder();

        OutputReaderThread(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        private void processLogLine(String line) {
            output.append(line).append(System.getProperty("line.separator"));

            // [2020-10-23 14:30:33] accepted: 2/3 (diff 274824.194), 1472.33 kH/s yes!

            int acceptedIdxStart = line.indexOf("accepted");
            if (acceptedIdxStart > 0) {
                // accepted shares
                int accStart = acceptedIdxStart + 10;
                int accEnd = line.indexOf("/", accStart);
                String acc = line.substring(accStart, accEnd);
                accepted = Long.parseLong(acc);

                // total shares
                int totalEnd = line.indexOf(" ", accEnd);
                String tot = line.substring(accEnd + 1, totalEnd);
                total = Long.parseLong(tot);

                // difficulty
                int diffStart = totalEnd + 7;
                int diffEnd = line.indexOf(")", diffStart);
                String diff = line.substring(diffStart, diffEnd);
                difficulty = Double.parseDouble(diff);

                // hashrate
                int speedStart = diffEnd + 3;
                int speedEnd = line.indexOf(" ", speedStart);
                String spd = line.substring(speedStart, speedEnd);
                speed = Double.parseDouble(spd);

                if (speed > max) {
                    max = speed;
                }
            }

            if (output.length() > Config.logMaxLength) {
                output.delete(0, output.indexOf(Objects.requireNonNull(System.getProperty("line.separator")), Config.logPruneLength) + 1);
            }
//
            raiseMiningServiceStatusChange(line, speed, max, accepted, total, difficulty, connection);
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    //line = line.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
                    Log.i(LOG_TAG, "miner: " + line);

                    processLogLine(line);

                    if (currentThread().isInterrupted()) return;
                }

            } catch (IOException e) {
                Log.w(LOG_TAG, "exception", e);
            }
        }

        public StringBuilder getOutput() {
            return output;
        }
    }

    private class InputReaderThread extends Thread {

        //private final OutputStream outputStream;
        //private BufferedWriter writer;

        InputReaderThread(OutputStream outputStream) {
            //this.outputStream = outputStream;
        }

        public void run() {
            try {
                //writer = new BufferedWriter(new OutputStreamWriter(outputStream));

                while (true) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        // ignore
                    }

                    if (currentThread().isInterrupted()) return;
                }

            } catch (Exception e) {
                Log.w(LOG_TAG, "exception", e);
            }
        }

//        public void sendInput(String s) {

//            try {
//                writer.write(s);
//                writer.flush();
//            } catch (Exception e) {
//                Log.w(LOG_TAG, "exception", e);
//            }
//        }
    }
}