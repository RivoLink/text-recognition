package mg.rivolink;

import java.io.IOException;

import mg.rivolink.mnist.data.MNISTDataLoader;
import mg.rivolink.mnist.model.MNISTPredictor;
import mg.rivolink.mnist.model.MNISTTrainer;

/**
 * MNISTExample - Example usage of the MNIST/EMNIST system
 */
public class MNISTExample {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Choose dataset type
        MNISTTrainer.DatasetType datasetType = MNISTTrainer.DatasetType.EMNIST;
        
        String trainImagesPath = datasetType == MNISTTrainer.DatasetType.EMNIST ?
            "emnist-byclass-train-images-idx3-ubyte" : "train-images-idx3-ubyte";
        String trainLabelsPath = datasetType == MNISTTrainer.DatasetType.EMNIST ?
            "emnist-byclass-train-labels-idx1-ubyte" : "train-labels-idx1-ubyte";
        String testImagesPath = datasetType == MNISTTrainer.DatasetType.EMNIST ?
            "emnist-byclass-test-images-idx3-ubyte" : "t10k-images-idx3-ubyte";
        String testLabelsPath = datasetType == MNISTTrainer.DatasetType.EMNIST ?
            "emnist-byclass-test-labels-idx1-ubyte" : "t10k-labels-idx1-ubyte";
        String modelPath = datasetType == MNISTTrainer.DatasetType.EMNIST ?
            "emnist_model.ser" : "mnist_model.ser";

        System.out.println("=== " + datasetType.description + " Recognition ===\n");

        // Load training data
        System.out.println("Loading training data...");
        MNISTDataLoader.MNISTData trainingData = 
            MNISTDataLoader.loadTrainingData("data/emnist_data/" + trainImagesPath, "data/emnist_data/" + trainLabelsPath);
        System.out.println("Loaded " + trainingData.images.length + " training samples\n");

        // Load test data
        System.out.println("Loading test data...");
        MNISTDataLoader.MNISTData testData = 
            MNISTDataLoader.loadTestData("data/emnist_data/" + testImagesPath, "data/emnist_data/" + testLabelsPath);
        System.out.println("Loaded " + testData.images.length + " test samples\n");

        // Create and train model
        MNISTTrainer trainer;
        if (datasetType == MNISTTrainer.DatasetType.EMNIST) {
            trainer = new MNISTTrainer(784, 256, 128, datasetType);
        } else {
            trainer = new MNISTTrainer(784, 128, 64, datasetType);
        }
        
        trainer.train(trainingData, 5);

        System.out.println();

        // Evaluate model
        trainer.evaluate(testData);

        System.out.println();

        // Save model
        System.out.println("Saving model...");
        trainer.saveModel("data/models/" + modelPath);

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
}
