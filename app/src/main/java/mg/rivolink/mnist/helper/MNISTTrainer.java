package mg.rivolink.mnist.helper;

import java.io.IOException;
import java.util.Random;

import mg.rivolink.ai.Network;
import mg.rivolink.ai.Neuron.Activation;
import mg.rivolink.io.NetworkIO;
import mg.rivolink.mnist.data.MNISTDataset;
import mg.rivolink.mnist.tool.MNISTLoader;

/**
 * Trains the neural network on MNIST or EMNIST data
 */
public class MNISTTrainer {

    private static final float MAX_GRADIENT = 0.05f;
    private static final float LEARNING_RATE = 0.01f;

    private Network network;

    private final int numClasses;
    private final Random random = new Random();

    public MNISTTrainer(int inputSize, int hiddenSize, MNISTDataset.Type datasetType) {
        Activation softmax = Activation.SOFTMAX;
        this.numClasses = datasetType.numClasses;
        this.network = new Network.Builder()
            .inputSize(inputSize)
            .hiddenSize(hiddenSize)
            .outputSize(numClasses)
            .outputActivation(softmax)
            .learningRate(LEARNING_RATE)
            .maxGradient(MAX_GRADIENT)
            .build();
    }

    public MNISTTrainer(int inputSize, int hidden1Size, int hidden2Size, MNISTDataset.Type datasetType) {
        Activation softmax = Activation.SOFTMAX;
        this.numClasses = datasetType.numClasses;
        this.network = new Network.Builder()
            .inputSize(inputSize)
            .hiddenSize(hidden1Size)
            .addHiddenLayer(hidden2Size)
            .outputSize(numClasses)
            .outputActivation(softmax)
            .learningRate(LEARNING_RATE)
            .maxGradient(MAX_GRADIENT)
            .build();
    }

    public void train(MNISTLoader.MNISTData trainingData, int epochs) {
        int dataSize = trainingData.size();
        float[] imageBuffer = new float[trainingData.getPixelCount()];

        int[] indices = new int[dataSize];
        for (int idx = 0; idx < dataSize; idx++) {
            indices[idx] = idx;
        }

        System.out.println("Starting training with " + dataSize + " samples for " + epochs + " epochs...");

        for (int epoch = 0; epoch < epochs; epoch++) {
            float totalLoss = 0;
            int correctPredictions = 0;

            // learning-rate decay per epoch
            network.alpha = (float)(LEARNING_RATE * Math.pow(0.95, epoch));

            // shuffle dataset indices
            indices = shuffleIndices(indices);

            for (int idx = 0; idx < dataSize; idx++) {
                int i = indices[idx];

                float[] image = trainingData.getImageAsFloat(i, imageBuffer);
                int label = trainingData.getLabel(i);
                float[] target = MNISTLoader.toOneHotFloat(label, numClasses);

                network.train(image, target);

                float[] prediction = network.predict(image);
                int predictedLabel = argMax(prediction);
                if (predictedLabel == label) {
                    correctPredictions++;
                }

                // Calculate mean squared error loss
                // totalLoss += calculateMSE(prediction, target);

                // Calculate cross-entropy loss
                totalLoss += calculateCrossEntropy(prediction, target);

                if ((i + 1) % 1000 == 0) {
                    System.out.println("  Epoch " + (epoch + 1) + "/" + epochs +
                                       " - Sample " + (i + 1) + "/" + dataSize);
                }
            }

            float avgLoss = totalLoss / dataSize;
            float accuracy = (float) correctPredictions / dataSize;
            System.out.println("Epoch " + (epoch + 1) + "/" + epochs +
                               " - Loss: " + String.format("%.4f", avgLoss) +
                               " - Accuracy: " + String.format("%.2f%%", accuracy * 100));
        }
    }

    public float evaluate(MNISTLoader.MNISTData testData) {
        int correctPredictions = 0;
        int totalSamples = testData.size();
        float[] imageBuffer = new float[testData.getPixelCount()];

        System.out.println("Evaluating on " + totalSamples + " test samples...");

        for (int i = 0; i < totalSamples; i++) {
            float[] image = testData.getImageAsFloat(i, imageBuffer);
            int label = testData.getLabel(i);

            float[] prediction = network.predict(image);
            int predictedLabel = argMax(prediction);

            if (predictedLabel == label) {
                correctPredictions++;
            }

            if ((i + 1) % 1000 == 0) {
                System.out.println("  Evaluated " + (i + 1) + "/" + totalSamples);
            }
        }

        float accuracy = (float) correctPredictions / totalSamples;
        System.out.println("Test Accuracy: " + String.format("%.2f%%", accuracy * 100));
        return accuracy;
    }

    public Network getNetwork() {
        return network;
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

    // Fisher-Yates shuffle
    private int[] shuffleIndices(int[] indices) {
        for (int i = indices.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;
        }
        return indices;
    }

    // MSE loss for regression
    @SuppressWarnings("unused")
    private float calculateMSE(float[] prediction, float[] target) {
        float mse = 0;
        for (int i = 0; i < prediction.length; i++) {
            float error = prediction[i] - target[i];
            mse += error * error;
        }
        return mse / prediction.length;
    }

    // Cross-entropy loss for classification
    private float calculateCrossEntropy(float[] prediction, float[] target) {
        float loss = 0;
        for (int i = 0; i < prediction.length; i++) {
            if (target[i] == 1) {
                // Add small epsilon to prevent log(0)
                loss = -(float)Math.log(Math.max(prediction[i], 1e-7));
                break;
            }
        }
        return loss;
    }

    public void saveModel(String filePath) throws IOException {
        NetworkIO.save(network, filePath, NetworkIO.Format.BINARY);
    }

    public void loadModel(String filePath) throws IOException, ClassNotFoundException {
        this.network = NetworkIO.load(filePath, NetworkIO.Format.BINARY);
    }

}
