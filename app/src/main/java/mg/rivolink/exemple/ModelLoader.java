package mg.rivolink.exemple;

import java.io.IOException;

import mg.rivolink.ai.Network;
import mg.rivolink.mnist.model.MNISTPredictor;
import mg.rivolink.mnist.model.MNISTTrainer;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * Utility class for model loading
 */
class ModelLoader {

    /**
     * Load MNIST model
     */
    public static MNISTPredictor loadMNISTModel(String modelPath) throws IOException, ClassNotFoundException {
        Network network = loadNetwork(modelPath);
        return new MNISTPredictor(network, MNISTTrainer.DatasetType.MNIST);
    }

    /**
     * Load EMNIST model
     */
    public static MNISTPredictor loadEMNISTModel(String modelPath) throws IOException, ClassNotFoundException {
        Network network = loadNetwork(modelPath);
        return new MNISTPredictor(network, MNISTTrainer.DatasetType.EMNIST);
    }

    /**
     * Load Network from file
     */
    public static Network loadNetwork(String filepath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath))) {
            Network network = (Network) ois.readObject();
            System.out.println("Loaded network from: " + filepath);
            return network;
        }
    }

    /**
     * Check if model file exists
     */
    public static boolean modelExists(String filepath) {
        return new java.io.File(filepath).exists();
    }
}
