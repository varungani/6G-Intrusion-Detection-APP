package com.example.tsanetapp;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.content.Context;
import android.net.VpnService;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView featureTextView;
    private boolean isVpnRunning = false;
    private BroadcastReceiver predictionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        featureTextView = findViewById(R.id.featureTextView);
        Button startVpnBtn = findViewById(R.id.startVpnBtn);
        Button stopVpnBtn = findViewById(R.id.stopVpnBtn);

        startVpnBtn.setOnClickListener(v -> {
            Intent vpnIntent = VpnService.prepare(this);
            if (vpnIntent != null) {
                startActivityForResult(vpnIntent, 0);
            } else {
                onActivityResult(0, RESULT_OK, null);
            }
        });

        stopVpnBtn.setOnClickListener(v -> {
            stopService(new Intent(this, MyVpnService.class));
            isVpnRunning = false;
            featureTextView.setText("VPN Stopped");
        });

        // Receive prediction result broadcast
        predictionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String result = intent.getStringExtra("prediction_result");
                featureTextView.setText("Prediction: " + result);
            }
        };
        registerReceiver(predictionReceiver, new IntentFilter("com.example.tsanetapp.PREDICTION"), RECEIVER_EXPORTED);

    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            startService(new Intent(this, MyVpnService.class));
            isVpnRunning = true;
            featureTextView.setText("VPN Started...");
        }
        super.onActivityResult(request, result, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (predictionReceiver != null) {
            unregisterReceiver(predictionReceiver);
        }
    }
}
