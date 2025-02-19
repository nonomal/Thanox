package github.tornaco.android.thanos.start;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import github.tornaco.android.rhino.plugin.Verify;
import github.tornaco.android.thanos.BuildProp;
import github.tornaco.android.thanos.R;
import github.tornaco.android.thanos.core.app.ThanosManager;
import github.tornaco.android.thanos.databinding.ActivityStartRulesBinding;
import github.tornaco.android.thanos.theme.ThemeActivity;
import github.tornaco.android.thanos.util.ActivityUtils;
import github.tornaco.android.thanos.widget.SwitchBar;

public class StartRuleActivity extends ThemeActivity implements StartRuleItemClickListener {

    private StartRuleViewModel viewModel;
    private ActivityStartRulesBinding binding;

    @Verify
    public static void start(Context context) {
        ActivityUtils.startActivity(context, StartRuleActivity.class);
    }

    @Override
    @Verify
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartRulesBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setupView();
        setupViewModel();
    }

    private void setupView() {
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // List.
        binding.ruleListView.setLayoutManager(new LinearLayoutManager(this));
        binding.ruleListView.setAdapter(new StartRuleListAdapter(this));
        binding.swipe.setOnRefreshListener(() -> viewModel.start());
        binding.swipe.setColorSchemeColors(getResources().getIntArray(
                github.tornaco.android.thanos.module.common.R.array.common_swipe_refresh_colors));

        binding.fab.setOnClickListener((View v) -> showEditOrAddDialog(null));

        // Switch.
        onSetupSwitchBar(binding.switchBar);
    }

    private void setupViewModel() {
        viewModel = obtainViewModel(this);
        viewModel.start();

        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);
        binding.executePendingBindings();
    }

    protected void onSetupSwitchBar(SwitchBar switchBar) {
        switchBar.setChecked(getSwitchBarCheckState());
        switchBar.addOnSwitchChangeListener(this::onSwitchBarCheckChanged);
    }

    protected boolean getSwitchBarCheckState() {
        return ThanosManager.from(getApplicationContext()).isServiceInstalled()
                && ThanosManager.from(getApplicationContext()).getActivityManager().isStartRuleEnabled();
    }

    protected void onSwitchBarCheckChanged(Switch switchBar, boolean isChecked) {
        ThanosManager.from(getApplicationContext()).getActivityManager().setStartRuleEnabled(isChecked);
    }

    @Override
    @Verify
    protected void onResume() {
        super.onResume();
        viewModel.resume();
    }

    public static StartRuleViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelProvider.AndroidViewModelFactory factory = ViewModelProvider.AndroidViewModelFactory
                .getInstance(activity.getApplication());
        return ViewModelProviders.of(activity, factory).get(StartRuleViewModel.class);
    }

    @Override
    public void onItemClick(@NonNull StartRule rule) {
        showEditOrAddDialog(rule.getRaw());
    }

    private void showEditOrAddDialog(String ruleIfEdit) {
        ThanosManager thanosManager = ThanosManager.from(getApplicationContext());
        if (!thanosManager.isServiceInstalled()) return;

        AppCompatEditText editText = new AppCompatEditText(thisActivity());
        editText.setText(ruleIfEdit);
        AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity())
                .setTitle(R.string.menu_title_rules)
                .setView(editText)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (editText.getText() == null) {
                            return;
                        }
                        String newRule = editText.getText().toString();
                        if (TextUtils.isEmpty(newRule)) {
                            return;
                        }
                        if (ruleIfEdit != null) {
                            thanosManager.getActivityManager().deleteStartRule(ruleIfEdit);
                        }
                        thanosManager.getActivityManager().addStartRule(newRule);
                        viewModel.start();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (!TextUtils.isEmpty(ruleIfEdit)) {
            builder.setNeutralButton(R.string.common_menu_title_remove, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    thanosManager.getActivityManager().deleteStartRule(ruleIfEdit);
                    viewModel.start();
                }
            });
        }

        builder.create().show();
    }

    @Override
    @Verify
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.start_rules_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    @Verify
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            showInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Verify
    private void showInfoDialog() {
        new AlertDialog.Builder(thisActivity())
                .setTitle(R.string.menu_title_rules)
                .setMessage(R.string.feature_summary_start_restrict_rules)
                .setNeutralButton(R.string.common_menu_title_wiki,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(BuildProp.THANOX_URL_DOCS_START_RULES));
                                if (intent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(Intent.createChooser(intent, ""));
                                }
                            }
                        })
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
