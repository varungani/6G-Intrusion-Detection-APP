// MyVpnService.java
package com.example.tsanetapp;

// ... other imports
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MyVpnService extends VpnService {

    private static final String TAG = "MyVpnService";
    private ParcelFileDescriptor vpnInterface = null;
    private Thread vpnThread;
    // Assuming you have your model loading and prediction logic in a class named 'ModelHandler'
    private ModelHandler model;

    @Override
    public void onCreate() {
        super.onCreate();
        model = new ModelHandler(getApplicationContext()); // Initialize your model handler
        model.loadModel(); // Load your trained model
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        vpnThread = new Thread(this::runVPN, "VPNThread");
        vpnThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (vpnThread != null) {
            vpnThread.interrupt();
            try {
                vpnThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error joining VPN thread", e);
            }
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN interface", e);
            }
        }
        if (model != null) {
            model.unloadModel(); // Unload the model when the service is destroyed
        }
        super.onDestroy();
    }

    public void runVPN() {
        try {
            Builder builder = new Builder();
            builder.addAddress("10.0.0.2", 24);
            builder.addRoute("0.0.0.0", 0);
            builder.setMtu(1500);
            builder.addDnsServer("8.8.8.8");
            builder.addDnsServer("8.8.4.4");
            vpnInterface = builder.establish();

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface");
                return;
            }

            FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
            ByteBuffer buffer = ByteBuffer.allocate(32767);

            while (!Thread.interrupted()) {
                int length = in.read(buffer.array());
                if (length > 0) {
                    buffer.limit(length);

                    // Step 1: Parse packet
                    PacketParser.ParsedPacketData parsedData = PacketParser.parsePacket(buffer.asReadOnlyBuffer());
                    if (parsedData != null) {
                        Log.d(TAG, "Parsed Data: " + parsedData.srcIp + " -> " + parsedData.destIp);

                        // Step 2: Extract features (using ParsedPacketData object)
                        String features = FeatureExtractor.extractFeatures(parsedData);
                        Log.d(TAG, "Extracted Features (String): " + features);

                        // Step 3: Convert the features String to a float array
                        String[] featureStrings = features.split(",");
                        if (featureStrings.length != 32) {
                            Log.e(TAG, "Error: Extracted features string does not contain 32 values. Found: " + featureStrings.length);
                            continue; // Skip prediction
                        }

                        float[] featuresArray = new float[32];
                        try {
                            for (int i = 0; i < 32; i++) {
                                featuresArray[i] = Float.parseFloat(featureStrings[i].trim());
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing feature string to float: " + e.getMessage());
                            continue; // Skip prediction
                        }
                        Log.d(TAG, "Features Array: " + Arrays.toString(featuresArray));

                        // Step 4: Predict using the float array directly
                        String prediction = model.predict(featuresArray);

                        // Step 5: Log + Send broadcast
                        Log.d(TAG, "Prediction: " + prediction);
                        Intent intent = new Intent("com.example.tsanetapp.PREDICTION");
                        intent.putExtra("prediction_result", prediction);
                        sendBroadcast(intent);
                    }
                }

                // Sleep briefly to avoid busy-waiting
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "VPN error", e);
        } finally {
            if (vpnInterface != null) {
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing VPN interface", e);
                }
            }
        }
    }

    // You'll need to implement your model loading and prediction logic in this class or a separate 'ModelHandler' class
    private static class ModelHandler {
        private android.content.Context context;
        // Placeholder for your actual model
        private Object trainedModel;

        public ModelHandler(android.content.Context context) {
            this.context = context;
        }

        public void loadModel() {
            // Implement your model loading logic here (e.g., from assets)
            Log.d(TAG, "Model loaded (placeholder)");
            trainedModel = new Object(); // Replace with your actual model loading
        }

        public String predict(float[] features) {
            // Implement your model prediction logic here
            Log.d(TAG, "Predicting with features: " + Arrays.toString(features));
            // Placeholder prediction logic
            if (features[0] > 0.5) {
                return "malicious";
            } else {
                return "benign";
            }
        }

        public void unloadModel() {
            // Implement logic to release model resources if needed
            Log.d(TAG, "Model unloaded (placeholder)");
            trainedModel = null;
        }
    }
}