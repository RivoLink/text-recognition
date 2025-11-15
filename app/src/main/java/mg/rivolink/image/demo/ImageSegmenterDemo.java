package mg.rivolink.image.demo;

import mg.rivolink.image.helper.ImageSegmenter;
import mg.rivolink.image.helper.ImageSegmenter.CharacterBound;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

        displayCharacterDetails(characters);
        displayCharacterSamples(grayscaleImage, width, height, characters);
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

    private static void displayCharacterDetails(List<CharacterBound> characters) {
        System.out.println("Detected character regions:");
        System.out.println();
        System.out.println("  No.    X      Y      Width  Height  Area");
        System.out.println("  ---  -----  -----  ------  ------  ------");

        for (int i = 0; i < characters.size(); i++) {
            CharacterBound bound = characters.get(i);
            int area = bound.getWidth() * bound.getHeight();

            System.out.printf("  %3d  %5d  %5d  %6d  %6d  %6d\n",
                i + 1,
                bound.getX(),
                bound.getY(),
                bound.getWidth(),
                bound.getHeight(),
                area
            );
        }

        System.out.println();
    }

    private static void displayCharacterSamples(
        float[] image, int width, int height, List<CharacterBound> characters
    ) {
        int samplesToShow = Math.min(3, characters.size());

        System.out.println("Character samples (first " + samplesToShow + "):");
        System.out.println();

        for (int i = 0; i < samplesToShow; i++) {
            CharacterBound bound = characters.get(i);
            System.out.println("Character " + (i + 1) + ":");
            System.out.println("  Position: (" + bound.getX() + ", " + bound.getY() + ")");
            System.out.println("  Size: " + bound.getWidth() + " x " + bound.getHeight());
            System.out.println();

            float[] normalized = ImageSegmenter.extractAndNormalize(image, width, height, bound);

            System.out.println("  Normalized to 28x28:");
            displayAsASCII(normalized, 28, 28);
            System.out.println();
        }

        if (characters.size() > samplesToShow) {
            System.out.println("  ... and " + (characters.size() - samplesToShow) + " more");
            System.out.println();
        }
    }

    private static void displayAsASCII(float[] pixels, int width, int height) {
        char[] asciiChars = {' ', '.', ':', '-', '=', '+', '*', '#', '@'};

        for (int y = 0; y < height; y++) {
            System.out.print("  ");
            for (int x = 0; x < width; x++) {
                float value = pixels[y * width + x];
                int index = (int)(value * (asciiChars.length - 1));
                index = Math.max(0, Math.min(asciiChars.length - 1, index));
                System.out.print(asciiChars[index]);
            }
            System.out.println();
        }
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
