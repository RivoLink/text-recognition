package mg.rivolink.mnist.task;

import mg.rivolink.mnist.data.MNISTDataset;
import mg.rivolink.mnist.tool.MNISTDownloader;
import mg.rivolink.mnist.tool.MNISTDownloader.DatasetPaths;

public final class MNISTDownloaderTask {

    private static final String MNIST_DATA_DIR = "data/mnist";
    private static final String EMNIST_DATA_DIR = "data/emnist";

    private MNISTDownloaderTask() {
        // utility class
    }

    public static void main(String[] args) {
        System.out.println("=== Dataset Downloader ===");
        System.out.println();

        MNISTDataset.Type datasetType = resolveDatasetType(args);
        final String downloadDir = datasetType == MNISTDataset.Type.EMNIST ? EMNIST_DATA_DIR : MNIST_DATA_DIR;
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

    private static MNISTDataset.Type resolveDatasetType(String[] args) {
        if (args != null && args.length > 0) {
            String requested = args[0].trim().toLowerCase();
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

}
