package mg.rivolink.mnist.helper;

import java.io.IOException;

import mg.rivolink.ai.Network;
import mg.rivolink.ai.Neuron.Activation;
import mg.rivolink.io.NetworkIO;
import mg.rivolink.mnist.data.MNISTDataset;
import mg.rivolink.mnist.tool.MNISTLoader;

/**
 * Trains the neural network on MNIST or EMNIST data
 */
public class MNISTTrainer {

    private Network network;
    private final int numClasses;

    public MNISTTrainer(int inputSize, int hiddenSize, MNISTDataset.Type datasetType) {
        Activation softmax = Activation.SOFTMAX;
        this.numClasses = datasetType.numClasses;
        this.network = new Network.Builder()
            .inputSize(inputSize)
            .hiddenSize(hiddenSize)
            .outputSize(numClasses)
            .outputActivation(softmax)
            .learningRate(0.1f)
            .maxGradient(5.0f)
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
            .learningRate(0.05f)
            .maxGradient(5.0f)
            .build();
    }

    public void train(MNISTLoader.MNISTData trainingData, int epochs) {
        int dataSize = trainingData.images.length;

        System.out.println("Starting training with " + dataSize + " samples for " + epochs + " epochs...");

        for (int epoch = 0; epoch < epochs; epoch++) {
            float totalLoss = 0;
            int correctPredictions = 0;

            for (int i = 0; i < dataSize; i++) {
                float[] image = trainingData.images[i];
                int label = trainingData.labels[i];
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
        int totalSamples = testData.images.length;

        System.out.println("Evaluating on " + totalSamples + " test samples...");

        for (int i = 0; i < totalSamples; i++) {
            float[] image = testData.images[i];
            int label = testData.labels[i];

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
