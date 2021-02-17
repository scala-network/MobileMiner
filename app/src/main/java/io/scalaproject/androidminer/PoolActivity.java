// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;
import io.scalaproject.androidminer.widgets.PoolInfoAdapter;
import io.scalaproject.androidminer.widgets.Toolbar;

public class PoolActivity extends BaseActivity
        implements PoolInfoAdapter.OnSelectPoolListener, PoolInfoAdapter.OnMenuPoolListener, View.OnClickListener {
    private static final String LOG_TAG = "PoolActivity";

    private Toolbar toolbar;

    private SwipeRefreshLayout pullToRefresh;
    private RecyclerView rvPools;
    private View fabAddPool;
    private LinearLayout llPoolsParent;
    private RelativeLayout rlSaveSettings;

    private Set<PoolItem> allPools = new HashSet<>();
    private Set<PoolItem> userdefinedPools = new HashSet<>();

    private PoolInfoAdapter poolsAdapter;

    private PoolItem selectedPool = null;

    RequestQueue mPoolQueue = null;

    public final static String RequesterType = "Requester";
    public final static int REQUESTER_NONE =1;
    public final static int REQUESTER_WIZARD = 0;
    public final static int REQUESTER_SETTINGS = 1;

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

        // If activity is created from Home Wizard
        Intent intent = getIntent();
        int requesterType = intent.getIntExtra(PoolActivity.RequesterType, PoolActivity.REQUESTER_NONE);

        llPoolsParent = findViewById(R.id.llPoolsParent);
        int marginBottom = requesterType == PoolActivity.REQUESTER_WIZARD ? Utils.getDimPixels(llPoolsParent, 90) : Utils.getDimPixels(llPoolsParent, 15);
        int marginDefault = Utils.getDimPixels(llPoolsParent, 15);
        // Must use parent layout for some reason
        ((RelativeLayout.LayoutParams) llPoolsParent.getLayoutParams()).setMargins(marginDefault, marginDefault, marginDefault, marginBottom);

        rlSaveSettings = findViewById(R.id.rlSaveSettings);
        rlSaveSettings.setVisibility(requesterType == PoolActivity.REQUESTER_WIZARD ? View.VISIBLE : View.GONE);

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                switch (type) {
                    case Toolbar.BUTTON_MAIN_CLOSE:
                        onBackPressed();
                }
            }

            @Override
            public void onButtonOptions(int type) {
                // Does nothing in pool view
            }
        });

        toolbar.setTitle("Mining Pool");
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_CLOSE);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_NONE);

        mPoolQueue = Volley.newRequestQueue(this);
        View view = findViewById(android.R.id.content).getRootView();

        fabAddPool = view.findViewById(R.id.fabAddPool);
        fabAddPool.setOnClickListener(this);
        fabAddPool.setVisibility(requesterType == PoolActivity.REQUESTER_WIZARD ? View.GONE : View.VISIBLE);

        rvPools = view.findViewById(R.id.rvPools);
        poolsAdapter = new PoolInfoAdapter(this, this, this);
        rvPools.setAdapter(poolsAdapter);

        pullToRefresh = view.findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        Utils.hideKeyboard(this);

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

    @Override
    public void onBackPressed() {
        //Config.write(Config.CONFIG_SELECTED_POOL, selectedPool.getKey().trim());
        //Config.write(Config.CONFIG_POOL_PORT, selectedPool.getDefaultPort().trim());

        SettingsFragment.selectedPoolTmp = selectedPool;

        ProviderManager.saveUserDefinedPools(getApplicationContext());

        super.onBackPressed();
    }

    private void updateSelectedPoolLayout() {
        // If recycler view has not been rendered yet
        if(rvPools.getLayoutManager().getItemCount() <= 0)
            return;

        String selectedPoolName = SettingsFragment.selectedPoolTmp == null ? Config.read(Config.CONFIG_SELECTED_POOL) : SettingsFragment.selectedPoolTmp.getKey();

        PoolItem[] allPools = ProviderManager.getAllPools();

        if(selectedPoolName.isEmpty()) {
            selectedPoolName = allPools[0].getKey();
        }

        if(!selectedPoolName.isEmpty()) {
            for (int i = 0; i < allPools.length; i++ ) {
                PoolItem poolItem = allPools[i];

                if(selectedPoolName.equals(poolItem.getKey())) {
                    //selectedPoolView = rvPools.getChildAt(i);
                    selectedPool = poolItem;
                }
            }
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
        selectedPool.setIsSelected(false);
        poolItem.setIsSelected(true);
        selectedPool = poolItem;

        SettingsFragment.selectedPoolTmp = null;

        newPool = false;

        poolsAdapter.dataSetChanged();
    }

    @Override
    public boolean onContextInteraction(MenuItem item, PoolItem poolItem) {
        switch (item.getItemId()) {
            case R.id.action_edit_pool:
                EditDialog diag = createEditDialog(poolItem);
                if (diag != null) {
                    diag.show();
                }

                break;
            case R.id.action_delete_pool:
                onDeletePool(poolItem);

                break;
            default:
                return super.onContextItemSelected(item);
        }

        return true;
    }

    public void onDeletePool(final PoolItem poolItem) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int action) {
                switch (action) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if(poolItem.isUserDefined()) {
                            userdefinedPools.remove(poolItem); // just used when saving
                            poolsAdapter.deletePool(poolItem);
                            ProviderManager.delete(poolItem);

                            ProviderManager.saveUserDefinedPools(getApplicationContext());

                            refresh();
                        }

                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        // do nothing
                        break;
                }
            }
        };

        if(!poolItem.isUserDefined()) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom);
            builder.setMessage("Default pools cannot be deleted.")
                    .setTitle(poolItem.getKey())
                    .setPositiveButton(getString(R.string.ok), dialogClickListener)
                    .show();
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom);
            builder.setMessage("Do you really want to delete this pool?")
                    .setTitle(poolItem.getKey())
                    .setPositiveButton(getString(R.string.yes), dialogClickListener)
                    .setNegativeButton(getString(R.string.no), dialogClickListener)
                    .show();
        }
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
                poolEdit.setPool(poolUrl);
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
            poolEditBackup = null;

            if (applyChanges()) {
                closeDialog();

                if (newPool) {
                    //allPools.add(poolEdit);
                    userdefinedPools.add(poolEdit); // just used when saving
                    poolsAdapter.addPool(poolEdit);
                    ProviderManager.add(poolEdit);

                    ProviderManager.saveUserDefinedPools(getApplicationContext());
                }

                poolsAdapter.dataSetChanged();
            }
        }

        private void closeDialog() {
            if (editDialog == null)
                return;

            Utils.hideKeyboard(getParent());

            editDialog.dismiss();
            editDialog = null;

            this.editDialog = null;
        }

        private void undoChanges() {
            if (poolEditBackup != null) {
                poolEdit.overwriteWith(poolEditBackup);
                poolEditBackup = null;
            }
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
                    ivPoolIcon.setImageBitmap(ProviderManager.getDefaultPoolIcon(getApplicationContext(), poolItem));
                }
            } else {
                poolEdit = new PoolItem();
                poolEdit.setUserDefined(true);
                poolEditBackup = null;
                ivPoolIcon.setImageBitmap(ProviderManager.getDefaultPoolIcon(getApplicationContext(), poolItem));
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

            //refresh();
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
        Config.write(Config.CONFIG_SELECTED_POOL, selectedPool.getKey().trim());

        startActivity(new Intent(PoolActivity.this, WizardSettingsActivity.class));
        //finish();
    }

    static public void parseVolleyError(VolleyError error) {
        String message = "";
        try {
            if (error.networkResponse != null) {
                String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject data = new JSONObject(responseBody);
                JSONArray errors = data.getJSONArray("errors");
                JSONObject jsonMessage = errors.getJSONObject(0);

                message = "VolleyError: " + jsonMessage.getString("message");
            } else {
                message = error.getMessage();
            }
        } catch (JSONException e) {
            message = "JSONException: " + e.getMessage();
        } catch (NullPointerException e) {
            message = "NullPointerException: " + e.getMessage();
        } catch (Exception e) {
            message = "Exception: " + e.getMessage();
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

            showProgressDialog(R.string.loading_pools);

            selectedPool = ProviderManager.getSelectedPool();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            allPools.clear();

            PoolItem[] pools = ProviderManager.getPools(getApplicationContext());
            for(int i = 0; i < pools.length; i++) {
                PoolItem poolItem = pools[i];

                StringRequest stringRequest = poolItem.getInterface().getStringRequest(poolsAdapter);
                mPoolQueue.add(stringRequest);

                allPools.add(poolItem);
            }

            return true;
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

            pullToRefresh.setRefreshing(false);

            poolsAdapter.setPools(allPools);
            poolsAdapter.allowClick(true);

            rvPools.post(() -> updateSelectedPoolLayout());

            dismissProgressDialog();
        }
    }
}