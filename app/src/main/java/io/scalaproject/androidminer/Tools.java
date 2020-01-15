/*
 *  File Tools for Monero Miner
 *  (c) 2018 Uwe Post
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
// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class Tools {

    private static final String LOG_TAG = "MiningSvc";

    public static String loadConfigTemplate(Context context, String path) {
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

    public static void copyFile(Context context, String assetFilePath, String localFilePath) {
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

    public static void copyDirectoryContents(Context context, String assetFilePath, String localFilePath) {

        String[] folder;

        try {
            folder = context.getAssets().list(assetFilePath);
        } catch (Exception e) {
            return;
        }

        for (final String f : folder) {

            Boolean isDirectory = isAssetDirectory(context,assetFilePath + "/" + f);

            if (isDirectory == false) {
                Log.i(LOG_TAG, "copy file: source:" + assetFilePath + "/" + f + " dest:" + localFilePath + "/" + f);
                File file = new File(localFilePath + "/" + f);
                if (file.exists() && file.isFile()) {
                    Log.i(LOG_TAG, "copy file delete: source:" + assetFilePath + "/" + f + " dest:" + localFilePath + "/" + f);
                    file.delete();
                }
                copyFile(context, assetFilePath + "/" + f, localFilePath + "/" + f);
            } else if (isDirectory == true) {
                Log.i(LOG_TAG, "make directory: source:" + assetFilePath + "/" + f + " dest:" + localFilePath + "/" + f);
                File dir = new File(localFilePath + "/" + f);
                dir.mkdir();
                copyDirectoryContents(context, assetFilePath + "/" + f, localFilePath + "/" + f);
            }
        }

    }

    private static boolean isAssetDirectory(Context context, String pathInAssetsDir){

        InputStream inputStream = null;
        Boolean isDirectory = false;
        try {
            inputStream = context.getAssets().open(pathInAssetsDir);
        }  catch(IOException e) {
            isDirectory = true;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
            }
        }
        return isDirectory;
    }

    public static void logDirectoryFiles(final File folder) {
        for (final File f : folder.listFiles()) {

            if (f.isDirectory()) {
                logDirectoryFiles(f);
            }

            if (f.isFile()) {
                Log.i(LOG_TAG, f.getName());
            }

        }
    }

    public static void deleteDirectoryContents(final File folder) {
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

    public static void writeConfig(String configTemplate, MiningService.MiningConfig miningConfig, String privatePath) {

        String config = configTemplate
                .replace("$algo$", miningConfig.algo)
                .replace("$url$", miningConfig.pool)
                .replace("$username$", miningConfig.username)
                .replace("$pass$", miningConfig.pass)

                .replace("$legacythreads$", Integer.toString(miningConfig.legacyThreads))
                .replace("$legacyintensity$", Integer.toString(miningConfig.legacyIntensity))
                .replace("$legacyalgo$", miningConfig.algo)

                .replace("$urlhost$", miningConfig.poolHost)
                .replace("$urlport$", miningConfig.poolPort)

                .replace("$cpuconfig$", miningConfig.cpuConfig);


        Log.i(LOG_TAG, "CONFIG: " + config);

        PrintWriter writer = null;

        try {
            writer = new PrintWriter(new FileOutputStream(privatePath + "/config.json"));
            writer.write(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) writer.close();
        }
    }

    public static Map<String, String> getCPUInfo() {

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

    public static String getABI() {
        String abiString;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            abiString = Build.SUPPORTED_ABIS[0];
        } else {
            abiString = Build.CPU_ABI;
        }
        return abiString.toLowerCase().trim();
    }

    static public String getCurrentCPUTemperature() {
        String file = readFile("/sys/devices/virtual/thermal/thermal_zone0/temp", '\n',new byte[4096]);
        float output = 0.0f;
        if (file != null) {
            output = (float) Long.parseLong(file);
        }
        if(output > 0.0f && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            output = output / 1000;
        }

        return String.format("%.1f", output);
    }

    static public String readFile(String file, char endChar,byte[] mBuffer) {

        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
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
        } catch (java.io.FileNotFoundException e) {
        } catch (java.io.IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (java.io.IOException e) {
                }
            }
            StrictMode.setThreadPolicy(savedPolicy);
        }
        return null;
    }

    static public String parseCurrency(String value, long coinUnits, long denominationUnits, String symbol) {

        double base = tryParseDouble(value, 1D);
        double d = base / (double) coinUnits;
        double d2 = Math.round(d * (double) denominationUnits) / (double) denominationUnits;

        Log.i(LOG_TAG, "parseCurrency: d: " + d + " d2: " + d2);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);

        return nf.format(d2) + " " + symbol.toUpperCase();
    }

    static public  Long tryParseLong(String s, Long fallback) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    static public  Double tryParseDouble(String s, Double fallback) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    static public  String getReadableHashRateString(long hashrate) {

        BigDecimal bn = new BigDecimal(hashrate);
        BigDecimal bnThousand = new BigDecimal(1000);

        int i = 0;
        String byteUnits[] = {"H", "KH", "MH", "GH", "TH", "PH"};

        while (bn.compareTo(bnThousand) > 0) {
            bn = bn.divide(bnThousand);
            i++;
        }

        DecimalFormat decimalFormat = new DecimalFormat("0.##");

        return decimalFormat.format(bn) + ' ' + byteUnits[i];
    }

}
