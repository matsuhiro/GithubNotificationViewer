package net.matsuhiro.github.notificationviewer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends FragmentActivity {
    private String mState = "";
    private ProgressDialog mProgressDialog;
    private byte[] mKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mKey = ScrambleUtil.getScrambleDigest(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String accessToken = getAccessTokenFromCache();
        if (!TextUtils.isEmpty(accessToken)) {
            startMainActivity(accessToken);
            return;
        }

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) return;

        String code = uri.getQueryParameter("code");
        if (TextUtils.isEmpty(code)) {
            return;
        }
        String state = uri.getQueryParameter("state");
        if (!mState.equals(state)) {
            // error
            Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
            return;
        }
        requestAccessToken(code);
    }

    public void onClickLoginButton(View view) {
        mState = String.valueOf(new Random().nextInt(100));
        String clientId = getClientId();
        StringBuilder builder = new StringBuilder("https://github.com/login/oauth/authorize");
        builder.append("?client_id=")
                .append(clientId)
                .append("&scope=user,repo,notifications,public_repo")
                .append("&state=")
                .append(mState);
        Uri uri = Uri.parse(builder.toString());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void startMainActivity(String accessToken) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("access_token", accessToken);
        startActivity(i);
        finish();
    }

    private void requestAccessToken(final String code) {
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("text/plain"), ""
        );
        String query = new StringBuilder()
                .append("https://github.com/login/oauth/access_token")
                .append("?")
                .append("client_id=").append(getClientId())
                .append("&")
                .append("client_secret=").append(getClientSecret())
                .append("&")
                .append("code=").append(code)
                .toString();
        final Request request = new Request.Builder()
                .url(query)
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("connecting...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                        Toast.makeText(LoginActivity.this, "retry login", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                final String accessToken = getAccessToken(result);
                setAccessTokenToCache(accessToken);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                        startMainActivity(accessToken);
                    }
                });
            }
        });

    }

    private String getAccessTokenFromCache() {
        SharedPreferences spref = getSharedPreferences("token", Context.MODE_PRIVATE);
        String encrypted = spref.getString("token", "");
        return ScrambleUtil.decrypt(mKey, encrypted);
    }

    private void setAccessTokenToCache(String accessToken) {
        SharedPreferences spref = getSharedPreferences("token", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spref.edit();
        editor.putString("token", ScrambleUtil.encrypt(mKey, accessToken));
        editor.apply();
    }

    private String getAccessToken(String responseBody) {
        String[] separated = responseBody.split("&");
        for (int i = 0; i < separated.length; i++) {
            if (separated[i].contains("access_token")) {
                String[] target = separated[i].split("=");
                if (target.length == 2) {
                    return target[1];
                }
            }
        }
        return "";
    }

    private String getClientId() {
        // src is scrambled with signature of debug.keystore...
        // It is wrong, but this app is sample, so I use debug.keystore.
        // debug.keystore is included in this repository (^^;)
        return ScrambleUtil.decrypt(mKey, "lP3tRu2C9OK8UFeMiBpE+FtBwlK+9kGFVGwuR+2nE+U=");
    }

    private String getClientSecret() {
        return ScrambleUtil.decrypt(mKey, "2RJUp8x/M2Fc1TAjCy3LCtfVShMnzSyH+sbUYCvsIxzUxp00EgOyNt6O3OHv0KO7");
    }
}
