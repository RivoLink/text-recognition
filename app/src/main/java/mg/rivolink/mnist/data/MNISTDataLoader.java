package mg.rivolink.mnist.data;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * MNISTDataLoader - Loads and processes MNIST dataset
 */
public class MNISTDataLoader {

    public static class MNISTData {
        public float[][] images;
        public int[] labels;
        
        public MNISTData(float[][] images, int[] labels) {
            this.images = images;
            this.labels = labels;
        }
    }

    public static MNISTData loadTrainingData(String imagePath, String labelPath) throws IOException {
        return loadData(imagePath, labelPath);
    }

    public static MNISTData loadTestData(String imagePath, String labelPath) throws IOException {
        return loadData(imagePath, labelPath);
    }

    private static MNISTData loadData(String imagePath, String labelPath) throws IOException {
        int[] labels = readLabels(labelPath);
        float[][] images = readImages(imagePath, labels.length);
        return new MNISTData(images, labels);
    }

    private static int[] readLabels(String filePath) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(filePath));
        
        int magicNumber = dis.readInt();
        int numLabels = dis.readInt();
        
        int[] labels = new int[numLabels];
        for (int i = 0; i < numLabels; i++) {
            labels[i] = dis.readUnsignedByte();
        }
        dis.close();
        return labels;
    }

    private static float[][] readImages(String filePath, int numImages) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(filePath));
        
        int magicNumber = dis.readInt();
        int numImagesFile = dis.readInt();
        int rows = dis.readInt();
        int cols = dis.readInt();
        
        int pixelCount = rows * cols;
        float[][] images = new float[numImages][pixelCount];
        
        for (int i = 0; i < numImages; i++) {
            for (int j = 0; j < pixelCount; j++) {
                int pixel = dis.readUnsignedByte();
                images[i][j] = pixel / 255.0f;
            }
        }
        dis.close();
        return images;
    }

    public static float[][] normalizeImages(float[][] images) {
        return images;
    }

    public static int[] toOneHot(int label, int numClasses) {
        int[] oneHot = new int[numClasses];
        oneHot[label] = 1;
        return oneHot;
    }

    public static float[] toOneHotFloat(int label, int numClasses) {
        float[] oneHot = new float[numClasses];
        oneHot[label] = 1.0f;
        return oneHot;
    }
}

