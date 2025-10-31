package mg.rivolink.mnist.task;

import mg.rivolink.mnist.data.MNISTDownloader;
import mg.rivolink.mnist.data.MNISTDownloader.DatasetPaths;
import mg.rivolink.mnist.data.MNISTDownloader.DatasetType;

public final class MNISTDownloaderTask {

    private static final String MNIST_DATA_DIR = "data/mnist";
    private static final String EMNIST_DATA_DIR = "data/emnist";

    private MNISTDownloaderTask() {
        // utility class
    }

    public static void main(String[] args) {
        System.out.println("=== Dataset Downloader ===");
        System.out.println();

        DatasetType datasetType = resolveDatasetType(args);
        final String downloadDir = datasetType == DatasetType.EMNIST ? EMNIST_DATA_DIR : MNIST_DATA_DIR;
        final MNISTDownloader downloader = new MNISTDownloader(downloadDir, datasetType);

        if (downloader.downloadAll()) {
            System.out.println();
            downloader.verifyDataset();
            System.out.println();

            final DatasetPaths paths = downloader.getDatasetPaths();
            System.out.println(paths);
            System.out.println();

            System.out.println("Ready to train! Use these paths:");
            System.out.println("  trainImagesPath: " + paths.trainImagesPath);
            System.out.println("  trainLabelsPath: " + paths.trainLabelsPath);
            System.out.println("  testImagesPath: " + paths.testImagesPath);
            System.out.println("  testLabelsPath: " + paths.testLabelsPath);
        } else {
            System.out.println();
            System.err.println("Download failed. Please check your internet connection.");
            System.exit(1);
        }
    }

    private static DatasetType resolveDatasetType(String[] args) {
        if (args != null && args.length > 0) {
            String requested = args[0].trim().toLowerCase();
            if ("mnist".equals(requested)) {
                return DatasetType.MNIST;
            }
            if ("emnist".equals(requested)) {
                return DatasetType.EMNIST;
            }
            System.out.println("Unknown dataset '" + args[0] + "'. Falling back to EMNIST.");
        }

        return DatasetType.EMNIST;
    }

}
