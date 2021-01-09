// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;
import io.scalaproject.androidminer.widgets.PoolInfoAdapter;

public class PoolActivity extends BaseActivity
        implements PoolInfoAdapter.OnSelectPoolListener, PoolInfoAdapter.OnMenuPoolListener, View.OnClickListener {
    private static final String LOG_TAG = "WizardPoolActivity";

    private int selectedPoolIndex = 0;

    private SwipeRefreshLayout pullToRefresh;
    private RecyclerView rvPools;
    private View fabAddPool;

    private Set<PoolItem> allPools = new HashSet<>();
    //private Set<PoolItem> userdefinedPools = new HashSet<>();

    private PoolInfoAdapter poolsAdapter;

    private View selectedPoolView = null;
    private PoolItem selectedPool = null;

    private Context mContext = null;
    RequestQueue mPoolQueue = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_pool);

        mPoolQueue = Volley.newRequestQueue(this);
        View view = findViewById(android.R.id.content).getRootView();

        fabAddPool = view.findViewById(R.id.fabAddPool);
        fabAddPool.setOnClickListener(this);
        //fabAddPool.setVisibility(readonly ? View.GONE : View.VISIBLE);

        rvPools = view.findViewById(R.id.rvPools);
        poolsAdapter = new PoolInfoAdapter(this, this, this);
        rvPools.setAdapter(poolsAdapter);

        rvPools.post(() -> updateSelectedPoolLayout());

        pullToRefresh = view.findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        Utils.hideKeyboard(this);

        allPools = new HashSet<>(Arrays.asList(ProviderManager.getPools()));

        refresh();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fabAddPool) {
            newPool = true;

            EditDialog diag = createEditDialog(null);
            if (diag != null) {
                diag.show();
            }
        }
    }

    private void updateSelectedPoolLayout() {
        // If recycler view has not been rendered yet
        if(rvPools.getLayoutManager().getItemCount() <= 0)
            return;

        String selectedPoolName = Config.read("selected_pool");

        PoolItem[] allPools = ProviderManager.getPools();

        if(selectedPoolName.isEmpty()) {
            selectedPoolName = allPools[0].getKey();
        }

        if(!selectedPoolName.isEmpty()) {
            for (int i = 0; i < allPools.length; i++ ) {
                PoolItem poolItem = allPools[i];
                Boolean bSelected = selectedPoolName.equals(poolItem.getKey());
                View itemView = rvPools.getChildAt(i);
                //View itemView = rvPools.getLayoutManager().findViewByPosition(i);
                setItemPoolLayout(itemView, bSelected);

                if(bSelected) {
                    selectedPoolView = itemView;
                    selectedPool = poolItem;
                }
            }
        }
    }

    private void setItemPoolLayout(View itemView, Boolean selected) {
        if(itemView != null) {
            RelativeLayout rlItemNode = (RelativeLayout) itemView;
            int bottom = rlItemNode.getPaddingBottom();
            int top = rlItemNode.getPaddingTop();
            int right = rlItemNode.getPaddingRight();
            int left = rlItemNode.getPaddingLeft();
            rlItemNode.setBackgroundResource(selected ? R.drawable.corner_radius_lighter_border_blue : R.drawable.corner_radius_lighter);
            rlItemNode.setPadding(left, top, right, bottom);
        }
    }

    private AsyncLoadPools asyncLoadPools = null;

    private void refresh() {
        if (asyncLoadPools != null) return; // ignore refresh request as one is ongoing

        asyncLoadPools = new AsyncLoadPools();
        asyncLoadPools.execute();
    }

    @Override
    public void onSelectPool(final View view, final PoolItem poolItem) {
        setItemPoolLayout(selectedPoolView, false);
        selectedPoolView = view;
        selectedPool = poolItem;
        setItemPoolLayout(selectedPoolView, true);

        newPool = false;
    }

    @Override
    public boolean onContextInteraction(MenuItem item, PoolItem poolItem) {
        switch (item.getItemId()) {
            case R.id.action_edit_pool:
                EditDialog diag = createEditDialog(poolItem);
                if (diag != null) {
                    diag.show();
                }
            case R.id.action_delete_pool:
                if(!poolItem.isUserDefined()) {

                } else {

                }
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    private PoolItem poolEdit = null;
    private PoolItem poolEditBackup = null;

    private boolean newPool = false;

    private EditDialog editDialog = null; // for preventing opening of multiple dialogs

    private EditDialog createEditDialog(final PoolItem poolItem) {
        if (editDialog != null) {
            editDialog.closeDialog();
            editDialog = null;
        }

        editDialog = new EditDialog(this, poolItem);

        return editDialog;
    }

    class EditDialog {
        private boolean applyChanges() {
            final String poolName = etPoolName.getEditText().getText().toString().trim();
            if (poolName.isEmpty()) {
                etPoolName.setError(getString(R.string.value_empty));
                return false;
            } else {
                poolEdit.setKey(poolName);
            }

            final String poolUrl = etPoolURL.getEditText().getText().toString().trim();
            if (poolUrl.isEmpty()) {
                etPoolURL.setError(getString(R.string.value_empty));
                return false;
            } else {
            poolEdit.setPoolUrl(poolUrl);
            }

            final String poolPort = etPoolPort.getEditText().getText().toString().trim();
            if (poolPort.isEmpty()) {
                etPoolPort.setError(getString(R.string.value_empty));
                return false;
            } else {
                poolEdit.setPort(poolPort);
            }

            return true;
        }

        private boolean applyChangesTmp() {
            final String poolName = etPoolName.getEditText().getText().toString().trim();
            poolEdit.setKey(poolName);

            final String poolURL = etPoolURL.getEditText().getText().toString().trim();
            poolEdit.setPoolUrl(poolURL);

            final String poolPort = etPoolPort.getEditText().getText().toString().trim();
            poolEdit.setPort(poolPort);

            return true;
        }

        private void apply() {
            if (applyChanges()) {
                closeDialog();

                if (newPool)
                    poolsAdapter.addPool(poolEdit);

                poolsAdapter.dataSetChanged();

                refresh();
            }
        }

        private void closeDialog() {
            if (editDialog == null) throw new IllegalStateException();

            Utils.hideKeyboard(getParent());

            editDialog.dismiss();
            editDialog = null;

            this.editDialog = null;
        }

        private void undoChanges() {
            if (poolEditBackup != null)
                poolEdit.overwriteWith(poolEditBackup);
        }

        private void show() {
            editDialog.show();
        }

        androidx.appcompat.app.AlertDialog editDialog = null;

        TextInputLayout etPoolName;
        TextInputLayout etPoolURL;
        TextInputLayout etPoolPort;
        ImageView ivPoolIcon;

        public static final int GET_FROM_GALLERY = 1;

        EditDialog(Activity activity, final PoolItem poolItem) {
            MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogCustom);
            LayoutInflater li = LayoutInflater.from(alertDialogBuilder.getContext());
            View promptsView = li.inflate(R.layout.prompt_editpool, null);
            alertDialogBuilder.setView(promptsView);

            etPoolName = promptsView.findViewById(R.id.etPoolName);
            etPoolURL = promptsView.findViewById(R.id.etPoolURL);
            etPoolPort = promptsView.findViewById(R.id.etPoolPort);
            ivPoolIcon = promptsView.findViewById(R.id.ivPoolIcon);

            Button btnSelectImage = promptsView.findViewById(R.id.btnSelectImage);
            btnSelectImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    applyChangesTmp();
                    pickImage();
                }
            });

            if (poolItem != null) {
                poolEdit = poolItem;

                if(poolEditBackup == null)
                    poolEditBackup = new PoolItem(poolItem);

                etPoolName.getEditText().setText(poolItem.getKey());
                etPoolURL.getEditText().setText(poolItem.getPoolUrl());
                etPoolPort.getEditText().setText(poolItem.getPort());

                Bitmap icon = poolItem.getIcon();
                if(icon != null)
                    ivPoolIcon.setImageBitmap(poolItem.getIcon());
                else {
                    ivPoolIcon.setImageBitmap(Utils.getBitmap(getApplicationContext(), R.drawable.ic_pool));
                }
            } else {
                poolEdit = new PoolItem();
                poolEdit.setUserDefined(true);
                poolEditBackup = null;
                ivPoolIcon.setImageBitmap(Utils.getBitmap(getApplicationContext(), R.drawable.ic_pool));
            }

            boolean isUserDefined = poolEdit.isUserDefined();
            etPoolName.setEnabled(isUserDefined);
            etPoolURL.setEnabled(isUserDefined);
            etPoolPort.setEnabled(isUserDefined);
            ivPoolIcon.setEnabled(isUserDefined);
            btnSelectImage.setEnabled(isUserDefined);

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    undoChanges();
                                    closeDialog();
                                    poolsAdapter.dataSetChanged(); // to refresh test results
                                }
                            });

            editDialog = alertDialogBuilder.create();

            // these need to be here, since we don't always close the dialog
            editDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface dialog) {
                    Button button = editDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            apply();
                        }
                    });
                }
            });

            refresh();
        }

        public void pickImage() {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("outputX", 256);
            intent.putExtra("outputY", 256);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("return-data", true);

            startActivityForResult(intent, 1);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == EditDialog.GET_FROM_GALLERY & resultCode == Activity.RESULT_OK) {
            // Already save the cropped image
            Bitmap bitmap = Utils.getCroppedBitmap((Bitmap) data.getExtras().get("data"));

            poolEdit.setIcon(bitmap);

            EditDialog diag = createEditDialog(poolEdit);
            if (diag != null) {
                diag.show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onNext(View view) {
        Config.write("selected_pool", Integer.toString(selectedPoolIndex));

        startActivity(new Intent(PoolActivity.this, WizardSettingsActivity.class));
        finish();
    }

    static public void parseVolleyError(VolleyError error) {
        String message = "";
        try {
            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            JSONObject data = new JSONObject(responseBody);
            JSONArray errors = data.getJSONArray("errors");
            JSONObject jsonMessage = errors.getJSONObject(0);

            message = "VolleyError: " + jsonMessage.getString("message");
        } catch (JSONException e) {
            message = "JSONException: " + e.getMessage();
        } finally {
            Log.i("parseVolleyError:", message);
        }
    }

    private class AsyncLoadPools extends AsyncTask<Void, PoolItem, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            poolsAdapter.setPools(null);
            poolsAdapter.allowClick(false);

            setItemPoolLayout(selectedPoolView, false);
            selectedPoolView = null;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Set<PoolItem> seedList = new HashSet<>();
            seedList.addAll(allPools);
            allPools.clear();

            /*Dispatcher d = new Dispatcher(new Dispatcher.Listener() {
                @Override
                public void onGet(PoolItem info) {
                    publishProgress(info);
                }
            });*/

            PoolItem[] pools = ProviderManager.getPools();
            for(int i = 0; i < pools.length; i++) {
                PoolItem poolItem = pools[i];

                if(poolItem.getApiUrl() != null) {
                    StringRequest stringRequest = poolItem.getInterface().getStringRequest(poolsAdapter);
                    mPoolQueue.add(stringRequest);
                }

                allPools.add(poolItem);
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(PoolItem... values) {
            if (!isCancelled())
                if (values != null)
                    poolsAdapter.addPool(values[0]);
                else
                    poolsAdapter.setPools(null);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            complete();
        }

        @Override
        protected void onCancelled(Boolean result) {
            complete();
        }

        private void complete() {
            asyncLoadPools = null;
            //if (!isAdded()) return;

            pullToRefresh.setRefreshing(false);

            poolsAdapter.setPools(allPools);
            poolsAdapter.allowClick(true);

            rvPools.post(() -> updateSelectedPoolLayout());
        }
    }
}