package jp.co.omron.plus_sensing.hvc_c2w_sample;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by ryoma on 16/01/14.
 */
public class HvcwApiWrap {

    private static final String TAG = HvcwApiWrap.class.getSimpleName();
    /** Web APIのリクエストURLのベース */
    private static final String SERVICE_URL = "https://developer.hvc.omron.com/c2w";

    private Handler handler = new Handler();

    public interface signupListener {
        void onResult(final String code, final String msg);
    }

    public void signup(String mailAddress, String apiKeyarg, final signupListener listener) {

        String url = SERVICE_URL + "/api/v1/signup.php";
        String apiKey = "apiKey=" + apiKeyarg;
        String email = "email=" + mailAddress;
        String params = apiKey + "&" + email;
        Log.d(TAG, "url:" + url);
        Log.d(TAG, "params:" + params);

        PostMessageTask task = new PostMessageTask(new Listener() {
            public void onReceived(String json) {
                if (json != null) {
                    Log.d(TAG, "json:" + json);

                    String code = new String();
                    String msg = new String();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        JSONObject result = jsonObject.getJSONObject("result");
                        code = result.getString("code");
                        msg = result.getString("msg");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final String codeRet = code;
                    final String msgRet = msg;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResult(codeRet, msgRet);
                        }
                    });
                } else {
                    Log.d(TAG, "json:null");
                }
            }
        });

        task.execute(url, params);
    }

        /**
     * POST処理のリスナー
     */
    public interface Listener {
        void onReceived(String json);
    }

    /**
     * リクエストURLにPOSTするタスク
     */
    public class PostMessageTask extends AsyncTask<String, Void, String> {

        /** POST処理のリスナー */
        private Listener listener = null;

        /**
         * コンストラクタ
         * @param listener POST処理のリスナー
         */
        public PostMessageTask(Listener listener) {
            this.listener = listener;
        }

        /**
         * バックグラウンド処理.
         * POSTしてその結果を返す
         * @param params [リクエストURL, アクセストークン（不要時は空）, パラメータ]
         * @return POST結果（HTTTPのエラーの場合はnull）
         */
        @Override
        protected String doInBackground(String... params) {
            HttpsURLConnection conn = null;
            String json = null;
            try {
                Log.d("PostSecureMessageTask", "connecting...");
                URL url;
                if (params[1].isEmpty()) {
                    // パラメータなし
                    url = new URL(params[0]);
                } else {
                    // パラメータがある場合はURLに連結
                    url = new URL(params[0] + "?" + params[1]);
                }
                conn = (HttpsURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                if (params.length == 3) {
                    // アクセストークンが必要なリクエストの場合
                    conn.setRequestProperty("Authorization", "Bearer " + params[2]);
                }
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // POST
                conn.connect();

                // レスポンス受信
                if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    Log.d("PostMessageTask", "response 200");
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    json = sb.toString();
                } else {
                    Log.d("PostMessageTask", "response " + conn.getResponseCode());
                }
            } catch(MalformedURLException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                if(conn != null) {
                    Log.d("PostMessageTask", "disconnecting...");
                    conn.disconnect();
                }
            }

            return json;
        }

        @Override
        protected void onPostExecute(String param) {
            listener.onReceived(param);
        }
    }

}
