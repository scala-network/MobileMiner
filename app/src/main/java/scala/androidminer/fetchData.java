// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.
package scala.androidminer;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

import org.ocpsoft.prettytime.PrettyTime;

public class fetchData extends AsyncTask<Void, Void, Void> {

    private static final String LOG_TAG = "MiningSvc";

    private String dataStatsAddress = "";
    private String dataStatsAddressMerged = "";

    private String dataStatsNetwork = "";
    private String dataStatsNetworkMerged = "";

    private String dataParsedAddress = "";
    private String dataParsedAddressMerged = "";

    private String dataParsedNetwork = "";
    private String dataParsedNetworkMerged = "";

    private int Error = 0;
    private int ErrorMerged = 0;

    private int ErrorNetwork = 0;
    private int ErrorNetworkMerged = 0;

    private statsChangeListener listener = null;

    public interface statsChangeListener {
        public void onStatsChange(String addressStats, String networkStats);
    }

    public void setStatsChangeListener(statsChangeListener listener) {
        if (this.listener != null) this.listener = null;
        this.listener = listener;
    }

    private void raiseStatsChange(String addressStats, String networkStats) {
        if (listener != null) listener.onStatsChange(addressStats, networkStats);
    }

    @Override
    protected Void doInBackground(Void... voids) {

        PrettyTime pTime = new PrettyTime();

        long coinUnits = 1;
        long denominationUnit = 1;
        String Symbol = "";
        String coinName = "";

        long coinUnitsMerged = 1;
        long denominationUnitMerged = 1;
        String SymbolMerged = "";
        String coinNameMerged = "";

        try {

            dataStatsNetwork = fetchJSON(StatsFragment.apiUrl + "/stats");
            Log.i(LOG_TAG, dataStatsNetwork);

            JSONObject joStats = new JSONObject(dataStatsNetwork);
            JSONObject joStatsConfig = joStats.getJSONObject("config");
            JSONObject joStatsNetwork = joStats.getJSONObject("network");

            coinName = joStatsConfig.optString("coin").toUpperCase();
            coinUnits = tryParseLong(joStatsConfig.optString("coinUnits"), 1L);
            Symbol = joStatsConfig.optString("symbol");
            denominationUnit = tryParseLong(joStatsConfig.optString("denominationUnit"), 1L);

            String networkHeight = joStatsNetwork.optString("height");
            String networkDifficulty = getReadableHashRateString(joStatsNetwork.optLong("difficulty"));
            String networkLastBlock = pTime.format(new Date(joStatsNetwork.optLong("timestamp") * 1000));
            String lastReward = parseCurrency(joStatsNetwork.optString("reward", "0"), coinUnits, denominationUnit, Symbol);

            dataParsedNetwork = coinName + "\n"
                    + "Height: " + networkHeight + "\n"
                    + "Difficulty: " + networkDifficulty + "\n"
                    + "Last Block: " + networkLastBlock + "\n"
                    + "Reward: " + lastReward;

            ErrorNetwork = 0;

        } catch (JSONException e) {
            ErrorNetwork = 1;
            Log.i(LOG_TAG, e.toString());
            e.printStackTrace();
        }

        try {

            if (StatsFragment.apiUrlMerged.equals("") == false) {

                dataStatsNetworkMerged = fetchJSON(StatsFragment.apiUrlMerged + "/stats");
                Log.i(LOG_TAG, dataStatsNetworkMerged);

                JSONObject joStatsMerged = new JSONObject(dataStatsNetworkMerged);
                JSONObject joStatsConfigMerged = joStatsMerged.getJSONObject("config");
                JSONObject joStatsNetworkMerged = joStatsMerged.getJSONObject("network");

                coinNameMerged = joStatsConfigMerged.optString("coin").toUpperCase();
                coinUnitsMerged = tryParseLong(joStatsConfigMerged.optString("coinUnits"), 1L);
                SymbolMerged = joStatsConfigMerged.optString("symbol");
                denominationUnitMerged = tryParseLong(joStatsConfigMerged.optString("denominationUnit"), 1L);

                String networkHeightMerged = joStatsNetworkMerged.optString("height");
                String networkDifficultyMerged = getReadableHashRateString(joStatsNetworkMerged.optLong("difficulty"));
                String networkLastBlockMerged = pTime.format(new Date(joStatsNetworkMerged.optLong("timestamp") * 1000));
                String lastRewardMerged = parseCurrency(joStatsNetworkMerged.optString("reward", "0"), coinUnitsMerged, denominationUnitMerged, SymbolMerged);
                dataParsedNetworkMerged = coinNameMerged + "\n"
                        + "Height: " + networkHeightMerged + "\n"
                        + "Difficulty: " + networkDifficultyMerged + "\n"
                        + "Last Block: " + networkLastBlockMerged + "\n"
                        + "Reward: " + lastRewardMerged;

                ErrorNetworkMerged = 0;
            }

        } catch (JSONException e) {
            ErrorNetworkMerged = 1;
            Log.i(LOG_TAG, e.toString());
            e.printStackTrace();
        }

        String walletParts[] = null;

        try {

            walletParts = StatsFragment.wallet.split(":");

            dataStatsAddress = fetchJSON(StatsFragment.apiUrl + "/stats_address?address=" + walletParts[0]);
            Log.i(LOG_TAG, dataStatsAddress);

            JSONObject joStatsAddress = new JSONObject(dataStatsAddress);
            JSONObject joStatsAddressStats = joStatsAddress.getJSONObject("stats");

            String hashRate = joStatsAddressStats.optString("hashrate", "0 H") + "/s";
            String balance = parseCurrency(joStatsAddressStats.optString("balance", "0"), coinUnits, denominationUnit, Symbol);
            String paid = parseCurrency(joStatsAddressStats.optString("paid", "0"), coinUnits, denominationUnit, Symbol);
            String lastShare = pTime.format(new Date(joStatsAddressStats.optLong("lastShare") * 1000));
            String blocks = String.valueOf(tryParseLong(joStatsAddressStats.optString("blocks"), 0L));

            dataParsedAddress = coinName + "\n"
                    + "Hash Rate: " + hashRate + "\n"
                    + "Balance: " + balance + "\n"
                    + "Paid: " + paid + "\n"
                    + "Last Share: " + lastShare + "\n"
                    + "Blocks Found: " + blocks;

            Error = 0;

        } catch (JSONException e) {
            Error = 1;
            Log.i(LOG_TAG, e.toString());
            e.printStackTrace();
        }

        try {

            if (StatsFragment.apiUrlMerged.equals("") == false) {

                if (walletParts.length > 1) {
                    dataStatsAddressMerged = fetchJSON(StatsFragment.apiUrlMerged + "/stats_address?address=" + walletParts[1]);
                    Log.i(LOG_TAG, dataStatsAddressMerged);

                    JSONObject joStatsAddressMerged = new JSONObject(dataStatsAddressMerged);
                    JSONObject joStatsAddressStatsMerged = joStatsAddressMerged.getJSONObject("stats");

                    String hashRateMerged = joStatsAddressStatsMerged.optString("hashrate", "0 H") + "/s";
                    String balanceMerged = parseCurrency(joStatsAddressStatsMerged.optString("balance", "0"), coinUnitsMerged, denominationUnitMerged, SymbolMerged);
                    String paidMerged = parseCurrency(joStatsAddressStatsMerged.optString("paid", "0"), coinUnitsMerged, denominationUnitMerged, SymbolMerged);
                    String lastShareMerged = pTime.format(new Date(joStatsAddressStatsMerged.optLong("lastShare") * 1000));
                    String blocksMerged = String.valueOf(tryParseLong(joStatsAddressStatsMerged.optString("blocks"), 0L));

                    dataParsedAddressMerged = coinNameMerged + "\n"
                            + "Hash Rate: " + hashRateMerged + "\n"
                            + "Balance: " + balanceMerged + "\n"
                            + "Paid: " + paidMerged + "\n"
                            + "Last Share: " + lastShareMerged + "\n"
                            + "Blocks Found: " + blocksMerged;

                } else {
                    //merged mining but no wallet
                    dataParsedAddressMerged = "No address";
                }

                ErrorMerged = 0;
            }

        } catch (JSONException e) {
            ErrorMerged = 1;
            Log.i(LOG_TAG, e.toString());
            e.printStackTrace();
        }

        return null;
    }

    private String parseCurrency(String value, long coinUnits, long denominationUnits, String symbol) {

        double base = tryParseDouble(value, 1D);
        double d = base / (double) coinUnits;
        double d2 = Math.round(d * (double) denominationUnits) / (double) denominationUnits;

        Log.i(LOG_TAG, "parseCurrency: d: " + d + " d2: " + d2);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);

        return nf.format(d2) + " " + symbol.toUpperCase();
    }

    private Long tryParseLong(String s, Long fallback) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private Double tryParseDouble(String s, Double fallback) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String getReadableHashRateString(long hashrate) {

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

    private String fetchJSON(String url) {

        String data = "";
        try {

            URL urlFetch = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlFetch.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                data = data + line;
            }

        } catch (MalformedURLException e) {
            Log.i(LOG_TAG, e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.i(LOG_TAG, e.toString());
            e.printStackTrace();
        }

        return data;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        String sDataAddress = "";
        if (this.dataParsedAddress != null && Error == 0) {
            sDataAddress = this.dataParsedAddress;
        }

        if (this.dataParsedAddressMerged != null && ErrorMerged == 0) {
            if (sDataAddress.equals("") == false) sDataAddress += "\n\n";
            sDataAddress += this.dataParsedAddressMerged;
        }

        String sDataNetwork = "";
        if (this.dataParsedNetwork != null && ErrorNetwork == 0) {
            sDataNetwork = this.dataParsedNetwork;
        }
        if (this.dataParsedNetworkMerged != null && ErrorNetworkMerged == 0) {
            if (sDataNetwork.equals("") == false) sDataNetwork += "\n\n";
            sDataNetwork += this.dataParsedNetworkMerged;
        }

        raiseStatsChange(sDataAddress, sDataNetwork);
    }
}
