package mg.rivolink.mnist.data;

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

}
