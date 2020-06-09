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

package io.scalaproject.androidminer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.os.AsyncTask;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;

import static android.os.PowerManager.*;

public class MiningService extends Service {

    private static final String LOG_TAG = "MiningSvc";
    private Process process;
    private String configTemplate;
    private String privatePath;
    private OutputReaderThread outputHandler;
    private InputReaderThread inputHandler;
    private ProcessMonitor procMon;
    private PowerManager.WakeLock wl;
    private int accepted = 0;
    private int difficulty = 0;
    private int connection = 0;
    private float speed = 0.0f;
    private float max = 0.0f;
    private String lastAssetPath;
    private String lastOutput = "";
    private static RequestQueue reqQueue;

    private static String API_IP = "https://json.geoiplookup.io/";

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

    private MiningServiceStateListener listener = null;

    public interface MiningServiceStateListener {
        void onStateChange(Boolean state);
        void onStatusChange(String status, float speed, float max, Integer accepted, Integer difficulty, Integer connection);
    }

    public void setMiningServiceStateListener(MiningServiceStateListener listener) {
        if (this.listener != null) this.listener = null;
        this.listener = listener;
    }

    Boolean mMiningServiceState = false;

    private void raiseMiningServiceStateChange(Boolean state) {
        mMiningServiceState = state;
        if (listener != null) listener.onStateChange(state);
    }

    private void raiseMiningServiceStatusChange(String status, float speed, float max, Integer accepted, Integer difficulty, Integer connection) {
        if (listener != null) listener.onStatusChange(status, speed, max, accepted, difficulty, connection);
    }

    public Boolean getMiningServiceState() {
        return mMiningServiceState;
    }

    private void copyMinerFiles() {
        String abi = Tools.getABI();
        String assetPath = "";
        String libraryPath = "";
        String configPath = "";

        Log.i(LOG_TAG, "MINING SERVICE ABI: " + abi);

        String assetExtension = Config.miner_xlarig;

        if (Arrays.asList(Config.SUPPORTED_ARCHITECTURES).contains(abi)) {
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

    class MiningServiceBinder extends Binder {
        MiningService getService() {
            return MiningService.this;
        }
    }

    private static String createCpuConfig(int cores, int threads, int intensity) {
        String cpuConfig = "";

        for (int i = 0; i < cores; i++) {
            for (int j = 0; j < threads; j++) {
                if (!cpuConfig.equals("")) {
                    cpuConfig += ",";
                }
                cpuConfig += "[" + intensity + "," + i + "]";
            }
        }

        return "[" + cpuConfig + "]";
    }

    static class MiningConfig {
        String username, pool, password, algo, assetExtension, cpuConfig, poolHost, poolPort;
        int cores, threads, intensity, legacyThreads, legacyIntensity;
    }

    public MiningConfig newConfig(String address, String password, int cores, int threads, int intensity) {
        MiningConfig config = new MiningConfig();
        PoolItem pi = ProviderManager.getSelectedPool();
        config.username = address;
        config.cores = cores;
        config.threads = threads;
        config.intensity = intensity;
        config.password = password;
        config.algo = Config.algo;
        config.assetExtension = Config.miner_xlarig;

        config.legacyThreads = threads * cores;
        config.legacyIntensity = intensity;

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
        return new MiningServiceBinder();
    }

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

    public static String getIpByHost(PoolItem pi) {
        String hostIP = "";

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(API_IP + pi.getPool(), new JSONObject(), future, future);
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

    public void startMining(MiningConfig config) {
        stopMining();
        new startMiningAsync().execute(config);
    }

    class startMiningAsync extends AsyncTask<MiningConfig, Void, String> {

        String getPoolHost() {

            PoolItem pi = ProviderManager.getSelectedPool();
            assert pi != null;
            return getIpByHost(pi);
        }

        private MiningConfig config;

        protected String doInBackground(MiningConfig... config) {

            try {
                this.config = config[0];
                this.config.pool = getPoolHost();
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

    public void startMiningProcess(MiningConfig config) {
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

            String[] args = {"./" + Config.miner_xlarig};

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

            outputHandler = new MiningService.OutputReaderThread(process.getInputStream(), Config.miner_xlarig);
            outputHandler.start();

            inputHandler = new MiningService.InputReaderThread(process.getOutputStream());
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

    public float getSpeed() {
        return speed;
    }

    public int getAccepted() {
        return accepted;
    }

    public String getOutput() {

        if (outputHandler != null && outputHandler.getOutput() != null) {
            lastOutput =  outputHandler.getOutput().toString();
        }

        return lastOutput;
    }

    public void sendInput(String s) {
        if (inputHandler != null) {
            inputHandler.sendInput(s);
        }
    }

    private class ProcessMonitor extends Thread {

        Process proc;

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
        private InputStream inputStream;
        private StringBuilder output = new StringBuilder();

        OutputReaderThread(InputStream inputStream, String miner) {
            this.inputStream = inputStream;
        }

        private void processLogLine(String line) {
            output.append(line).append(System.getProperty("line.separator"));

            String lineCompare = line.toLowerCase();
            if (lineCompare.contains("accepted")) {
                accepted++;

                if(lineCompare.contains("diff")) {
                    int i = lineCompare.indexOf("diff ") + "diff ".length();
                    int imax = lineCompare.indexOf(" ", i);
                    String diff = lineCompare.substring(i, imax).trim();
                    difficulty = Integer.parseInt(lineCompare.substring(i, imax).trim());;
                }

                if(lineCompare.contains("ms)")) {
                    int i = lineCompare.indexOf("(", lineCompare.length() - 10) + 1;
                    int imax = lineCompare.indexOf("ms)");
                    String conn = lineCompare.substring(i, imax).trim();
                    connection = Integer.parseInt(lineCompare.substring(i, imax).trim());;
                }

            } else if (lineCompare.contains("speed")) {
                String[] split = TextUtils.split(line, " ");
                String tmpSpeed = split[4];
                if (tmpSpeed.equals("n/a")) {
                    tmpSpeed = split[5];
                    if (tmpSpeed.equals("n/a")) {
                        tmpSpeed = split[6];
                    }
                }

                speed = Float.parseFloat(tmpSpeed.trim());

                if (lineCompare.contains("max")) {
                    int i = lineCompare.indexOf("max ") + "max ".length();
                    int imax = lineCompare.indexOf(" ", i);
                    String tmpMax = lineCompare.substring(i, imax).trim();
                    max = Float.parseFloat(tmpMax);
                }
            }

            if (output.length() > Config.logMaxLength) {
                output.delete(0, output.indexOf(Objects.requireNonNull(System.getProperty("line.separator")), Config.logPruneLength) + 1);
            }

            raiseMiningServiceStatusChange(line, speed, max, accepted, difficulty, connection);
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {

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

        private OutputStream outputStream;
        private BufferedWriter writer;

        InputReaderThread(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        public void run() {
            try {
                writer = new BufferedWriter(new OutputStreamWriter(outputStream));

                while (true) {

                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {

                    }

                    if (currentThread().isInterrupted()) return;
                }

            } catch (Exception e) {
                Log.w(LOG_TAG, "exception", e);
            }
        }

        public void sendInput(String s) {

            try {
                writer.write(s);
                writer.flush();
            } catch (Exception e) {
                Log.w(LOG_TAG, "exception", e);
            }
        }
    }
}