// TSANetModel.java
package com.example.tsanetapp;

import android.content.Context;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.Device; // Optional if you decide to specify CPU explicitly

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

public class TSANetModel {

    private Module model = null;
    private Context context;

    public TSANetModel(Context context) {
        this.context = context;
        try {
            String modelPath = assetFilePath(context, "tsanet_model_android_cpu (1).pt"); // Use the pruned model
            // Load the model onto the CPU (explicitly or just with one arg)
            model = Module.load(modelPath);  // Or: Module.load(modelPath, Device.CPU);
            Log.i("TSANetModel", "Model loaded successfully on CPU.");
        } catch (Exception e) {
            Log.e("TSANetModel", "Model loading failed", e);
            e.printStackTrace();
        }
    }

    public String predict(float[] features) {
        Log.d("TSANetModel", "Input features size: " + features.length);
        if (features.length != 32) {
            Log.e("TSANetModel", "Input features array must have a length of 32.");
            return "Prediction Failed: Incorrect input size";
        }

        long[] shape = new long[]{1, 32, 1}; // Match the expected input shape
        try {
            // 1. Create a direct ByteBuffer with the required capacity
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(features.length * Float.BYTES);
            byteBuffer.order(ByteOrder.nativeOrder()); // Ensure native byte order

            // 2. Create a FloatBuffer view of the ByteBuffer and put the float array into it
            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
            floatBuffer.put(features);

            // 3. Rewind the FloatBuffer (important for reading from the beginning)
            floatBuffer.rewind();

            // 4. Create the Tensor from the FloatBuffer and the shape
            Tensor inputTensor = Tensor.fromBlob(floatBuffer, shape);
            Log.d("TSANetModel", "Input Tensor shape: " + Arrays.toString(inputTensor.shape()));

            // Perform inference
            IValue outputIValue = model.forward(IValue.from(inputTensor));

            // Check the actual type of the output and handle accordingly
            Tensor outputTensor;
            if (outputIValue.isTuple()) {
                final IValue[] outputTuple = outputIValue.toTuple();
                outputTensor = outputTuple[0].toTensor(); // Assuming the first element of the tuple is the output tensor
            } else if (outputIValue.isTensor()) {
                outputTensor = outputIValue.toTensor();
            } else {
                Log.e("TSANetModel", "Unexpected output IValue type. isTensor(): " + outputIValue.isTensor() + ", isTuple(): " + outputIValue.isTuple());
                return "Prediction Failed: Unexpected model output";
            }

            float[] scores = outputTensor.getDataAsFloatArray();
            Log.d("TSANetModel", "Raw Scores: " + Arrays.toString(scores));

            // Get class with max score
            float maxScore = -Float.MAX_VALUE;
            int predictedClass = -1;
            for (int i = 0; i < scores.length; i++) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i];
                    predictedClass = i;
                }
            }

            return "Predicted Class: " + predictedClass + " (Confidence: " + maxScore + ")";

        } catch (Exception e) {
            Log.e("TSANetModel", "Prediction failed (Ask Gemini)", e);
            e.printStackTrace();
            return "Prediction Failed: " + e.getMessage();
        }
    }

    private static String assetFilePath(Context context, String assetName) throws Exception {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName);
             OutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        }

        return file.getAbsolutePath();
    }
}