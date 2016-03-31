package com.example.autopilot;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class QuadcopterActivity extends Activity {
    public static final long UI_REFRESH_PERIOD_MS = 250;
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String PREFS_ID_LAST_IP = "lastServerIP";

    private static TextView accessoryConnectionStatus;
    private TextToSpeech tts;
    private CheckBox connectToServerCheckBox;
    private EditText serverIpEditText;
    private MainController mainController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                        Log.e("error", "This Language is not supported");
                } else Log.e("error", "Initilization Failed!");
            }
        });
        tts.setLanguage(Locale.US);

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
            }
            TextView tv = (TextView) findViewById(R.id.logView);
            final ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
            tv.setText(log.toString());
            sv.post(new Runnable() {
                @Override
                public void run() {
                    sv.fullScroll(View.FOCUS_DOWN);
                }
            });
        } catch (IOException e) {
        }

        // Get UI elements.
        accessoryConnectionStatus = (TextView) findViewById(R.id.accessory_connection_status);
        serverIpEditText = (EditText) findViewById(R.id.serverIpEditText);
        connectToServerCheckBox = (CheckBox) findViewById(R.id.connectToServerCheckBox);

        // In the "server IP" field, insert the last used IP address.
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String lastIP = settings.getString(PREFS_ID_LAST_IP, "192.168.0.8");
        serverIpEditText.setText(lastIP);

        // Create the main controller.
        mainController = new MainController(this);
    }

    // Deactivate some buttons.
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CALL)
            return true;
        else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Prevent sleep mode.
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Start the main controller.
        try {
            mainController.start();
        } catch (Exception e) {
            Toast.makeText(this, "The USB transmission could not start.",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Allow the user starting the TCP client.
        serverIpEditText.setEnabled(true);

        // Connect automatically to the computer.
        // This way, it is possible to start the communication just by plugging
        // the ADK (if the auto-start of this application is checked).
        connectToServerCheckBox.setChecked(true);
        onConnectToServerCheckBoxToggled(null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Reallow sleep mode.
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Stop the main controller.
        mainController.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Save the server IP.
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_ID_LAST_IP, serverIpEditText.getText().toString());
        editor.commit();
    }

    public void onConnectToServerCheckBoxToggled(View v) {
        if (connectToServerCheckBox.isChecked()) // Connect.
        {
            mainController.startClient(serverIpEditText.getText().toString());

            serverIpEditText.setEnabled(false);
        } else // Disconnect.
        {
            serverIpEditText.setEnabled(true);

            mainController.stopClient();
        }
    }

    public void updateAccessoryConnectionStatus(String string) {
        speak(string);
        accessoryConnectionStatus.setText(string);
    }

    public void speak(String string) {
        tts.speak(string, TextToSpeech.QUEUE_FLUSH, null);
    }
}
