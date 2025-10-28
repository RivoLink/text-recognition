package mg.rivolink.mnist.data;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * MNISTDownloader - Downloads EMNIST (Extended MNIST) dataset
 * EMNIST includes digits (0-9) and letters (A-Z, a-z)
 */
public class MNISTDownloader {

    private static final String MNIST_BASE_URL = "http://yann.lecun.com/exdb/mnist/";
    private static final String EMNIST_BASE_URL = "https://github.com/aurelienduarte/emnist/raw/refs/heads/master/gzip/";
    
    private static final String[] MNIST_FILES = {
        "train-images-idx3-ubyte.gz",
        "train-labels-idx1-ubyte.gz",
        "t10k-images-idx3-ubyte.gz",
        "t10k-labels-idx1-ubyte.gz"
    };

    private static final String[] EMNIST_FILES = {
        "emnist-byclass-train-images-idx3-ubyte.gz",
        "emnist-byclass-train-labels-idx1-ubyte.gz",
        "emnist-byclass-test-images-idx3-ubyte.gz",
        "emnist-byclass-test-labels-idx1-ubyte.gz"
    };

    public enum DatasetType {
        MNIST("MNIST - Digits only (0-9)", MNIST_BASE_URL, MNIST_FILES),
        EMNIST("EMNIST - Digits and Letters (0-9, A-Z, a-z)", EMNIST_BASE_URL, EMNIST_FILES);

        public final String description;
        public final String baseUrl;
        public final String[] files;

        DatasetType(String description, String baseUrl, String[] files) {
            this.description = description;
            this.baseUrl = baseUrl;
            this.files = files;
        }
    }

    private final String downloadDir;
    private final DatasetType datasetType;
    private volatile boolean downloadCancelled = false;

    public MNISTDownloader(String downloadDir, DatasetType datasetType) {
        this.downloadDir = downloadDir;
        this.datasetType = datasetType;
        new File(downloadDir).mkdirs();
    }

    /**
     * Downloads all dataset files based on type
     */
    public boolean downloadAll() {
        System.out.println("=== " + datasetType.description + " ===");
        System.out.println("Starting dataset download to: " + downloadDir);
        System.out.println("Files needed: " + datasetType.files.length + " (2 training, 2 test)");
        System.out.println();

        for (int i = 0; i < datasetType.files.length; i++) {
            String fileName = datasetType.files[i];
            System.out.println("[" + (i + 1) + "/" + datasetType.files.length + "] Downloading " + fileName + "...");
            
            if (!download(fileName)) {
                System.err.println("Failed to download: " + fileName);
                return false;
            }
            
            System.out.println("✓ Completed");
            System.out.println();
        }

        System.out.println("✓ All files downloaded successfully!");
        return true;
    }

    /**
     * Downloads a single dataset file
     */
    private boolean download(String fileName) {
        String url = datasetType.baseUrl + fileName;
        String gzFilePath = downloadDir + File.separator + fileName;
        String unzipFilePath = gzFilePath.substring(0, gzFilePath.length() - 3);

        try {
            // Check if file already exists
            if (new File(unzipFilePath).exists()) {
                System.out.println("  File already exists: " + unzipFilePath);
                return true;
            }

            // Download gzip file
            if (!downloadFile(url, gzFilePath)) {
                return false;
            }

            // Extract gzip file
            System.out.println("  Extracting...");
            if (!extractGzip(gzFilePath, unzipFilePath)) {
                return false;
            }

            // Delete gzip file after extraction
            new File(gzFilePath).delete();
            return true;

        } catch (Exception e) {
            System.err.println("  Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Downloads a file from URL with progress tracking
     */
    private boolean downloadFile(String urlString, String filePath) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int fileSize = connection.getContentLength();
            System.out.println("  File size: " + formatBytes(fileSize));

            try (InputStream is = connection.getInputStream();
                 FileOutputStream fos = new FileOutputStream(filePath)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                long lastProgressTime = System.currentTimeMillis();

                while ((bytesRead = is.read(buffer)) != -1) {
                    if (downloadCancelled) {
                        System.out.println("  Download cancelled");
                        new File(filePath).delete();
                        return false;
                    }

                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastProgressTime >= 1000) {
                        printProgress(totalBytesRead, fileSize);
                        lastProgressTime = currentTime;
                    }
                }

                System.out.println("  Downloaded: " + formatBytes(totalBytesRead));
                return true;

            }
        } catch (Exception e) {
            System.err.println("  Download failed: " + e.getMessage());
            new File(filePath).delete();
            return false;
        }
    }

    /**
     * Extracts gzip compressed file
     */
    private boolean extractGzip(String gzFilePath, String outputFilePath) {
        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzFilePath));
             FileOutputStream fos = new FileOutputStream(outputFilePath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            System.out.println("  Extraction complete: " + outputFilePath);
            return true;

        } catch (Exception e) {
            System.err.println("  Extraction failed: " + e.getMessage());
            new File(outputFilePath).delete();
            return false;
        }
    }

    /**
     * Prints download progress
     */
    private void printProgress(long downloadedBytes, int totalBytes) {
        if (totalBytes <= 0) return;

        int percentage = (int) ((downloadedBytes * 100) / totalBytes);
        int barLength = 50;
        int filledLength = (percentage * barLength) / 100;

        StringBuilder bar = new StringBuilder("  [");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filledLength ? "=" : " ");
        }
        bar.append("] ");
        bar.append(String.format("%3d%%", percentage));
        bar.append(" (").append(formatBytes(downloadedBytes)).append("/").append(formatBytes(totalBytes)).append(")");

        System.out.print("\r" + bar);
    }

    /**
     * Formats bytes to human readable format
     */
    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Cancels the current download
     */
    public void cancelDownload() {
        downloadCancelled = true;
    }

    /**
     * Verifies that all required files exist
     */
    public boolean verifyDataset() {
        System.out.println("Verifying dataset files for: " + datasetType.description);
        boolean allExist = true;

        for (String fileName : datasetType.files) {
            String unzipFileName = fileName.substring(0, fileName.length() - 3);
            String filePath = downloadDir + File.separator + unzipFileName;
            File file = new File(filePath);

            if (file.exists()) {
                System.out.println("✓ " + unzipFileName + " (" + formatBytes(file.length()) + ")");
            } else {
                System.out.println("✗ " + unzipFileName + " (MISSING)");
                allExist = false;
            }
        }

        System.out.println();
        if (allExist) {
            System.out.println("✓ All dataset files are present!");
        } else {
            System.out.println("✗ Some files are missing. Please download them.");
        }
        return allExist;
    }

    /**
     * Gets the file paths for downloaded dataset
     */
    public DatasetPaths getDatasetPaths() {
        String trainImages, trainLabels, testImages, testLabels;

        if (datasetType == DatasetType.EMNIST) {
            trainImages = downloadDir + File.separator + "emnist-byclass-train-images-idx3-ubyte";
            trainLabels = downloadDir + File.separator + "emnist-byclass-train-labels-idx1-ubyte";
            testImages = downloadDir + File.separator + "emnist-byclass-test-images-idx3-ubyte";
            testLabels = downloadDir + File.separator + "emnist-byclass-test-labels-idx1-ubyte";
        } else {
            trainImages = downloadDir + File.separator + "train-images-idx3-ubyte";
            trainLabels = downloadDir + File.separator + "train-labels-idx1-ubyte";
            testImages = downloadDir + File.separator + "t10k-images-idx3-ubyte";
            testLabels = downloadDir + File.separator + "t10k-labels-idx1-ubyte";
        }

        return new DatasetPaths(trainImages, trainLabels, testImages, testLabels, datasetType);
    }

    /**
     * Container class for dataset file paths
     */
    public static class DatasetPaths {
        public String trainImagesPath;
        public String trainLabelsPath;
        public String testImagesPath;
        public String testLabelsPath;
        public DatasetType datasetType;

        public DatasetPaths(String trainImagesPath, String trainLabelsPath,
                           String testImagesPath, String testLabelsPath, DatasetType datasetType) {
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
            if (datasetType == DatasetType.EMNIST) {
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
            return "DatasetPaths (" + datasetType.description + ")\n" +
                   "{\n" +
                   "  trainImages: " + trainImagesPath + "\n" +
                   "  trainLabels: " + trainLabelsPath + "\n" +
                   "  testImages: " + testImagesPath + "\n" +
                   "  testLabels: " + testLabelsPath + "\n" +
                   "}\n" +
                   getLabelDescription();
        }
    }

    /**
     * Example usage
     */
    public static void main(String[] args) {
        System.out.println("=== Dataset Downloader ===\n");
        
        // Choose dataset type
        DatasetType selectedDataset = DatasetType.EMNIST; // Change to MNIST if needed
        
        String downloadDir = selectedDataset == DatasetType.EMNIST ? "data/emnist_data" : "data/mnist_data";
        MNISTDownloader downloader = new MNISTDownloader(downloadDir, selectedDataset);

        // Download all files
        if (downloader.downloadAll()) {
            System.out.println();
            
            // Verify the dataset
            downloader.verifyDataset();
            System.out.println();
            
            // Print dataset paths
            DatasetPaths paths = downloader.getDatasetPaths();
            System.out.println(paths);
            System.out.println();
            
            // Now you can use the paths with MNISTExample
            System.out.println("Ready to train! Use these paths:");
            System.out.println("  trainImagesPath: " + paths.trainImagesPath);
            System.out.println("  trainLabelsPath: " + paths.trainLabelsPath);
            System.out.println("  testImagesPath: " + paths.testImagesPath);
            System.out.println("  testLabelsPath: " + paths.testLabelsPath);
        } else {
            System.err.println("\n✗ Download failed. Please check your internet connection.");
            System.exit(1);
        }
    }
}
