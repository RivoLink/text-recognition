package mg.rivolink.mnist.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mg.rivolink.ai.Network;
import mg.rivolink.io.NetworkIO;
import mg.rivolink.mnist.data.MNISTDataset;
import mg.rivolink.mnist.helper.MNISTPredictor;
import mg.rivolink.mnist.tool.MNISTLoader;

public final class MNISTEvaluatorTask {

    private static final int SAMPLE_COUNT = 5;

    private static final String MODEL_DIR = "data/models/";
    private static final String MNIST_DATA_DIR = "data/mnist";
    private static final String EMNIST_DATA_DIR = "data/emnist";

    private MNISTEvaluatorTask() {
        // utility class
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        MNISTDataset.Type datasetType = resolveDatasetType(args);
        MNISTDataset.Paths datasetPaths = resolveDatasetPaths(datasetType);

        String testImagesPath = datasetPaths.testImagesPath;
        String testLabelsPath = datasetPaths.testLabelsPath;
        ensureDatasetFilesExist(testImagesPath, testLabelsPath, datasetType);

        String modelPath = buildModelPath(datasetType);
        ensureModelExists(modelPath, datasetType);

        System.out.println("=== " + datasetType.description + " Evaluation ===");
        System.out.println();
        System.out.println("Model file : " + modelPath);
        System.out.println("Test images: " + testImagesPath);
        System.out.println("Test labels: " + testLabelsPath);
        System.out.println();

        System.out.println("Loading test data...");
        MNISTLoader.MNISTData testData = MNISTLoader.loadTestData(testImagesPath, testLabelsPath);

        System.out.println("Loaded " + testData.size() + " test samples.");
        System.out.println();

        System.out.println("Loading model...");
        Network network = NetworkIO.load(modelPath);

        System.out.println("Model loaded.");
        System.out.println();

        long startNs = System.nanoTime();
        EvaluationSummary summary = runEvaluation(network, testData, datasetType);
        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;

        printSummary(summary, durationMs);
    }

    private static EvaluationSummary runEvaluation(
        Network network, MNISTLoader.MNISTData testData, MNISTDataset.Type datasetType
    ) {
        int correct = 0;
        int incorrect = 0;
        int totalSamples = testData.size();

        double confidenceSum = 0;
        double correctConfidenceSum = 0;
        double incorrectConfidenceSum = 0;

        MNISTPredictor predictor = new MNISTPredictor(network, datasetType);

        List<PredictionSummary> firstSamples = new ArrayList<>(SAMPLE_COUNT);
        List<PredictionSummary> firstMistakes = new ArrayList<>(SAMPLE_COUNT);

        System.out.println("Evaluating on " + totalSamples + " samples...");

        float[] sampleBuffer = new float[testData.getPixelCount()];
        for (int i = 0; i < totalSamples; i++) {
            float[] sample = testData.getImageAsFloat(i, sampleBuffer);
            MNISTPredictor.PredictionResult result = predictor.predictWithConfidence(sample);

            int actualLabel = testData.getLabel(i);
            boolean match = result.labelIndex == actualLabel;

            confidenceSum += result.confidence;

            if (match) {
                correct++;
                correctConfidenceSum += result.confidence;
            } else {
                incorrect++;
                incorrectConfidenceSum += result.confidence;
            }

            String label = labelToString(actualLabel, datasetType);
            PredictionSummary summary = new PredictionSummary(
                i,
                label,
                result.prediction,
                result.confidence,
                match
            );

            if (!match && firstMistakes.size() < SAMPLE_COUNT) {
                firstMistakes.add(summary);
            }

            if (firstSamples.size() < SAMPLE_COUNT) {
                firstSamples.add(summary);
            }

            if ((i + 1) % 1000 == 0 || i == totalSamples - 1) {
                System.out.println("  Processed " + (i + 1) + "/" + totalSamples);
            }
        }

        return new EvaluationSummary(
            totalSamples,
            correct,
            incorrect,
            confidenceSum,
            correctConfidenceSum,
            incorrectConfidenceSum,
            firstSamples,
            firstMistakes
        );
    }

    private static void printSummary(EvaluationSummary summary, long durationMs) {
        double accuracy = summary.totalSamples == 0 ? 0.0 : (double) summary.correctSamples / summary.totalSamples;

        System.out.println();
        System.out.println("Results");
        System.out.println("  Correct predictions : " + summary.correctSamples + "/" + summary.totalSamples);
        System.out.println("  Accuracy            : " + formatPercent(accuracy));
        System.out.println("  Average confidence  : " + formatPercent(safeAverage(summary.confidenceSum, summary.totalSamples)));

        if (summary.correctSamples > 0) {
            System.out.println("    Correct only      : " + formatPercent(
                safeAverage(summary.correctConfidenceSum, summary.correctSamples)
            ));
        }

        if (summary.incorrectSamples > 0) {
            System.out.println("    Misclassifications: " + formatPercent(
                safeAverage(summary.incorrectConfidenceSum, summary.incorrectSamples)
            ));
        } else {
            System.out.println("    Misclassifications: n/a (perfect accuracy)");
        }

        double seconds = durationMs / 1000.0;
        double samplesPerSecond = seconds > 0 ? summary.totalSamples / seconds : summary.totalSamples;

        System.out.println("  Evaluation time     : " + durationMs + " ms (" +
                           String.format("%.1f", samplesPerSecond) + " samples/s)");
        System.out.println();

        printPredictions("Sample predictions", summary.firstSamples);
        if (!summary.firstMistakes.isEmpty()) {
            printPredictions("First misclassifications", summary.firstMistakes);
        } else {
            System.out.println("First misclassifications: none detected in evaluation set.");
            System.out.println();
        }
    }

    private static void printPredictions(String title, List<PredictionSummary> summaries) {
        System.out.println(title + ":");
        for (PredictionSummary summary : summaries) {
            System.out.println(
                "  #" + summary.sampleIndex +
                " -> predicted " + summary.predictedLabel +
                " (" + formatPercent(summary.confidence) + ")" +
                " | actual " + summary.actualLabel +
                (summary.correct ? " [OK]" : " [MISS]")
            );
        }
        System.out.println();
    }

    private static double safeAverage(double sum, int count) {
        if (count == 0) {
            return 0.0;
        }
        return sum / count;
    }

    private static String labelToString(int label, MNISTDataset.Type datasetType) {
        if (datasetType == MNISTDataset.Type.EMNIST) {
            if (label >= 0 && label <= 9) {
                return String.valueOf(label);
            }
            if (label >= 10 && label <= 35) {
                return String.valueOf((char) ('A' + (label - 10)));
            }
            if (label >= 36 && label <= 61) {
                return String.valueOf((char) ('a' + (label - 36)));
            }
        }
        return String.valueOf(label);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0);
    }

    private static MNISTDataset.Type resolveDatasetType(String[] args) {
        if (args != null && args.length > 0) {
            String requested = args[0].trim().toLowerCase(Locale.ROOT);
            if ("mnist".equals(requested)) {
                return MNISTDataset.Type.MNIST;
            }
            if ("emnist".equals(requested)) {
                return MNISTDataset.Type.EMNIST;
            }
            System.out.println("Unknown dataset '" + args[0] + "'. Falling back to EMNIST.");
        }

        return MNISTDataset.Type.EMNIST;
    }

    private static MNISTDataset.Paths resolveDatasetPaths(MNISTDataset.Type datasetType) {
        if (datasetType == MNISTDataset.Type.EMNIST) {
            return MNISTDataset.resolvePaths(EMNIST_DATA_DIR, datasetType);
        }
        return MNISTDataset.resolvePaths(MNIST_DATA_DIR, datasetType);
    }

    private static String buildModelPath(MNISTDataset.Type datasetType) {
        String modelFile = datasetType.name().toLowerCase(Locale.ROOT) + "-model.bin";
        return MODEL_DIR + modelFile;
    }

    private static void ensureDatasetFilesExist(
        String testImagesPath, String testLabelsPath, MNISTDataset.Type datasetType
    ) {
        boolean missing = false;
        if (!new File(testImagesPath).exists()) {
            System.err.println("Missing test images: " + testImagesPath);
            missing = true;
        }
        if (!new File(testLabelsPath).exists()) {
            System.err.println("Missing test labels: " + testLabelsPath);
            missing = true;
        }
        if (missing) {
            String datasetKey = datasetType.name().toLowerCase(Locale.ROOT);
            System.err.println("Please download the dataset with:");
            System.err.println("  ./gradlew downloadDataset -Pdataset=" + datasetKey);
            System.exit(1);
        }
    }

    private static void ensureModelExists(String modelPath, MNISTDataset.Type datasetType) {
        if (new File(modelPath).exists()) {
            return;
        }
        String datasetKey = datasetType.name().toLowerCase(Locale.ROOT);
        System.err.println("Model not found: " + modelPath);
        System.err.println("Train the model with:");
        System.err.println("  ./gradlew trainModel -Pdataset=" + datasetKey);
        System.exit(1);
    }

    private static final class PredictionSummary {

        private final int sampleIndex;
        private final String actualLabel;
        private final String predictedLabel;
        private final double confidence;
        private final boolean correct;

        private PredictionSummary(
            int sampleIndex, String actualLabel, String predictedLabel,
            double confidence, boolean correct
        ) {
            this.sampleIndex = sampleIndex;
            this.actualLabel = actualLabel;
            this.predictedLabel = predictedLabel;
            this.confidence = confidence;
            this.correct = correct;
        }
    }

    private static final class EvaluationSummary {

        private final int totalSamples;
        private final int correctSamples;
        private final int incorrectSamples;
        private final double confidenceSum;
        private final double correctConfidenceSum;
        private final double incorrectConfidenceSum;
        private final List<PredictionSummary> firstSamples;
        private final List<PredictionSummary> firstMistakes;

        private EvaluationSummary(
            int totalSamples, int correctSamples, int incorrectSamples,
            double confidenceSum, double correctConfidenceSum, double incorrectConfidenceSum,
            List<PredictionSummary> firstSamples, List<PredictionSummary> firstMistakes
        ) {
            this.totalSamples = totalSamples;
            this.correctSamples = correctSamples;
            this.incorrectSamples = incorrectSamples;
            this.confidenceSum = confidenceSum;
            this.correctConfidenceSum = correctConfidenceSum;
            this.incorrectConfidenceSum = incorrectConfidenceSum;
            this.firstSamples = firstSamples;
            this.firstMistakes = firstMistakes;
        }
    }

}
