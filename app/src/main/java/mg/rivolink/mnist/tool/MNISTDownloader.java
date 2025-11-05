package mg.rivolink.mnist.tool;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import mg.rivolink.mnist.data.MNISTDataset;

/**
 * Downloads MNIST and EMNIST dataset
 * MNIST - Digits only (0-9)
 * EMNIST - Digits and Letters (0-9, A-Z, a-z)
 */
public class MNISTDownloader {

    private final String downloadDir;
    private final MNISTDataset.Type datasetType;
    private volatile boolean downloadCancelled = false;

    public MNISTDownloader(String downloadDir, MNISTDataset.Type datasetType) {
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
            
            System.out.println("Completed");
            System.out.println();
        }

        System.out.println("All files downloaded successfully!");
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
            if (new File(unzipFilePath).exists()) {
                System.out.println("  File already exists: " + unzipFilePath);
                return true;
            }

            if (!downloadFile(url, gzFilePath)) {
                return false;
            }

            System.out.println("  Extracting...");
            if (!extractGzip(gzFilePath, unzipFilePath)) {
                return false;
            }

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
            URL url = URI.create(urlString).toURL();
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

                if (fileSize > 0) {
                    printProgress(totalBytesRead, fileSize);
                    System.out.println();
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
                System.out.println("[OK] " + unzipFileName + " (" + formatBytes(file.length()) + ")");
            } else {
                System.out.println("[MISSING] " + unzipFileName);
                allExist = false;
            }
        }

        System.out.println();
        if (allExist) {
            System.out.println("All dataset files are present!");
        } else {
            System.out.println("Some files are missing. Please download them.");
        }
        return allExist;
    }

}
