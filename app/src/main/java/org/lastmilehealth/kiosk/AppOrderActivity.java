package org.lastmilehealth.kiosk;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Andreas Schrade on 17.09.2015.
 */
public class AppOrderActivity extends Activity implements View.OnClickListener {

    PackageManager PM;
    RecyclerView rvOrderingApp;
    OrderingAdapter mOrderingAdapter;
    private int spanCount = 4;

    public List<AppDetail> alowedApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_ordering);

        PM = getPackageManager();

        rvOrderingApp = (RecyclerView) findViewById(R.id.rv_ordering_app);
        rvOrderingApp.setLayoutManager(new GridLayoutManager(this, spanCount));

        findViewById(R.id.saveBtn).setOnClickListener(this);
        findViewById(R.id.cancelBtn).setOnClickListener(this);

        // loading allowed apps
        Set<String> allowedAppPackages = PrefUtils.getAllowedApps(this);
        Set<String> hiddenAppPackages = PrefUtils.getAppsWithHiddenIcon(this);

        for (String packageName : allowedAppPackages) {
            if (hiddenAppPackages.contains(packageName))
                continue; // skip

            AppDetail appDetail = buildAppInfoByPackage(packageName);
            appDetail.displayOrder = PrefUtils.getOrderForApp(this, packageName);
            alowedApps.add(appDetail);
        }

        // sorting the app based on display order previously set
        Collections.sort(alowedApps, new AppOrderComparator());

        mOrderingAdapter = new OrderingAdapter(alowedApps);
        rvOrderingApp.setAdapter(mOrderingAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rvOrderingApp);
    }

    private ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                    ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            // moving original array list of alowed apps
            if (fromPosition < toPosition)
                for (int i = fromPosition; i < toPosition; i++)
                    Collections.swap(alowedApps, i, i + 1);
            else
                for (int i = fromPosition; i > toPosition; i--)
                    Collections.swap(alowedApps, i, i - 1);

            mOrderingAdapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG)
                ((OrderingAdapter.AppViewHolder) viewHolder).setSelected();
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            ((OrderingAdapter.AppViewHolder) viewHolder).clearSelection();
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }
    };

    private AppDetail buildAppInfoByPackage(String packageName) {
        AppDetail app = new AppDetail();
        try {
            ApplicationInfo ai = this.getPackageManager().getApplicationInfo(packageName, 0);
            app.label = PM.getApplicationLabel(ai);
            app.icon = PM.getApplicationIcon(ai);
            app.name = ai.packageName;
        } catch (final PackageManager.NameNotFoundException e) {
            Log.i("Kiosk", "name not found for package: " + packageName);
        }
        return app;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.saveBtn:
                for (int i = 0; i < alowedApps.size(); i++) {
                    AppDetail appDetail = alowedApps.get(i);
                    PrefUtils.setOrderForApp(this, appDetail.name.toString(), i);
                }
                finish();
                break;
            case R.id.cancelBtn:
                finish();
                break;
        }
    }
}