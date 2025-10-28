package mg.rivolink.mnist.text;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * ImageSegmenter - Segments handwritten text image into individual characters
 */
public class ImageSegmenter {

    /**
     * CharacterBound - Represents a single character's bounding box
     */
    public static class CharacterBound {
        public int x;      // Left position
        public int y;      // Top position
        public int width;
        public int height;

        public CharacterBound(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return String.format("CharBound(x=%d, y=%d, w=%d, h=%d)", x, y, width, height);
        }
    }

    /**
     * Detects connected components (individual characters) in a binary image
     * Returns list of bounding boxes for each character
     * 
     * @param image Flattened 1D array of pixels (row-major order)
     * @param imageWidth Width of the image
     * @param imageHeight Height of the image
     */
    public static List<CharacterBound> segmentCharacters(float[] image, int imageWidth, int imageHeight) {
        List<CharacterBound> characters = new ArrayList<>();
        boolean[][] visited = new boolean[imageHeight][imageWidth];

        // Threshold: pixels > 0.1 are considered foreground
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int index = y * imageWidth + x;
                if (!visited[y][x] && image[index] > 0.1f) {
                    CharacterBound bound = floodFill(image, visited, x, y, imageWidth, imageHeight);
                    if (bound != null && bound.width > 5 && bound.height > 5) {
                        characters.add(bound);
                    }
                }
            }
        }

        // Sort characters left to right, top to bottom
        characters.sort((a, b) -> {
            if (Math.abs(a.y - b.y) > 10) {
                return Integer.compare(a.y, b.y);  // Different rows
            }
            return Integer.compare(a.x, b.x);  // Same row, sort by x
        });

        return characters;
    }

    /**
     * Flood fill algorithm to find connected components
     */
    private static CharacterBound floodFill(float[] image, boolean[][] visited,
                                           int startX, int startY, int width, int height) {
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        int minX = startX, maxX = startX;
        int minY = startY, maxY = startY;

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int x = pos[0];
            int y = pos[1];

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);

            // Check 8-connected neighbors
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    int ny = y + dy;

                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && !visited[ny][nx]) {
                        int index = ny * width + nx;
                        if (image[index] > 0.1f) {
                            visited[ny][nx] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }
            }
        }

        return new CharacterBound(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * Extracts and normalizes pixel data for a character bounding box
     * Returns 28x28 normalized image for EMNIST model
     */
    public static float[] extractCharacterPixels(float[] image, int imageWidth, 
                                                CharacterBound bound) {
        return resizeAndNormalize(image, imageWidth, bound, 28, 28);
    }

    /**
     * Resizes character region to 28x28 and normalizes
     */
    private static float[] resizeAndNormalize(float[] image, int imageWidth,
                                             CharacterBound bound, int targetSize, int targetHeight) {
        float[] normalized = new float[targetSize * targetHeight];

        // Simple nearest neighbor resizing
        for (int ty = 0; ty < targetHeight; ty++) {
            for (int tx = 0; tx < targetSize; tx++) {
                int sx = bound.x + (tx * bound.width) / targetSize;
                int sy = bound.y + (ty * bound.height) / targetHeight;

                if (sx >= 0 && sx < imageWidth && sy >= 0) {
                    int index = sy * imageWidth + sx;
                    if (index < image.length) {
                        normalized[ty * targetSize + tx] = image[index];
                    }
                }
            }
        }

        return normalized;
    }
}
