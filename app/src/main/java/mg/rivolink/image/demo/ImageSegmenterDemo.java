package mg.rivolink.image.demo;

import mg.rivolink.image.helper.ImageSegmenter;
import mg.rivolink.image.helper.ImageSegmenter.CharacterBound;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ImageSegmenterDemo {

    private ImageSegmenterDemo() {
        // utility class
    }

    public static void main(String[] args) throws IOException {
        String imagePath = resolveImagePath(args);

        System.out.println("=== Image Segmentation Demo ===");
        System.out.println();

        File imageFile = new File(imagePath);
        validateImageFile(imageFile, imagePath);

        System.out.println("Loading image: " + imagePath);
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            System.err.println("Error: Failed to read image file");
            System.exit(1);
        }

        int width = image.getWidth();
        int height = image.getHeight();
        System.out.println("Image dimensions: " + width + " x " + height);
        System.out.println();

        System.out.println("Segmenting characters...");
        long startTime = System.currentTimeMillis();
        List<CharacterBound> characters = ImageSegmenter.segmentCharactersRGB(image);
        long endTime = System.currentTimeMillis();

        System.out.println("Segmentation complete in " + (endTime - startTime) + " ms");
        System.out.println("Detected " + characters.size() + " character(s)");
        System.out.println();

        if (characters.isEmpty()) {
            System.out.println("No characters detected");
            System.out.println("Try checking image quality or contrast");
            return;
        }

        // Convert to grayscale for display purposes
        float[] grayscaleImage = ImageSegmenter.convertToGrayscale(image);

        displayDetails(characters);
        displayCharacters(grayscaleImage, width, height, characters);
        displayStatistics(characters, width, height);
    }

    private static String resolveImagePath(String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("Usage: ImageSegmenterDemo <image-path>");
            System.exit(1);
        }
        return args[0];
    }

    private static void validateImageFile(File file, String path) {
        if (!file.exists()) {
            System.err.println("Error: Image file not found: " + path);
            System.exit(1);
        }

        if (!isValidImageFormat(path)) {
            System.err.println("Error: Unsupported image format. Please use PNG or JPG");
            System.exit(1);
        }
    }

    private static boolean isValidImageFormat(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static void displayDetails(List<CharacterBound> characters) {
        if (characters.isEmpty()) {
            return;
        }

        System.out.println("Character details table:");
        System.out.println();

        int maxChars = characters.size();
        int colsPerRow = 5;

        // Pre-calculate max widths for better alignment
        int maxPosWidth = 0;
        int maxSizeWidth = 0;

        for (CharacterBound bound : characters) {
            String pos = "(" + bound.getX() + "," + bound.getY() + ")";
            String size = bound.getWidth() + "x" + bound.getHeight();
            maxPosWidth = Math.max(maxPosWidth, pos.length());
            maxSizeWidth = Math.max(maxSizeWidth, size.length());
        }

        int colWidth = Math.max(maxPosWidth, Math.max(maxSizeWidth, 6)) + 2;
        String rowLabel = "%-5s ";

        for (int startIdx = 0; startIdx < maxChars; startIdx += colsPerRow) {
            int endIdx = Math.min(startIdx + colsPerRow, maxChars);

            // Header row (No.)
            System.out.printf(rowLabel, "No.");
            for (int i = startIdx; i < endIdx; i++) {
                System.out.printf("%-" + colWidth + "s", String.valueOf(i + 1));
            }
            System.out.println();

            // Separator
            System.out.printf(rowLabel, "-----");
            for (int i = startIdx; i < endIdx; i++) {
                // Print dashes for column width - 1
                for (int j = 0; j < colWidth - 1; j++) {
                    System.out.print("-");
                }
                System.out.print(" ");
            }
            System.out.println();

            // Position (x,y)
            System.out.printf(rowLabel, "Pos");
            for (int i = startIdx; i < endIdx; i++) {
                CharacterBound bound = characters.get(i);
                String pos = "(" + bound.getX() + "," + bound.getY() + ")";
                System.out.printf("%-" + colWidth + "s", pos);
            }
            System.out.println();

            // Size WxH
            System.out.printf(rowLabel, "Size");
            for (int i = startIdx; i < endIdx; i++) {
                CharacterBound bound = characters.get(i);
                String size = bound.getWidth() + "x" + bound.getHeight();
                System.out.printf("%-" + colWidth + "s", size);
            }
            System.out.println();

            // Area
            System.out.printf(rowLabel, "Area");
            for (int i = startIdx; i < endIdx; i++) {
                CharacterBound bound = characters.get(i);
                int area = bound.getWidth() * bound.getHeight();
                System.out.printf("%-" + colWidth + "d", area);
            }
            System.out.println();
            System.out.println();
        }
    }

    private static void displayCharacters(
        float[] image, int width, int height, List<CharacterBound> characters
    ) {
        if (characters.isEmpty()) {
            return;
        }

        final int DISPLAY_SIZE = 10;
        System.out.println("All characters (10x10 each):");
        System.out.println();

        // Extract and normalize all characters to 10x10
        List<float[]> normalizedChars = new ArrayList<>();
        for (CharacterBound bound : characters) {
            float[] normalized = resizeCharacter(image, width, height, bound, DISPLAY_SIZE);
            normalizedChars.add(normalized);
        }

        // Display horizontally, row by row
        for (int row = 0; row < DISPLAY_SIZE; row++) {
            for (int i = 0; i < normalizedChars.size(); i++) {
                float[] charPixels = normalizedChars.get(i);

                // Print one row of this character
                for (int col = 0; col < DISPLAY_SIZE; col++) {
                    float value = charPixels[row * DISPLAY_SIZE + col];
                    System.out.print(getASCIIChar(value));
                }

                // Add separator between characters
                System.out.print("  ");
            }
            System.out.println();
        }

        // Print character numbers below
        System.out.println();
        for (int i = 0; i < normalizedChars.size(); i++) {
            System.out.printf("%-12s", "Char " + (i + 1));
        }
        System.out.println();
        System.out.println();
    }

    private static float[] resizeCharacter(
        float[] image, int imageWidth, int imageHeight, CharacterBound bound, int targetSize
    ) {
        float[] result = new float[targetSize * targetSize];
        float scaleX = (float) bound.getWidth() / targetSize;
        float scaleY = (float) bound.getHeight() / targetSize;

        for (int ty = 0; ty < targetSize; ty++) {
            for (int tx = 0; tx < targetSize; tx++) {
                int sx = bound.getX() + (int)(tx * scaleX);
                int sy = bound.getY() + (int)(ty * scaleY);

                if (sx >= 0 && sx < imageWidth && sy >= 0 && sy < imageHeight) {
                    int index = sy * imageWidth + sx;
                    if (index >= 0 && index < image.length) {
                        result[ty * targetSize + tx] = image[index];
                    }
                }
            }
        }

        return result;
    }

    private static char getASCIIChar(float value) {
        char[] asciiChars = {' ', '.', ':', '-', '=', '+', '*', '#', '@'};
        int index = (int)(value * (asciiChars.length - 1));
        index = Math.max(0, Math.min(asciiChars.length - 1, index));
        return asciiChars[index];
    }

    private static void displayStatistics(
        List<CharacterBound> characters, int imageWidth, int imageHeight
    ) {
        if (characters.isEmpty()) {
            return;
        }

        int minWidth = Integer.MAX_VALUE;
        int maxWidth = Integer.MIN_VALUE;
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        double avgWidth = 0;
        double avgHeight = 0;
        int totalArea = 0;

        for (CharacterBound bound : characters) {
            int w = bound.getWidth();
            int h = bound.getHeight();
            int area = w * h;

            minWidth = Math.min(minWidth, w);
            maxWidth = Math.max(maxWidth, w);
            minHeight = Math.min(minHeight, h);
            maxHeight = Math.max(maxHeight, h);
            avgWidth += w;
            avgHeight += h;
            totalArea += area;
        }

        avgWidth /= characters.size();
        avgHeight /= characters.size();

        System.out.println("Statistics:");
        System.out.println();
        System.out.printf("  Width:  min=%d, max=%d, avg=%.1f\n", minWidth, maxWidth, avgWidth);
        System.out.printf("  Height: min=%d, max=%d, avg=%.1f\n", minHeight, maxHeight, avgHeight);
        System.out.println();
        System.out.printf("  Total character area: %d pixels\n", totalArea);
        System.out.printf("  Image area: %d pixels\n", imageWidth * imageHeight);
        System.out.printf("  Coverage: %.2f%%\n", (totalArea * 100.0) / (imageWidth * imageHeight));
        System.out.println();

        if (maxWidth > imageWidth * 0.8) {
            System.out.println("Warning: Some characters are very wide (>80% of image)");
        }
        if (minWidth < 10 || minHeight < 10) {
            System.out.println("Warning: Some characters are very small (might be noise)");
        }
    }

}
