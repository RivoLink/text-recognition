package mg.rivolink.mnist.task;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import mg.rivolink.mnist.data.MNISTDataset;
import mg.rivolink.mnist.helper.MNISTTrainer;
import mg.rivolink.mnist.tool.MNISTLoader;

public final class MNISTRetrainerTask {

    private static final int DEFAULT_EPOCHS = 2;

    private static final String MODEL_DIR = "data/models/";
    private static final String MNIST_DATA_DIR = "data/mnist";
    private static final String EMNIST_DATA_DIR = "data/emnist";

    private MNISTRetrainerTask() {
        // utility class
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        MNISTDataset.Type datasetType = resolveDatasetType(args);
        MNISTDataset.Paths datasetPaths = resolveDatasetPaths(datasetType);

        int epochs = resolveEpochs(args);
        String modelPath = buildModelPath(datasetType);

        ensureDatasetFilesExist(datasetPaths, datasetType);
        ensureModelExists(modelPath, datasetType);

        System.out.println("=== " + datasetType.description + " Retraining ===");
        System.out.println("Model file : " + modelPath);
        System.out.println("Train data : " + datasetPaths.trainImagesPath);
        System.out.println("Test data  : " + datasetPaths.testImagesPath);
        System.out.println("Epochs     : " + epochs);
        System.out.println();

        System.out.println("Loading training data...");
        MNISTLoader.MNISTData trainingData =
            MNISTLoader.loadTrainingData(datasetPaths.trainImagesPath, datasetPaths.trainLabelsPath);
        System.out.println("Loaded " + trainingData.size() + " training samples");
        System.out.println();

        System.out.println("Loading test data...");
        MNISTLoader.MNISTData testData =
            MNISTLoader.loadTestData(datasetPaths.testImagesPath, datasetPaths.testLabelsPath);
        System.out.println("Loaded " + testData.size() + " test samples");
        System.out.println();

        MNISTTrainer trainer = createTrainer(datasetType);
        trainer.loadModel(modelPath);

        System.out.println("Baseline evaluation...");
        trainer.evaluate(testData);
        System.out.println();

        System.out.println("Resuming training...");
        trainer.train(trainingData, epochs);
        System.out.println();

        System.out.println("Saving model...");
        trainer.saveModel(modelPath);
        System.out.println("Model saved to " + modelPath);
        System.out.println();

        System.out.println("Post-training evaluation...");
        trainer.evaluate(testData);
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

    private static int resolveEpochs(String[] args) {
        if (args != null && args.length > 1) {
            try {
                int value = Integer.parseInt(args[1]);
                if (value > 0) {
                    return value;
                }
                System.out.println("Epochs must be positive. Using default: " + DEFAULT_EPOCHS);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid epoch count '" + args[1] + "'. Using default: " + DEFAULT_EPOCHS);
            }
        }
        return DEFAULT_EPOCHS;
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

    private static MNISTTrainer createTrainer(MNISTDataset.Type datasetType) {
        if (datasetType == MNISTDataset.Type.EMNIST) {
            return new MNISTTrainer(784, 256, 128, datasetType);
        }
        return new MNISTTrainer(784, 128, 64, datasetType);
    }

    private static void ensureDatasetFilesExist(MNISTDataset.Paths paths, MNISTDataset.Type datasetType) {
        boolean missing = false;
        if (!new File(paths.trainImagesPath).exists()) {
            System.err.println("Missing train images: " + paths.trainImagesPath);
            missing = true;
        }
        if (!new File(paths.trainLabelsPath).exists()) {
            System.err.println("Missing train labels: " + paths.trainLabelsPath);
            missing = true;
        }
        if (!new File(paths.testImagesPath).exists()) {
            System.err.println("Missing test images: " + paths.testImagesPath);
            missing = true;
        }
        if (!new File(paths.testLabelsPath).exists()) {
            System.err.println("Missing test labels: " + paths.testLabelsPath);
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

}
