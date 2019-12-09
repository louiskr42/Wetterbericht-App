package com.coreelements.de.stormy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_TIMELABEL = "KEY_TIMELABEL";
    private static final String KEY_TEMPERATURLABEL = "KEY_TEMPERATURLABEL";
    private static final String KEY_HUMIDITYVALUE = "KEY_HUMIDITYVALUE";
    private static final String KEY_PRECIPVALUE = "KEY_PRECIPVALUE";
    private static final String KEY_SUMMARYLABEL = "KEY_SUMMARYLABEL";
    private static final String KEY_ICONIMAGEVIEW = "KEY_ICONIMAGEVIEW";
    private static final String KEY_LOCATIONLABEL = "KEY_LOCATIONLABEL";

    private CurrentWeather mCurrentWeather;

    @BindView(R.id.timeLabel) TextView mTimeLabel;
    @BindView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.locationLabel) TextView mLocationLabel;
    @BindView(R.id.refreshImageView) ImageView mRefreshImageView;
    @BindView(R.id.progressBar2) ProgressBar mProgressBar;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (!mTemperatureLabel.getText().toString().contains("--")) {
            outState.putString(KEY_TIMELABEL, mTimeLabel.getText().toString());
            outState.putString(KEY_TEMPERATURLABEL, mTemperatureLabel.getText().toString());
            if (!mHumidityValue.getText().toString().contains("--")) {
                outState.putDouble(KEY_HUMIDITYVALUE, Double.parseDouble(mHumidityValue.getText().toString().trim().replace("%", "")));
            }
            if (!mPrecipValue.getText().toString().contains("--")) {
                outState.putInt(KEY_PRECIPVALUE, Integer.parseInt(mPrecipValue.getText().toString().trim().replace("%", "")));
            }
            outState.putString(KEY_SUMMARYLABEL, mSummaryLabel.getText().toString());
            outState.putInt(KEY_ICONIMAGEVIEW, mCurrentWeather.getIconId());
            outState.putString(KEY_LOCATIONLABEL, mLocationLabel.getText().toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

            mTimeLabel.setText(savedInstanceState.getString(KEY_TIMELABEL, ""));
            mTemperatureLabel.setText(savedInstanceState.getString(KEY_TEMPERATURLABEL, "--"));
            mHumidityValue.setText(savedInstanceState.getDouble(KEY_HUMIDITYVALUE, 0.00) + "%");
            mPrecipValue.setText(savedInstanceState.getInt(KEY_PRECIPVALUE, 0) + "%");
            mSummaryLabel.setText(savedInstanceState.getString(KEY_SUMMARYLABEL, ""));
            mIconImageView.setImageDrawable(getResources().getDrawable(savedInstanceState.getInt(KEY_ICONIMAGEVIEW, R.drawable.cloudy_night)));
            mLocationLabel.setText(savedInstanceState.getString(KEY_LOCATIONLABEL, "--"));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        TextView darkSky = (TextView)findViewById(R.id.darkSkyAttribution);
        darkSky.setMovementMethod(LinkMovementMethod.getInstance());

        mProgressBar.setVisibility(View.INVISIBLE);

        final double latitude = 53.551086;
        final double longitude = 9.993682;

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(latitude, longitude);
                Toast.makeText(getApplicationContext(), "Weather was updated!", Toast.LENGTH_LONG).show();
            }
        });

        getForecast(latitude, longitude);

        Log.d(TAG, "Main UI code is running!");
    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = "3307709910f85b51a2a72a47788d6d2e";
        String forecastURL = "https://api.darksky.net/forecast/" + apiKey + "/" + latitude + "," + longitude;

        if (isNetworkAvailable()) {
            toogleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastURL)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toogleRefresh();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toogleRefresh();
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                    catch (JSONException j){

                    }
                }
            });
        }
        else {
            Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void toogleRefresh() {
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mProgressBar.setProgress(87, true);
            }
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        mTemperatureLabel.setText(mCurrentWeather.getTemperature() + "");
        mTimeLabel.setText("At " + mCurrentWeather.getFormattedTime() + " it will be:");
        mHumidityValue.setText(mCurrentWeather.getHumidity() + "%");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance() + "%");
        mSummaryLabel.setText(mCurrentWeather.getSummary());
        Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);
        mLocationLabel.setText(mCurrentWeather.getTimeZone());
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);
        currentWeather.setSummary(currently.getString("summary"));

        return currentWeather;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        else {
            Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show();
        }
        return isAvailable;
    }

    private void alertUserAboutError() {
AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

}