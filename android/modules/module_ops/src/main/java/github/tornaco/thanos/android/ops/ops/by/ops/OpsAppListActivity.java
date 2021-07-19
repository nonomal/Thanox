package github.tornaco.thanos.android.ops.ops.by.ops;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.common.collect.Lists;

import github.tornaco.android.thanos.common.AppItemClickListener;
import github.tornaco.android.thanos.common.AppListModel;
import github.tornaco.android.thanos.common.CommonAppListFilterActivity;
import github.tornaco.android.thanos.common.CommonAppListFilterAdapter;
import github.tornaco.android.thanos.common.CommonAppListFilterViewModel;
import github.tornaco.android.thanos.common.StateImageProvider;
import github.tornaco.android.thanos.core.app.ThanosManager;
import github.tornaco.android.thanos.core.pm.AppInfo;
import github.tornaco.android.thanos.core.secure.ops.AppOpsManager;
import github.tornaco.android.thanos.util.ActivityUtils;
import github.tornaco.android.thanos.widget.SwitchBar;
import github.tornaco.thanos.android.ops.R;
import github.tornaco.thanos.android.ops.model.Op;
import github.tornaco.thanos.android.ops.ops.repo.OpsPackageLoader;
import util.CollectionUtils;
import util.Consumer;

public class OpsAppListActivity extends CommonAppListFilterActivity {
    private Op op;
    private CommonAppListFilterAdapter appListFilterAdapter;

    public static void start(Context context, Op op) {
        Bundle data = new Bundle();
        data.putParcelable("op", op);
        ActivityUtils.startActivity(context, OpsAppListActivity.class, data);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        this.op = getIntent().getParcelableExtra("op");
        if (op == null) {
            finish();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected String getTitleString() {
        return op.getTitle();
    }

    @NonNull
    @Override
    protected AppItemClickListener onCreateAppItemViewClickListener() {
        return this::showModeSelectionDialog;
    }

    private void showModeSelectionDialog(AppInfo appInfo) {
        String[] items = Lists.newArrayList(
                getString(R.string.module_ops_mode_allow),
                getString(R.string.module_ops_mode_foreground),
                getString(R.string.module_ops_mode_ignore))
                .toArray(new String[0]);
        int currentMode = Integer.parseInt(appInfo.getStr());
        int currentSelection = 0;
        if (currentMode == AppOpsManager.MODE_FOREGROUND) {
            currentSelection = 1;
        }
        if (currentMode == AppOpsManager.MODE_IGNORED) {
            currentSelection = 2;
        }
        new AlertDialog.Builder(thisActivity())
                .setTitle(appInfo.getAppLabel())
                .setSingleChoiceItems(items, currentSelection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int newMode = AppOpsManager.MODE_ALLOWED;
                        if (which == 1) {
                            newMode = AppOpsManager.MODE_FOREGROUND;
                        }
                        if (which == 2) {
                            newMode = AppOpsManager.MODE_IGNORED;
                        }
                        int finalNewMode = newMode;
                        ThanosManager.from(getApplicationContext())
                                .ifServiceInstalled(thanosManager -> thanosManager.getAppOpsManager()
                                        .setMode(op.getCode(), appInfo.getUid(), appInfo.getPkgName(), finalNewMode));

                        appInfo.setStr(String.valueOf(newMode));
                        appListFilterAdapter.notifyDataSetChanged();
                    }
                })
                .setCancelable(true)
                .show();
    }

    @Override
    protected CommonAppListFilterAdapter onCreateCommonAppListFilterAdapter() {
        appListFilterAdapter = new CommonAppListFilterAdapter(onCreateAppItemViewClickListener(), new StateImageProvider() {
            @Override
            public int provideImageRes(@NonNull AppListModel model) {
                String payload = model.appInfo.getStr();
                int mode = Integer.parseInt(payload);
                if (mode == AppOpsManager.MODE_ALLOWED) {
                    return R.drawable.module_ops_ic_checkbox_circle_fill_green;
                }
                if (mode == AppOpsManager.MODE_FOREGROUND) {
                    return R.drawable.module_ops_ic_checkbox_circle_fill_amber;
                }
                if (mode == AppOpsManager.MODE_IGNORED) {
                    return R.drawable.module_ops_ic_forbid_2_fill_red;
                }
                return 0;
            }

            @Override
            public View.OnClickListener provideOnClickListener(@NonNull AppListModel model, int itemIndex) {
                return v -> {
                    AppInfo appInfo = model.appInfo;
                    quickSwitch(appInfo, itemIndex);
                };
            }
        }, false);
        return appListFilterAdapter;
    }

    private void quickSwitch(AppInfo appInfo, int itemIndex) {
        String payload = appInfo.getStr();
        int mode = Integer.parseInt(payload);
        int newMode = AppOpsManager.MODE_ALLOWED;
        if (mode == AppOpsManager.MODE_ALLOWED) {
            newMode = AppOpsManager.MODE_FOREGROUND;
        }
        if (mode == AppOpsManager.MODE_FOREGROUND) {
            newMode = AppOpsManager.MODE_IGNORED;
        }
        if (mode == AppOpsManager.MODE_IGNORED) {
            newMode = AppOpsManager.MODE_ALLOWED;
        }
        int finalNewMode = newMode;
        ThanosManager.from(thisActivity())
                .ifServiceInstalled(thanosManager -> thanosManager.getAppOpsManager()
                        .setMode(op.getCode(), appInfo.getUid(), appInfo.getPkgName(), finalNewMode));
        appInfo.setStr(String.valueOf(finalNewMode));
        appListFilterAdapter.notifyItemChanged(itemIndex);
    }

    @NonNull
    @Override
    protected CommonAppListFilterViewModel.ListModelLoader onCreateListModelLoader() {
        return new OpsPackageLoader(getApplicationContext(), op);
    }

    @Override
    protected void onInflateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.module_ops_op_app_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (R.id.action_select_all_allow == item.getItemId()) {
            new AlertDialog.Builder(thisActivity())
                    .setMessage(github.tornaco.android.thanos.module.common.R.string.common_dialog_message_are_you_sure)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectAll(AppOpsManager.MODE_ALLOWED);
                        }
                    }).show();
            return true;
        }
        if (R.id.action_select_all_foreground == item.getItemId()) {
            new AlertDialog.Builder(thisActivity())
                    .setMessage(github.tornaco.android.thanos.module.common.R.string.common_dialog_message_are_you_sure)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectAll(AppOpsManager.MODE_FOREGROUND);
                        }
                    }).show();
            return true;
        }
        if (R.id.action_un_select_all_ignore == item.getItemId()) {
            new AlertDialog.Builder(thisActivity())
                    .setMessage(github.tornaco.android.thanos.module.common.R.string.common_dialog_message_are_you_sure)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectAll(AppOpsManager.MODE_IGNORED);
                        }
                    }).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void selectAll(int mode) {
        CollectionUtils.consumeRemaining(appListFilterAdapter.getListModels(), new Consumer<AppListModel>() {
            @Override
            public void accept(AppListModel model) {
                AppInfo appInfo = model.appInfo;
                ThanosManager.from(getApplicationContext())
                        .ifServiceInstalled(thanosManager -> thanosManager.getAppOpsManager()
                                .setMode(op.getCode(), appInfo.getUid(), appInfo.getPkgName(), mode));
            }
        });
        viewModel.start();
    }

    @Override
    protected void onSetupSwitchBar(SwitchBar switchBar) {
        super.onSetupSwitchBar(switchBar);
        switchBar.hide();
    }
}
