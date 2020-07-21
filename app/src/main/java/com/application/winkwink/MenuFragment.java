package com.application.winkwink;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.winkwink.Utilities.GameRivalsContract;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class MenuFragment extends Fragment implements View.OnClickListener {

    private static final int REQUEST_ACCESS_COARSE_LOCATION_ID = 1;

    private RivalsRecycleAdapter mAdapter;
    private SQLiteOpenHelper dbHelper;

    public MenuFragment() {}

    public static MenuFragment newInstance() { return new MenuFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        MainActivity ma = (MainActivity) getActivity();

        assert ma != null;
        dbHelper = ma.getDbHelper();

        SharedPreferences pref = ma.getPreferences(Context.MODE_PRIVATE);

        boolean registered = pref.getBoolean("GAME_RESULT_REGISTERED", true);
        String userId = pref.getString("GAME_RESULT_CHALLENGER", null);
        long userScore = pref.getLong("GAME_RESULT_SCORE", 0);

        RivalsDbUpdater task;

        if(!registered)
            task = new RivalsDbUpdater(userId, userScore, dbHelper, mAdapter, ma);
        else
            task = new RivalsDbUpdater(null, 0, dbHelper, mAdapter, ma);

        Thread t = new Thread(task);
        t.start();

        pref.edit().putBoolean("GAME_RESULT_REGISTERED", true).apply();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        Button findButton = view.findViewById((R.id.find_button));
        findButton.setOnClickListener(this);

        Button hostButton = view.findViewById(R.id.host_button);
        hostButton.setOnClickListener(this);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_2);

        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new RivalsRecycleAdapter(getActivity());
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.find_button) {

            handleFragmentLocationPermission();
        } else if(view.getId() == R.id.host_button) {

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.menu_container, LobbyFragment.newInstance())
                    .addToBackStack("LOBBY_TRANSITION")
                    .commit();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_ACCESS_COARSE_LOCATION_ID) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.menu_container, BluetoothListFragment.newInstance())
                        .addToBackStack("BLUETOOTH_LIST_TRANSITION")
                        .commit();
            } else {
                //TODO
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
        }
    }

    private void handleFragmentLocationPermission () {

        Context context = getContext();

        assert context != null;

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.menu_container, BluetoothListFragment.newInstance())
                    .addToBackStack("BLUETOOTH_LIST_TRANSITION")
                    .commit();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COARSE_LOCATION_ID);
        }
    }

    private static class RivalsDbUpdater implements Runnable {

        private static final String insertCommand = "INSERT INTO " +
                GameRivalsContract.RivalsEntry.TABLE_NAME +
                "(" + GameRivalsContract.RivalsEntry.COLUMN_NAME_PLAYER_ID +
                ", " + GameRivalsContract.RivalsEntry.COLUMN_NAME_PLAYER_SCORE +
                ") VALUES (?,?)";

        private SQLiteOpenHelper dbHelper;
        private WeakReference<Activity> activityWeakReference;

        private String id;
        private long score;

        private RivalsRecycleAdapter rivalsRecycleAdapter;

        public RivalsDbUpdater(String newId, long newScore, SQLiteOpenHelper helper,
                               RivalsRecycleAdapter rra, Activity a) {

            id = newId;
            score = newScore;
            dbHelper = helper;
            rivalsRecycleAdapter = rra;
            activityWeakReference = new WeakReference<>(a);
        }

        @Override
        public void run() {

            SQLiteDatabase sqlDb = dbHelper.getWritableDatabase();
            String tableName = GameRivalsContract.RivalsEntry.TABLE_NAME;
            String playerIdColumn = GameRivalsContract.RivalsEntry.COLUMN_NAME_PLAYER_ID;
            String playerScoreColumn = GameRivalsContract.RivalsEntry.COLUMN_NAME_PLAYER_SCORE;

            if (id != null) {
                ContentValues cv = new ContentValues();

                cv.put(playerIdColumn, id);
                cv.put(playerScoreColumn, score);

                sqlDb.insert(tableName,
                        null,
                        cv);
            }

            String queryString =
                    "SELECT " + playerIdColumn +
                            ", (avg(" + playerScoreColumn + ")) AS avg, " +
                            "(count(*)) as cnt FROM " + tableName +
                            " GROUP BY " + playerIdColumn;

            Cursor cursor = sqlDb.rawQuery(queryString, null);

            Activity activity = activityWeakReference.get();

            if(activity != null)
                activity.runOnUiThread(() -> rivalsRecycleAdapter.setCursor(cursor));
        }
    }

    private static class RivalsRecycleAdapter
            extends RecyclerView.Adapter<RivalsRecycleAdapter.MyViewHolder> {

        private Cursor cursor;
        private WeakReference<Activity> activityWeakReference;

        public static class MyViewHolder extends RecyclerView.ViewHolder {

            private CardView cv;
            private TextView idView;
            private TextView matchesView;
            private TextView averageView;

            public MyViewHolder(View v) {
                super(v);

                cv = v.findViewById(R.id.cv);
                idView = v.findViewById(R.id.cv_main_line);
                matchesView = v.findViewById(R.id.cv_secondary_line);
                averageView = v.findViewById(R.id.cv_tertiary_line);
            }
        }

        public RivalsRecycleAdapter(Activity a) {

            activityWeakReference = new WeakReference<>(a);
        }

        public void setCursor(Cursor c) {

            if(cursor != null)
                cursor.close();

            cursor = c;
            this.notifyDataSetChanged();
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public RivalsRecycleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                                             int viewType) {

            Activity a = activityWeakReference.get();

            // create a new view
            View v = a.getLayoutInflater()
                    .inflate(R.layout.card_view_2_item, parent, false);

            return new RivalsRecycleAdapter.MyViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(RivalsRecycleAdapter.MyViewHolder holder, int position) {

            cursor.moveToPosition(position);

            String playerId = cursor.getString(cursor.getColumnIndexOrThrow(
                    GameRivalsContract.RivalsEntry.COLUMN_NAME_PLAYER_ID));
            int playerAvg = cursor.getInt(cursor.getColumnIndexOrThrow("avg"));
            int playerGames = cursor.getInt(cursor.getColumnIndexOrThrow("cnt"));

            String gamesFormat = "Games played: %d";
            String avgFormat = "Average score: %d";

            holder.idView.setText(playerId);
            holder.matchesView.setText(String.format(Locale.ROOT,gamesFormat, playerGames));
            holder.averageView.setText(String.format(Locale.ROOT, avgFormat, playerAvg));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {

            if(cursor != null)
                return cursor.getCount();
            else
                return 0;
        }

    }

}