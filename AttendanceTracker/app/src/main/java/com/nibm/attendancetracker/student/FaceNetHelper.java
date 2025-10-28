package com.nibm.attendancetracker.student;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FaceNetHelper {
    private static final String TAG = "FaceNetHelper";
    private static final String MODEL_FILE = "facenet.tflite";
    private static final int INPUT_SIZE = 160;
    private int EMBEDDING_SIZE = 512;

    private Interpreter interpreter;
    private final Context context;

    public FaceNetHelper(Context context) {
        this.context = context;
        try {
            initializeInterpreter();
            diagnoseModel();
            detectEmbeddingSize();
        } catch (IOException e) {
            Log.e(TAG, "Error initializing interpreter: " + e.getMessage());
        }
    }

    private void initializeInterpreter() throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        ByteBuffer model = FileUtil.loadMappedFile(context, MODEL_FILE);
        interpreter = new Interpreter(model, options);

        Log.d(TAG, "FaceNet model loaded successfully");
    }

    public void diagnoseModel() {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized");
            return;
        }

        try {
            int[] inputShape = interpreter.getInputTensor(0).shape();
            Log.d(TAG, "=== MODEL DIAGNOSTICS ===");
            Log.d(TAG, "Input shape: [" + inputShape[0] + ", " + inputShape[1] + ", " +
                    inputShape[2] + ", " + inputShape[3] + "]");
            Log.d(TAG, "Input type: " + interpreter.getInputTensor(0).dataType());

            int[] outputShape = interpreter.getOutputTensor(0).shape();
            Log.d(TAG, "Output shape: [" + outputShape[0] + ", " + outputShape[1] + "]");
            Log.d(TAG, "Output type: " + interpreter.getOutputTensor(0).dataType());
            Log.d(TAG, "Expected embedding size: " + outputShape[1]);

            int numOutputs = interpreter.getOutputTensorCount();
            Log.d(TAG, "Number of output tensors: " + numOutputs);

            for (int i = 0; i < numOutputs; i++) {
                int[] shape = interpreter.getOutputTensor(i).shape();
                Log.d(TAG, "Output tensor " + i + " shape: " + java.util.Arrays.toString(shape));
            }

            Log.d(TAG, "=========================");

        } catch (Exception e) {
            Log.e(TAG, "Error diagnosing model: " + e.getMessage());
        }
    }

    private void detectEmbeddingSize() {
        if (interpreter == null) return;

        try {
            int[] outputShape = interpreter.getOutputTensor(0).shape();

            Log.d(TAG, "=== MODEL INFO ===");
            Log.d(TAG, "Full output shape: " + java.util.Arrays.toString(outputShape));

            if (outputShape.length >= 2) {
                EMBEDDING_SIZE = outputShape[1];
                Log.d(TAG, "Detected embedding size: " + EMBEDDING_SIZE);
            } else if (outputShape.length == 1) {
                EMBEDDING_SIZE = outputShape[0];
                Log.d(TAG, "Detected embedding size (1D): " + EMBEDDING_SIZE);
            }

            int[] inputShape = interpreter.getInputTensor(0).shape();
            Log.d(TAG, "Input shape: " + java.util.Arrays.toString(inputShape));
            Log.d(TAG, "==================");

        } catch (Exception e) {
            Log.e(TAG, "Error detecting embedding size: " + e.getMessage());
        }
    }


    private Bitmap correctOrientation(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            Log.d(TAG, "EXIF Orientation: " + orientation);
            Log.d(TAG, "Image dimensions before correction: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            Matrix matrix = new Matrix();
            boolean needsTransform = false;

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    Log.d(TAG, "Rotating 90 degrees");
                    matrix.postRotate(90);
                    needsTransform = true;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    Log.d(TAG, "Rotating 180 degrees");
                    matrix.postRotate(180);
                    needsTransform = true;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    Log.d(TAG, "Rotating 270 degrees");
                    matrix.postRotate(270);
                    needsTransform = true;
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    Log.d(TAG, "Flipping horizontal");
                    matrix.postScale(-1, 1);
                    needsTransform = true;
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    Log.d(TAG, "Flipping vertical");
                    matrix.postScale(1, -1);
                    needsTransform = true;
                    break;
                default:
                    Log.d(TAG, "No rotation needed (orientation: " + orientation + ")");
                    return bitmap;
            }

            if (needsTransform) {
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(),
                        matrix, true
                );

                Log.d(TAG, "Image corrected: " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                        " -> " + rotatedBitmap.getWidth() + "x" + rotatedBitmap.getHeight());

                return rotatedBitmap;
            }

            return bitmap;

        } catch (IOException e) {
            Log.e(TAG, "Error reading EXIF: " + e.getMessage());
            return autoRotateIfNeeded(bitmap);
        }
    }

    // ✅ NEW: Fallback rotation based on image dimensions
    private Bitmap autoRotateIfNeeded(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Log.d(TAG, "Auto-rotation check: " + width + "x" + height);

        if (width > height) {
            Log.d(TAG, "Image is landscape, rotating 90 degrees for portrait");
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            Log.d(TAG, "After rotation: " + rotatedBitmap.getWidth() + "x" + rotatedBitmap.getHeight());
            return rotatedBitmap;
        }

        Log.d(TAG, "Image orientation OK");
        return bitmap;
    }

    public float[] getEmbedding(Bitmap bitmap, String imagePath) {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized");
            return null;
        }

        try {
            // Correct orientation first
            Bitmap correctedBitmap = correctOrientation(bitmap, imagePath);

            Bitmap squareBitmap = centerCropToSquare(correctedBitmap);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, INPUT_SIZE, INPUT_SIZE, true);

            ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

            float[][] output = new float[1][EMBEDDING_SIZE];
            interpreter.run(inputBuffer, output);

            float[] embedding = output[0];

            float magnitude = calculateMagnitude(embedding);
            Log.d(TAG, "Raw embedding - Size: " + embedding.length + ", Magnitude: " + magnitude);

            StringBuilder sb = new StringBuilder("First 5 values: ");
            for (int i = 0; i < Math.min(5, embedding.length); i++) {
                sb.append(String.format("%.3f ", embedding[i]));
            }
            Log.d(TAG, sb.toString());

            // Cleanup
            if (correctedBitmap != bitmap) correctedBitmap.recycle();
            if (squareBitmap != correctedBitmap) squareBitmap.recycle();
            resizedBitmap.recycle();

            return embedding;

        } catch (Exception e) {
            Log.e(TAG, "Error generating embedding: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public float[] getEmbedding(Bitmap bitmap) {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized");
            return null;
        }

        try {
            Bitmap squareBitmap = centerCropToSquare(bitmap);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, INPUT_SIZE, INPUT_SIZE, true);

            ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

            float[][] output = new float[1][EMBEDDING_SIZE];
            interpreter.run(inputBuffer, output);

            float[] embedding = output[0];

            float magnitude = calculateMagnitude(embedding);
            Log.d(TAG, "Raw embedding - Size: " + embedding.length + ", Magnitude: " + magnitude);

            StringBuilder sb = new StringBuilder("First 5 values: ");
            for (int i = 0; i < Math.min(5, embedding.length); i++) {
                sb.append(String.format("%.3f ", embedding[i]));
            }
            Log.d(TAG, sb.toString());

            if (squareBitmap != bitmap) squareBitmap.recycle();
            resizedBitmap.recycle();

            return embedding;

        } catch (Exception e) {
            Log.e(TAG, "Error generating embedding: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap centerCropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Log.d(TAG, "Original bitmap size: " + width + "x" + height);

        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, x, y, size, size);
        Log.d(TAG, "Cropped to square: " + size + "x" + size);

        return croppedBitmap;
    }

    public float calculateMagnitude(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            Log.e(TAG, "Invalid embedding for magnitude calculation");
            return 0.0f;
        }

        float magnitude = 0.0f;
        for (float val : embedding) {
            magnitude += val * val;
        }
        return (float) Math.sqrt(magnitude);
    }

    public void normalizeEmbedding(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            Log.e(TAG, "Cannot normalize null or empty embedding");
            return;
        }

        float magnitude = calculateMagnitude(embedding);
        Log.d(TAG, "Normalizing - Before magnitude: " + magnitude);

        if (magnitude > 1e-6f) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] = embedding[i] / magnitude;
            }
            float newMagnitude = calculateMagnitude(embedding);
            Log.d(TAG, "Normalizing - After magnitude: " + newMagnitude);

            if (Math.abs(newMagnitude - 1.0f) > 0.01f) {
                Log.w(TAG, "⚠️ Normalization may have failed. Expected ~1.0, got " + newMagnitude);
            }
        } else {
            Log.e(TAG, "Cannot normalize - magnitude too small: " + magnitude);
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;

            byteBuffer.putFloat((r - 0.5f) * 2.0f);
            byteBuffer.putFloat((g - 0.5f) * 2.0f);
            byteBuffer.putFloat((b - 0.5f) * 2.0f);
        }

        return byteBuffer;
    }

    public float calculateDistance(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null) {
            Log.e(TAG, "Null embedding provided");
            return Float.MAX_VALUE;
        }

        if (embedding1.length != embedding2.length) {
            Log.e(TAG, "Embedding length mismatch: " + embedding1.length + " vs " + embedding2.length);
            return Float.MAX_VALUE;
        }

        float distance = 0.0f;
        for (int i = 0; i < embedding1.length; i++) {
            float diff = embedding1[i] - embedding2[i];
            distance += diff * diff;
        }
        distance = (float) Math.sqrt(distance);

        float mag1 = calculateMagnitude(embedding1);
        float mag2 = calculateMagnitude(embedding2);

        Log.d(TAG, "=== DISTANCE CALCULATION ===");
        Log.d(TAG, "Embedding 1 magnitude: " + mag1);
        Log.d(TAG, "Embedding 2 magnitude: " + mag2);
        Log.d(TAG, "Euclidean distance: " + distance);
        Log.d(TAG, "Embedding size: " + embedding1.length);

        if (mag1 > 1.1f || mag2 > 1.1f) {
            Log.w(TAG, "⚠️ WARNING: Embeddings don't appear to be normalized!");
        }

        Log.d(TAG, "===========================");

        return distance;
    }

    public float calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null || embedding1.length != embedding2.length) {
            Log.e(TAG, "Invalid embeddings for cosine similarity");
            return -1.0f;
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        if (norm1 < 1e-6f || norm2 < 1e-6f) {
            Log.e(TAG, "Cannot calculate cosine similarity - zero norm");
            return -1.0f;
        }

        float similarity = dotProduct / ((float) Math.sqrt(norm1) * (float) Math.sqrt(norm2));
        Log.d(TAG, "Cosine Similarity: " + similarity + " (1.0 = identical, -1.0 = opposite)");

        return similarity;
    }

    public int getEmbeddingSize() {
        return EMBEDDING_SIZE;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
            Log.d(TAG, "Interpreter closed");
        }
    }
}