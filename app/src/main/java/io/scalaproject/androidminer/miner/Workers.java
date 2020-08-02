// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.miner;

import android.os.AsyncTask;

public class Workers extends AsyncTask<String, Integer, String> {
    public MinerData data = new MinerData();

    public Workers(){
        data.hashes =0;
    }

    @Override
    protected void onPreExecute() {
        //Setup precondition to execute some task
    }

    @Override
    protected String doInBackground(String... params) {
        //Do some task
        return "";
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        //Update the progress of current task
    }

    @Override
    protected void onPostExecute(String s) {
        //Show the result obtained from doInBackground
    }
}
