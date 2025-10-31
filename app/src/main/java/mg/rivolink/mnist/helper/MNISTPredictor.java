package mg.rivolink.mnist.helper;

import mg.rivolink.ai.Network;

/**
 * Makes predictions on new handwritten digit or letter images
 */
public class MNISTPredictor {

    private Network network;
    private MNISTTrainer.DatasetType datasetType;

    public MNISTPredictor(Network network, MNISTTrainer.DatasetType datasetType) {
        this.network = network;
        this.datasetType = datasetType;
    }

    public int predict(float[] imagePixels) {
        if (imagePixels.length != network.inputSize) {
            throw new IllegalArgumentException(
                "Image size mismatch: expected " + network.inputSize +
                " pixels, got " + imagePixels.length
            );
        }

        float[] output = network.predict(imagePixels);
        return argMax(output);
    }

    public PredictionResult predictWithConfidence(float[] imagePixels) {
        if (imagePixels.length != network.inputSize) {
            throw new IllegalArgumentException(
                "Image size mismatch: expected " + network.inputSize +
                " pixels, got " + imagePixels.length
            );
        }

        float[] output = network.predict(imagePixels);
        int predictedLabel = argMax(output);
        float confidence = output[predictedLabel];
        String prediction = labelToString(predictedLabel);

        return new PredictionResult(predictedLabel, prediction, confidence, output);
    }

    /**
     * Converts label index to character/digit string
     */
    private String labelToString(int label) {
        if (datasetType == MNISTTrainer.DatasetType.EMNIST) {
            if (label >= 0 && label <= 9) {
                return String.valueOf(label);
            } else if (label >= 10 && label <= 35) {
                return String.valueOf((char)('A' + (label - 10)));
            } else if (label >= 36 && label <= 61) {
                return String.valueOf((char)('a' + (label - 36)));
            }
        }
        return String.valueOf(label);
    }

    public static class PredictionResult {
        public int labelIndex;
        public String prediction;
        public float confidence;
        public float[] allProbabilities;

        public PredictionResult(int labelIndex, String prediction, float confidence, float[] allProbabilities) {
            this.labelIndex = labelIndex;
            this.prediction = prediction;
            this.confidence = confidence;
            this.allProbabilities = allProbabilities;
        }

        @Override
        public String toString() {
            return "Predicted: " + prediction + ", Confidence: " + String.format("%.2f%%", confidence * 100);
        }
    }

    private int argMax(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

}
