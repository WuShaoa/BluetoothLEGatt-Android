package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MotionClassifier {
    // Display preferences
    public static final float GOOD_PROB_THRESHOLD = 0.5f;

    /** Tag for the {@link Log}. */
    private static final String TAG = "MotionClassifier";

    /** Number of results to show in the UI. */
    private static final int RESULTS_TO_SHOW = 3;

    /** Dimensions of inputs. */
    private static final int DIM_BATCH_SIZE = 1;

    private static final int DIM_DATA_SIZE = 18;

    /** Labels corresponding to the output of the vision model. */
    private List<String> labelList = Arrays.asList("Falling", "Other");

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
    private float[][][] sensorDataInput = new float[DIM_BATCH_SIZE][1][DIM_DATA_SIZE];;
    private float[][] predictionOutput;


    private String mModelPath = "my_model2.tflite";

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    private Interpreter tflite;

    /** Options for configuring the Interpreter.*/
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    /** An instance of host activity.*/
    private Activity activity = null;

    MotionClassifier(Activity activity) throws IOException {
        // TODO(b/169965231): Add support for delegates.
        this.activity = activity;

        initializeModel();

        predictionOutput = new float[1][1];
        Log.d(TAG, "Created a MotionClassifier.");
    }

    /** Classifies a frame from the preview stream. */
    public void classifyMotion(float[] input, MotionClassifierCallBack callback) {
        //printTopKLabels(builder);

        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
//            builder.append(new SpannableString("Uninitialized Classifier."));
        }

        sensorDataInput[0][0] = Arrays.copyOf(input, DIM_DATA_SIZE);

        // Here's where the magic happens!!!
        long startTime = SystemClock.uptimeMillis();
        tflite.run(sensorDataInput, predictionOutput);
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));

        callback.onPredictionResult(predictionOutput[0], endTime - startTime);
    }

    public float[] classifyMotion(float[] input){
        sensorDataInput[0][0] = Arrays.copyOf(input, DIM_DATA_SIZE);

        tflite.run(sensorDataInput, predictionOutput);

        return predictionOutput[0];
    }

    private void initializeModel() throws IOException {
        if (tflite == null) {
            MappedByteBuffer tfLiteModel = FileUtil.loadMappedFile(activity, mModelPath);
            tflite = new Interpreter(tfLiteModel, tfliteOptions);
        }
    }

    public void setModelByPath(String path) throws IOException {
        mModelPath = path;
        initializeModel();
    }

    public List<String> getLabels() {
        return labelList;
    }

    public void setLabels(List<String> l){ labelList = l;}

    interface MotionClassifierCallBack {
        void onPredictionResult(float[] result,long duration);
    }
}
