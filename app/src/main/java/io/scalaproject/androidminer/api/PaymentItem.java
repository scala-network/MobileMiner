// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2021, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;

import io.scalaproject.androidminer.Config;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.api.providers.*;

public class PaymentItem {

    public String mHash, mTimestamp, mAmount, mFee;

    public PaymentItem() {
    }


    /*static public Comparator<PaymentItem> PaymentComparator = new Comparator<PaymentItem>() {
        @Override
        public int compare(PaymentItem o1, PaymentItem o2) {
            return 0;
            //return o1.getKey().compareToIgnoreCase(o2.getKey());
        }
    };*/
}
