package mg.rivolink.mnist.data;

import java.io.File;

public final class MNISTDataset {

    private MNISTDataset() {
        // utility container
    }

    public enum Type {
        MNIST(
            10,
            "MNIST - Digits only (0-9)",
            "https://github.com/rivolink/mnist/raw/master/",
            new String[] {
                "train-images-idx3-ubyte.gz",
                "train-labels-idx1-ubyte.gz",
                "t10k-images-idx3-ubyte.gz",
                "t10k-labels-idx1-ubyte.gz"
            }
        ),
        EMNIST(
            62,
            "EMNIST - Digits and Letters (0-9, A-Z, a-z)",
            "https://github.com/rivolink/emnist/raw/master/gzip/",
            new String[] {
                "emnist-byclass-train-images-idx3-ubyte.gz",
                "emnist-byclass-train-labels-idx1-ubyte.gz",
                "emnist-byclass-test-images-idx3-ubyte.gz",
                "emnist-byclass-test-labels-idx1-ubyte.gz"
            }
        );

        public final int numClasses;
        public final String description;
        public final String baseUrl;
        public final String[] files;

        Type(int numClasses, String description, String baseUrl, String[] files) {
            this.numClasses = numClasses;
            this.description = description;
            this.baseUrl = baseUrl;
            this.files = files;
        }
    }

    public static class Paths {

        public String trainImagesPath;
        public String trainLabelsPath;
        public String testImagesPath;
        public String testLabelsPath;
        public Type datasetType;

        public Paths(String trainImagesPath, String trainLabelsPath,
                     String testImagesPath, String testLabelsPath, Type datasetType) {
            this.trainImagesPath = trainImagesPath;
            this.trainLabelsPath = trainLabelsPath;
            this.testImagesPath = testImagesPath;
            this.testLabelsPath = testLabelsPath;
            this.datasetType = datasetType;
        }

        /**
         * Get label description for EMNIST
         */
        public String getLabelDescription() {
            if (datasetType == Type.EMNIST) {
                return "EMNIST Labels:\n" +
                       "  0-9: Digits\n" +
                       "  10-35: Uppercase letters (A-Z)\n" +
                       "  36-61: Lowercase letters (a-z)";
            } else {
                return "MNIST Labels:\n" +
                       "  0-9: Digits";
            }
        }

        @Override
        public String toString() {
            return "Paths (" + datasetType.description + ")\n" +
                   "{\n" +
                   "  trainImages: " + trainImagesPath + "\n" +
                   "  trainLabels: " + trainLabelsPath + "\n" +
                   "  testImages: " + testImagesPath + "\n" +
                   "  testLabels: " + testLabelsPath + "\n" +
                   "}\n" +
                   getLabelDescription();
        }
    }

    public static Paths resolvePaths(String baseDir, Type datasetType) {
        String[] resolvedPaths = new String[datasetType.files.length];
        for (int i = 0; i < datasetType.files.length; i++) {
            String fileName = datasetType.files[i];
            String unzippedName = fileName.endsWith(".gz")
                ? fileName.substring(0, fileName.length() - 3)
                : fileName;
            resolvedPaths[i] = baseDir + File.separator + unzippedName;
        }

        if (resolvedPaths.length < 4) {
            throw new IllegalStateException("Dataset " + datasetType + " must declare at least 4 files.");
        }

        return new Paths(
            resolvedPaths[0],
            resolvedPaths[1],
            resolvedPaths[2],
            resolvedPaths[3],
            datasetType
        );
    }

}
