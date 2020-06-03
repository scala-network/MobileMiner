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

import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class Tools {

    private static final String LOG_TAG = "MiningSvc";

    static String loadConfigTemplate(Context context, String path) {
        try {
            StringBuilder buf = new StringBuilder();
            InputStream json = context.getAssets().open(path);
            BufferedReader in = new BufferedReader(new InputStreamReader(json, "UTF-8"));
            String str;

            while ((str = in.readLine()) != null) {
                buf.append(str);
            }

            in.close();
            return buf.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyFile(Context context, String assetFilePath, String localFilePath) {
        try {
            InputStream in = context.getAssets().open(assetFilePath);
            FileOutputStream out = new FileOutputStream(localFilePath);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();

            File bin = new File(localFilePath);
            bin.setExecutable(true);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void copyDirectoryContents(Context context, String assetFilePath, String localFilePath) {

        String[] folder;

        try {
            folder = context.getAssets().list(assetFilePath);
        } catch (Exception e) {
            return;
        }

        assert folder != null;
        for (final String f : folder) {

            boolean isDirectory = isAssetDirectory(context,assetFilePath + "/" + f);

            if (!isDirectory) {
                Log.i(LOG_TAG, "copy file: source:" + assetFilePath + "/" + f + " dest:" + localFilePath + "/" + f);
                File file = new File(localFilePath + "/" + f);
                if (file.exists() && file.isFile()) {
                    Log.i(LOG_TAG, "copy file delete: source:" + assetFilePath + "/" + f + " dest:" + localFilePath + "/" + f);
                    file.delete();
                }
                copyFile(context, assetFilePath + "/" + f, localFilePath + "/" + f);
            } else if (isDirectory) {
                Log.i(LOG_TAG, "make directory: source:" + assetFilePath + "/" + f + " dest:" + localFilePath + "/" + f);
                File dir = new File(localFilePath + "/" + f);
                dir.mkdir();
                copyDirectoryContents(context, assetFilePath + "/" + f, localFilePath + "/" + f);
            }
        }
    }

    static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }

        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }
    private static boolean isAssetDirectory(Context context, String pathInAssetsDir){

        InputStream inputStream = null;
        boolean isDirectory = false;
        try {
            inputStream = context.getAssets().open(pathInAssetsDir);
        }  catch(IOException e) {
            isDirectory = true;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ignored) {
            }
        }

        return isDirectory;
    }

    static void logDirectoryFiles(final File folder) {
        for (final File f : folder.listFiles()) {

            if (f.isDirectory()) {
                logDirectoryFiles(f);
            }

            if (f.isFile()) {
                Log.i(LOG_TAG, f.getName());
            }
        }
    }

    static void deleteDirectoryContents(final File folder) {
        for (final File f : folder.listFiles()) {

            if (f.isDirectory()) {
                Log.i(LOG_TAG, "Delete Directory: " + f.getName());
                deleteDirectoryContents(f);
            }

            if (f.isFile()) {
                Log.i(LOG_TAG, "Delete File: " + f.getName());
                f.delete();
            }
        }
    }

    static void writeConfig(String configTemplate, MiningService.MiningConfig miningConfig, String privatePath) {

        String config = configTemplate
                .replace("$algo$", miningConfig.algo)
                .replace("$url$", miningConfig.pool)
                .replace("$username$", miningConfig.username)
                .replace("$pass$", miningConfig.password)

                .replace("$legacythreads$", Integer.toString(miningConfig.legacyThreads))
                .replace("$legacyintensity$", Integer.toString(miningConfig.legacyIntensity))
                .replace("$legacyalgo$", miningConfig.algo)

                .replace("$urlhost$", miningConfig.poolHost)
                .replace("$urlport$", miningConfig.poolPort)

                .replace("$cpuconfig$", miningConfig.cpuConfig);

        Log.i(LOG_TAG, "CONFIG: " + config);

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(privatePath + "/config.json"))) {
            writer.write(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Map<String, String> getCPUInfo() {
        Map<String, String> output = new HashMap<>();

        try {
            BufferedReader br = null;
            br = new BufferedReader(new FileReader("/proc/cpuinfo"));

            String str;

            while ((str = br.readLine()) != null) {

                String[] data = str.split(":");

                if (data.length > 1) {

                    String key = data[0].trim().replace(" ", "_");
                    if (key.equals("model_name")) key = "cpu_model";

                    String value = data[1].trim();

                    if (key.equals("cpu_model"))
                        value = value.replaceAll("\\s+", " ");

                    output.put(key, value);
                }
            }

            br.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    static String getABI() {
        String abiString;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            abiString = Build.SUPPORTED_ABIS[0];
        } else {
            abiString = Build.CPU_ABI;
        }

        return abiString.toLowerCase().trim();
    }

    static float getCurrentCPUTemperature() {
        float output = 0.0f;

        String file = readFile("/sys/devices/virtual/thermal/thermal_zone0/temp", '\n',new byte[4096]);
        if (file != null) {
            output = (float) Long.parseLong(file);
        }

        if(output > 0.0f && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            output = output / 1000;
        }

        if(output > 100.0f) // ugly temporary workaround
            output = 0.0f;

        return output;
    }

    static float getCPUUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" +");  // Split on one or more spaces

            long idle1 = Long.parseLong(toks[4]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            try {
                Thread.sleep(360);
            } catch (Exception e) {}

            reader.seek(0);
            load = reader.readLine();
            reader.close();

            toks = load.split(" +");

            long idle2 = Long.parseLong(toks[4]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    private static String readFile(String file, char endChar, byte[] mBuffer) {
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();

        try (FileInputStream is = new FileInputStream(file)) {
            int len = is.read(mBuffer);
            is.close();

            if (len > 0) {
                int i;
                for (i = 0; i < len; i++) {
                    if (mBuffer[i] == endChar) {
                        break;
                    }
                }
                return new String(mBuffer, 0, i);
            }
        } catch (java.io.FileNotFoundException ignored) {
        } catch (IOException ignored) {
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }

        return null;
    }

    static public String parseCurrency(String value, long coinUnits, long denominationUnits, String symbol) {
        double d2 = parseCurrencyFloat(value, coinUnits, denominationUnits);

        Log.i(LOG_TAG, "parseCurrency: d2: " + d2);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);

        return nf.format(d2) + " " + symbol.toUpperCase();
    }

    static public float parseCurrencyFloat(String value, long coinUnits, long denominationUnits) {
        double base = tryParseDouble(value);
        double d = base / (float) coinUnits;
        float d2 = Math.round(d * (float) denominationUnits) / (float) denominationUnits;

        return d2;
    }

    static public Long tryParseLong(String s, Long fallback) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 1.0;
        }
    }

    static public String getReadableHashRateString(long hashrate) {
        BigDecimal bn = new BigDecimal(hashrate);
        BigDecimal bnThousand = new BigDecimal(1000);

        int i = 0;
        String[] byteUnits = {"H", "KH", "MH", "GH", "TH", "PH"};

        while (bn.compareTo(bnThousand) > 0) {
            bn = bn.divide(bnThousand);
            i++;
        }

        DecimalFormat decimalFormat = new DecimalFormat("0.##");

        return decimalFormat.format(bn) + ' ' + byteUnits[i];
    }
}
