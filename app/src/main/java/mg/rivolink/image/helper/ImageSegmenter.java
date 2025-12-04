package mg.rivolink.image.helper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

/**
 * Segments handwritten text images into individual characters
 * optimized for MNIST/EMNIST model input (28x28 pixels)
 */
public class ImageSegmenter {

    private static final int MIN_CHARACTER_WIDTH = 5;
    private static final int MIN_CHARACTER_HEIGHT = 5;

    private static final int SAME_LINE_TOLERANCE = 10;

    private static final int TARGET_SIZE = 28;
    private static final int TARGET_PADDING = 2;
    private static final float FOREGROUND_THRESHOLD = 0.1f;

    // 8-connectivity offsets
    private static final int[][] NEIGHBORS = {
        {-1, -1}, {0, -1}, {1, -1},
        {-1,  0},          {1,  0},
        {-1,  1}, {0,  1}, {1,  1}
    };

    /**
     * Represents a character's bounding box in the source image
     */
    public static class CharacterBound {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        public CharacterBound(int x, int y, int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Width and height must be positive");
            }
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }

        public boolean isValidSize() {
            return width >= MIN_CHARACTER_WIDTH && height >= MIN_CHARACTER_HEIGHT;
        }

        @Override
        public String toString() {
            return String.format("CharBound(x=%d, y=%d, w=%d, h=%d)", x, y, width, height);
        }
    }

    /**
     * Segments an image into individual character bounding boxes (RGB version)
     *
     * @param image BufferedImage to segment
     * @return List of character bounds sorted in reading order (top-to-bottom, left-to-right)
     */
    public static List<CharacterBound> segmentCharactersRGB(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        float[] grayscale = convertToGrayscale(image);
        return segmentCharactersGrayscale(grayscale, image.getWidth(), image.getHeight());
    }

    /**
     * Segments an image into individual character bounding boxes (grayscale version)
     *
     * @param image Flattened grayscale array (row-major order), values in [0, 1]
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @return List of character bounds sorted in reading order (top-to-bottom, left-to-right)
     * @throws IllegalArgumentException if dimensions don't match array size
     */
    public static List<CharacterBound> segmentCharactersGrayscale(float[] image, int width, int height) {
        validateInput(image, width, height);

        boolean[] visited = new boolean[image.length];
        List<CharacterBound> characters = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            int x = 0;
            while (x < width) {
                int index = y * width + x;

                if (visited[index]) {
                    // Skip visited regions faster
                    while (x < width && visited[y * width + x]) {
                        x++;
                    }
                    continue;
                }

                if (isForeground(image[index])) {
                    CharacterBound bound = findConnectedComponent(image, visited, x, y, width, height);

                    if (bound != null && bound.isValidSize()) {
                        characters.add(bound);
                    }

                    // Skip directly to end of detected character
                    x = Math.min(bound.getX() + bound.getWidth(), width);
                } else {
                    x++;
                }
            }
        }

        sortInReadingOrder(characters);
        return characters;
    }

    /**
     * Converts a BufferedImage to normalized grayscale array
     *
     * @param image BufferedImage to convert
     * @return Normalized grayscale array (0.0-1.0, dark text = high value)
     */
    public static float[] convertToGrayscale(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        float[] grayscale = new float[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                float gray = 0.299f * r + 0.587f * g + 0.114f * b;

                // Normalize to 0.0-1.0 and invert (dark text = high value)
                grayscale[y * width + x] = 1.0f - (gray / 255.0f);
            }
        }

        return grayscale;
    }

    /**
     * Converts a normalized grayscale array back to an image and saves it to disk
     * Inverse of convertToGrayscale: values in [0,1] where dark text = high value
     *
     * @param grayscale Flattened grayscale array (row-major order)
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param path Destination file path (extension determines format)
     * @return BufferedImage that was written to disk
     * @throws IllegalArgumentException if inputs are invalid
     * @throws IOException if writing the image fails
     */
    public static BufferedImage grayscaleToImage(float[] grayscale, int width, int height, String path)
        throws IOException {

        validateInput(grayscale, width, height);
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Output path cannot be null or blank");
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float normalized = grayscale[y * width + x];
                float clamped = Math.max(0f, Math.min(1f, normalized));

                // Reverse inversion: grayscale 0 = white background, 1 = black foreground
                int gray255 = Math.round((1.0f - clamped) * 255.0f);
                int rgb = (gray255 << 16) | (gray255 << 8) | gray255;
                image.setRGB(x, y, rgb);
            }
        }

        File outputFile = new File(path);
        String format = extractFormat(path);

        if (!ImageIO.write(image, format, outputFile)) {
            throw new IOException("No appropriate writer found for format: " + format);
        }

        return image;
    }

    /**
     * Extracts and normalizes a character region to MNIST/EMNIST format (28x28)
     *
     * @param image Source image array
     * @param width Source image width
     * @param height Source image height
     * @param bound Character bounding box
     * @return 28x28 normalized pixel array
     */
    public static float[] extractAndNormalize(float[] image, int width, int height, CharacterBound bound) {
        return resizeWithBilinear(image, width, height, bound, TARGET_SIZE);
    }

    private static void validateInput(float[] image, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        if (image.length != width * height) {
            throw new IllegalArgumentException(
                String.format("Image array size (%d) doesn't match dimensions (%dx%d=%d)",
                    image.length, width, height, width * height)
            );
        }
    }

    private static boolean isForeground(float pixelValue) {
        return pixelValue > FOREGROUND_THRESHOLD;
    }

    /**
     * Finds connected component using BFS flood fill
     */
    private static CharacterBound findConnectedComponent(
        float[] image, boolean[] visited, int startX, int startY, int width, int height
    ) {
        Queue<Integer> queue = new LinkedList<>();
        int startIndex = startY * width + startX;
        queue.offer(startIndex);
        visited[startIndex] = true;

        int minX = startX, maxX = startX;
        int minY = startY, maxY = startY;

        while (!queue.isEmpty()) {
            int index = queue.poll();
            int x = index % width;
            int y = index / width;

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);

            // Check 8-connected neighbors
            for (int[] offset : NEIGHBORS) {
                int nx = x + offset[0];
                int ny = y + offset[1];

                if (isValidCoordinate(nx, ny, width, height)) {
                    int neighborIndex = ny * width + nx;

                    if (!visited[neighborIndex] && isForeground(image[neighborIndex])) {
                        visited[neighborIndex] = true;
                        queue.offer(neighborIndex);
                    }
                }
            }
        }

        return new CharacterBound(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isValidCoordinate(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private static String extractFormat(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == path.length() - 1) {
            return "png";
        }
        return path.substring(dotIndex + 1);
    }

    /**
     * Sorts characters in reading order: top-to-bottom, left-to-right
     */
    private static void sortInReadingOrder(List<CharacterBound> characters) {
        characters.sort((a, b) -> {
            // If y-positions differ significantly, sort by row
            if (Math.abs(a.y - b.y) > SAME_LINE_TOLERANCE) {
                return Integer.compare(a.y, b.y);
            }
            // Same row, sort by column
            return Integer.compare(a.x, b.x);
        });
    }

    /**
     * Resizes character region using bilinear interpolation for better quality
     */
    private static float[] resizeWithBilinear(
        float[] image, int imageWidth, int imageHeight, CharacterBound bound, int targetSize
    ) {
        float[] result = new float[targetSize * targetSize];
        int innerWidth = targetSize - 2 * TARGET_PADDING;
        int innerHeight = targetSize - 2 * TARGET_PADDING;

        if (innerWidth <= 0 || innerHeight <= 0) {
            return result;
        }

        float scaleX = (float) bound.width / innerWidth;
        float scaleY = (float) bound.height / innerHeight;

        for (int ty = TARGET_PADDING; ty < TARGET_PADDING + innerHeight; ty++) {
            for (int tx = TARGET_PADDING; tx < TARGET_PADDING + innerWidth; tx++) {
                float sx = bound.x + (tx - TARGET_PADDING) * scaleX;
                float sy = bound.y + (ty - TARGET_PADDING) * scaleY;

                result[ty * targetSize + tx] = bilinearInterpolate(image, imageWidth, imageHeight, sx, sy);
            }
        }

        return result;
    }

    /**
     * Performs bilinear interpolation for smooth resizing
     */
    private static float bilinearInterpolate(float[] image, int width, int height, float x, float y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = Math.min(x0 + 1, width - 1);
        int y1 = Math.min(y0 + 1, height - 1);

        float dx = x - x0;
        float dy = y - y0;

        float val00 = getPixelSafe(image, width, height, x0, y0);
        float val10 = getPixelSafe(image, width, height, x1, y0);
        float val01 = getPixelSafe(image, width, height, x0, y1);
        float val11 = getPixelSafe(image, width, height, x1, y1);

        float top = val00 * (1 - dx) + val10 * dx;
        float bottom = val01 * (1 - dx) + val11 * dx;

        return top * (1 - dy) + bottom * dy;
    }

    private static float getPixelSafe(float[] image, int width, int height, int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 0f;
        }
        int index = y * width + x;
        return (index >= 0 && index < image.length) ? image[index] : 0f;
    }

}
