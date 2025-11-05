package mg.rivolink.mnist.task;

import java.io.IOException;
import java.util.Locale;

import mg.rivolink.mnist.data.MNISTDataset;
import mg.rivolink.mnist.tool.MNISTLoader;
import mg.rivolink.mnist.helper.MNISTPredictor;
import mg.rivolink.mnist.helper.MNISTTrainer;

public final class MNISTTrainerTask {

    private static final String MODEL_DIR = "data/models/";
    private static final String MNIST_DATA_DIR = "data/mnist";
    private static final String EMNIST_DATA_DIR = "data/emnist";

    private MNISTTrainerTask() {
        // utility class
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        MNISTDataset.Type datasetType = resolveDatasetType(args);
        MNISTDataset.Paths datasetPaths = resolveDatasetPaths(datasetType);

        String modelPath = buildModelPath(datasetType);

        System.out.println("=== " + datasetType.description + " Recognition ===");
        System.out.println();

        System.out.println("Loading training data...");
        MNISTLoader.MNISTData trainingData =
            MNISTLoader.loadTrainingData(datasetPaths.trainImagesPath, datasetPaths.trainLabelsPath);

        System.out.println("Loaded " + trainingData.images.length + " training samples");
        System.out.println();

        System.out.println("Loading test data...");
        MNISTLoader.MNISTData testData =
            MNISTLoader.loadTestData(datasetPaths.testImagesPath, datasetPaths.testLabelsPath);

        System.out.println("Loaded " + testData.images.length + " test samples");
        System.out.println();

        MNISTTrainer trainer;
        if (datasetType == MNISTDataset.Type.EMNIST) {
            trainer = new MNISTTrainer(784, 256, 128, datasetType);
        } else {
            trainer = new MNISTTrainer(784, 128, 64, datasetType);
        }

        // Train model
        trainer.train(trainingData, 5);
        System.out.println();

        // Evaluate model
        trainer.evaluate(testData);
        System.out.println();

        // Save model
        System.out.println("Saving model...");
        trainer.saveModel(modelPath);

        System.out.println();

        // Make predictions
        MNISTPredictor predictor = new MNISTPredictor(trainer.getNetwork(), datasetType);

        System.out.println("Making predictions on test samples:");
        for (int i = 0; i < 5; i++) {
            MNISTPredictor.PredictionResult result =
                predictor.predictWithConfidence(testData.images[i]);

            System.out.println("Sample " + i + ": " + result +
                               " (Actual: " + testData.labels[i] + ")");
        }
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

}
