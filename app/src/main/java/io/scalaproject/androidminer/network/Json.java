// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Json {

    private static final String LOG_TAG = "MiningSvc";

    private static final Json ourInstance = new Json();

    public static Json getInstance() {
        return ourInstance;
    }

    private Json() {
    }

    public static String fetch(String url) {

        StringBuilder data = new StringBuilder();
        try {
            URL urlFetch = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlFetch.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                    data.append(line);
            }

        } catch (MalformedURLException e) {
                Log.i(LOG_TAG, e.toString());
//                e.printStackTrace();
        } catch (IOException e) {
            Log.i(LOG_TAG, e.toString());
//            e.printStackTrace();
        }

        return data.toString();
    }
}
