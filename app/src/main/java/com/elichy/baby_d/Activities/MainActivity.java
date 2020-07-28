package com.elichy.baby_d.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.elichy.baby_d.Globals;
import com.elichy.baby_d.Models.Parent;
import com.elichy.baby_d.Models.ParentLogin;
import com.elichy.baby_d.Models.ResAPIHandler;
import com.elichy.baby_d.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private TextView attempts;
    private Button login;
    private Button register;
    private int counter = 0;
    private final static int NUM_ATTEMPTS = 4;
    private static final String TAG = "MainActivity";
    private ResAPIHandler resAPIHandler;
    private Retrofit retrofit;
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TEXT = "token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Load main application");
        loginInit();
        setListenrs();
    }

    private void setListenrs() {
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Try to login go to validate function");
                loginValidate(email.getText().toString(), password.getText().toString());
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Move to Register activity");
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginInit() {
        email = (EditText) findViewById(R.id.uMail);
        password = (EditText) findViewById(R.id.uPass);
        attempts = (TextView) findViewById(R.id.attempts);
        login = (Button) findViewById(R.id.login);
        register = (Button) findViewById(R.id.registerBtn);
        attempts.setText("");
        retrofit = new Retrofit.Builder()
                .baseUrl(String.format("%s/api/parent/", Globals.server_ip))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        resAPIHandler = retrofit.create(ResAPIHandler.class);

        Log.d(TAG, "loginInit: Done init all activity attributes");
    }

    private void loginValidate(String userEmail, String userPassword){
        Log.d(TAG, "loginValidate: Try to log in");
        ParentLogin parentInfo = new ParentLogin(userEmail, userPassword);
        Call<Parent> call = resAPIHandler.loginParent(parentInfo);
        call.enqueue(new Callback<Parent>() {
            @Override
            public void onResponse(Call<Parent> call, Response<Parent> response) {
                if (!response.isSuccessful()){
                    Log.d(TAG, "loginValidate: Fail to login");
                    counter++;
                    attempts.setText(String.format("Number of remaining attempts %s", (NUM_ATTEMPTS - counter)));
                    if (counter == NUM_ATTEMPTS){
                        Log.d(TAG, "loginValidate: No more retries for login, lock the option");
                        login.setEnabled(false);
                    }
                    return;
                }
                Parent jwt = response.body();
                saveToShardPref(jwt);
                Log.d(TAG, String.format("onResponse: jwt: %s",jwt));
                Intent intent = new Intent(MainActivity.this, ParentViewActivity.class);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Parent> call, Throwable t) {
                Log.d(TAG, "loginValidate: Fail to login");
                counter++;
                attempts.setText(String.format("Number of remaining attempts %s", (NUM_ATTEMPTS - counter)));
                if (counter == NUM_ATTEMPTS){
                    Log.d(TAG, "loginValidate: No more retries for login, lock the option");
                    login.setEnabled(false);
                }
            }
        });
    }

    private void saveToShardPref(Parent jwt) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TEXT, jwt.getToken());
        editor.apply();
    }
}