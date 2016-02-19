package net.matsuhiro.github.notificationviewer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.matsuhiro.github.notificationviewer.entity.Notification;
import net.matsuhiro.github.notificationviewer.entity.SubjectDetail;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements
        NotificationRecyclerViewAdapter.OnListInteractionListener {

    private String mAccessToken = "";
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NotificationRecyclerViewAdapter mAdapter;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        final int offset = (int) (8 * getResources().getDisplayMetrics().density);
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = ((RecyclerView.LayoutParams) view.getLayoutParams())
                        .getViewLayoutPosition();
                if (position == 0) {
                    outRect.set(offset, offset, offset, offset);
                } else {
                    outRect.set(offset, 0, offset, offset);
                }
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestNotificationsData();
            }
        });

        mAccessToken = getIntent().getStringExtra("access_token");

        setupNotificationsList();
        requestNotificationsData();
        mProgressDialog = null;
    }

    protected void onResume() {
        super.onResume();
        mAdapter.enableClick(true);
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void onListFragmentInteraction(Notification item) {
        mAdapter.enableClick(false);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("connecting...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        showDetailInBrowser(item);
    }

    private void setupNotificationsList() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new NotificationRecyclerViewAdapter(new CopyOnWriteArrayList<Notification>(), this);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void requestNotificationsData() {
        if (TextUtils.isEmpty(mAccessToken)) {
            return;
        }

        Request request = new Request.Builder()
                .url("https://api.github.com/notifications")
                .header("Authorization", "token " + mAccessToken)
                .build();
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "connection error", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                Gson gson = new Gson();
                try {
                    final CopyOnWriteArrayList<Notification> notifications =
                            gson.fromJson(result, new TypeToken<CopyOnWriteArrayList<Notification>>() {
                            }.getType());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.setItemsAndNotify(notifications);
                            if (mSwipeRefreshLayout.isRefreshing()) {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
                } catch (JsonSyntaxException e) {
                    Log.e(MainActivity.class.getSimpleName(), "json error");
                }
            }
        });
    }

    private void showDetailInBrowser(Notification notification) {
        Request request = new Request.Builder()
                .url(notification.subject.url)
                .header("Authorization", "token " + mAccessToken)
                .build();
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "connection error", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                Gson gson = new Gson();
                try {
                    SubjectDetail detail = gson.fromJson(result, SubjectDetail.class);
                    final String url = detail.htmlUrl;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Uri uri = Uri.parse(url);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    });
                } catch (JsonSyntaxException e) {
                    Log.e(MainActivity.class.getSimpleName(), "json error");
                }
            }
        });
    }
}

