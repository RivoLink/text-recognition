package mg.rivolink.mnist.tool;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Loads MNIST or EMNIST dataset
 */
public class MNISTLoader {

    private static class MNISTImages {
        private final byte[][] pixels;
        private final int pixelCount;

        // Image i has pixels[i], rows*cols grayscale pixels
        private MNISTImages(byte[][] pixels, int pixelCount) {
            this.pixels = pixels;
            this.pixelCount = pixelCount;
        }
    }

    public static class MNISTData {

        private final byte[][] pixels;
        private final int[] labels;
        private final int pixelCount;

        public MNISTData(byte[][] pixels, int[] labels, int pixelCount) {
            this.pixels = pixels;
            this.labels = labels;
            this.pixelCount = pixelCount;
        }

        public int size() {
            return labels.length;
        }

        public int getLabel(int index) {
            return labels[index];
        }

        public int[] getLabels() {
            return labels;
        }

        public int getPixelCount() {
            return pixelCount;
        }

        public float[] getImageAsFloat(int index, float[] buffer) {
            if (buffer == null || buffer.length != pixelCount) {
                buffer = new float[pixelCount];
            }

            byte[] imageBytes = pixels[index];
            for (int i = 0; i < pixelCount; i++) {
                buffer[i] = (imageBytes[i] & 0xFF) / 255.0f;
            }
            return buffer;
        }

        public byte[] getImageBytes(int index) {
            return pixels[index];
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
        MNISTImages imagesData = readImages(imagePath, labels.length);
        return new MNISTData(imagesData.pixels, labels, imagesData.pixelCount);
    }

    @SuppressWarnings("unused")
    private static int[] readLabels(String filePath) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {
            int magicNumber = dis.readInt();
            int numLabels = dis.readInt();

            int[] labels = new int[numLabels];
            for (int i = 0; i < numLabels; i++) {
                labels[i] = dis.readUnsignedByte();
            }
            return labels;
        }
    }

    @SuppressWarnings("unused")
    private static MNISTImages readImages(String filePath, int numImagesRequested) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {
            int magicNumber = dis.readInt();
            int numImagesInFile = dis.readInt();
            int rows = dis.readInt();
            int cols = dis.readInt();

            int pixelCount = rows * cols;
            int numImages = Math.min(numImagesRequested, numImagesInFile);

            byte[][] pixels = new byte[numImages][pixelCount];
            for (int i = 0; i < numImages; i++) {
                byte[] pixel = pixels[i];
                for (int j = 0; j < pixelCount; j++) {
                    pixel[j] = (byte) dis.readUnsignedByte();
                }
            }
            return new MNISTImages(pixels, pixelCount);
        }
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
