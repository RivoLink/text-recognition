package mg.rivolink.image.helper;

import java.awt.image.BufferedImage;

/**
 * Provides morphological operations for image preprocessing
 *
 * Implements erosion and dilation operations to improve character segmentation:
 * - Erosion: Shrinks foreground objects, separates touching characters
 * - Dilation: Expands foreground objects, fills small gaps
 * - Opening: Erosion followed by dilation, removes noise
 * - Closing: Dilation followed by erosion, fills small holes
 */
public class ImagePreprocessor {

    /**
     * Structuring element shapes for morphological operations
     */
    public enum StructuringElement {
        SQUARE_3X3,     // 3x3 square kernel
        SQUARE_5X5,     // 5x5 square kernel
        CROSS_3X3,      // 3x3 cross kernel (+ shape)
        HORIZONTAL_3X1, // Horizontal line
        VERTICAL_1X3    // Vertical line
    }

    /**
     * Applies erosion operation to a grayscale image
     * Erosion shrinks foreground objects and separates touching characters
     *
     * @param image Grayscale image array (values 0.0-1.0)
     * @param width Image width
     * @param height Image height
     * @param element Structuring element to use
     * @return Eroded image
     */
    public static float[] erode(float[] image, int width, int height, StructuringElement element) {
        validateInput(image, width, height);

        boolean[][] kernel = getKernel(element);
        int kernelHeight = kernel.length;
        int kernelWidth = kernel[0].length;
        int offsetY = kernelHeight / 2;
        int offsetX = kernelWidth / 2;

        float[] result = new float[image.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float minValue = 1.0f;

                // Apply kernel
                for (int ky = 0; ky < kernelHeight; ky++) {
                    for (int kx = 0; kx < kernelWidth; kx++) {
                        if (!kernel[ky][kx]) continue;

                        int ny = y + ky - offsetY;
                        int nx = x + kx - offsetX;

                        if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
                            int index = ny * width + nx;
                            minValue = Math.min(minValue, image[index]);
                        } else {
                            // Treat outside as background (0)
                            minValue = 0.0f;
                        }
                    }
                }

                result[y * width + x] = minValue;
            }
        }

        return result;
    }

    /**
     * Applies dilation operation to a grayscale image
     * Dilation expands foreground objects and fills small gaps
     *
     * @param image Grayscale image array (values 0.0-1.0)
     * @param width Image width
     * @param height Image height
     * @param element Structuring element to use
     * @return Dilated image
     */
    public static float[] dilate(float[] image, int width, int height, StructuringElement element) {
        validateInput(image, width, height);

        boolean[][] kernel = getKernel(element);
        int kernelHeight = kernel.length;
        int kernelWidth = kernel[0].length;
        int offsetY = kernelHeight / 2;
        int offsetX = kernelWidth / 2;

        float[] result = new float[image.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float maxValue = 0.0f;

                // Apply kernel
                for (int ky = 0; ky < kernelHeight; ky++) {
                    for (int kx = 0; kx < kernelWidth; kx++) {
                        if (!kernel[ky][kx]) continue;

                        int ny = y + ky - offsetY;
                        int nx = x + kx - offsetX;

                        if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
                            int index = ny * width + nx;
                            maxValue = Math.max(maxValue, image[index]);
                        }
                    }
                }

                result[y * width + x] = maxValue;
            }
        }

        return result;
    }

    /**
     * Applies opening operation (erosion followed by dilation)
     * Removes small noise while preserving larger structures
     *
     * @param image Grayscale image array
     * @param width Image width
     * @param height Image height
     * @param element Structuring element
     * @return Opened image
     */
    public static float[] open(float[] image, int width, int height, StructuringElement element) {
        float[] eroded = erode(image, width, height, element);
        return dilate(eroded, width, height, element);
    }

    /**
     * Applies closing operation (dilation followed by erosion)
     * Fills small holes and gaps in foreground objects
     *
     * @param image Grayscale image array
     * @param width Image width
     * @param height Image height
     * @param element Structuring element
     * @return Closed image
     */
    public static float[] close(float[] image, int width, int height, StructuringElement element) {
        float[] dilated = dilate(image, width, height, element);
        return erode(dilated, width, height, element);
    }

    /**
     * Separates touching characters by applying erosion
     * Specifically designed for character segmentation
     *
     * @param image Grayscale image array
     * @param width Image width
     * @param height Image height
     * @param iterations Number of erosion iterations (1-3 recommended)
     * @return Image with separated characters
     */
    public static float[] separateTouchingCharacters(float[] image, int width, int height, int iterations) {
        validateInput(image, width, height);

        float[] result = image.clone();

        // Apply multiple erosion iterations
        for (int i = 0; i < iterations; i++) {
            result = erode(result, width, height, StructuringElement.SQUARE_3X3);
        }

        return result;
    }

    /**
     * Enhances character edges and fills small gaps
     * Useful for broken or thin characters
     *
     * @param image Grayscale image array
     * @param width Image width
     * @param height Image height
     * @return Enhanced image
     */
    public static float[] enhanceCharacters(float[] image, int width, int height) {
        // Apply closing to fill small gaps, then opening to remove noise
        float[] closed = close(image, width, height, StructuringElement.SQUARE_3X3);
        return open(closed, width, height, StructuringElement.SQUARE_3X3);
    }

    /**
     * Applies contrast enhancement using histogram stretching
     *
     * @param image Grayscale image array
     * @return Contrast-enhanced image
     */
    public static float[] enhanceContrast(float[] image) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("Image cannot be null or empty");
        }

        // Find min and max values
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (float pixel : image) {
            min = Math.min(min, pixel);
            max = Math.max(max, pixel);
        }

        // Avoid division by zero
        if (max - min < 0.001f) {
            return image.clone();
        }

        // Stretch histogram
        float[] result = new float[image.length];
        float range = max - min;

        for (int i = 0; i < image.length; i++) {
            result[i] = (image[i] - min) / range;
        }

        return result;
    }

    /**
     * Applies median filter to remove salt-and-pepper noise
     *
     * @param image Grayscale image array
     * @param width Image width
     * @param height Image height
     * @param kernelSize Kernel size (3, 5, or 7)
     * @return Filtered image
     */
    public static float[] medianFilter(float[] image, int width, int height, int kernelSize) {
        validateInput(image, width, height);

        if (kernelSize % 2 == 0 || kernelSize < 3) {
            throw new IllegalArgumentException("Kernel size must be odd and >= 3");
        }

        float[] result = new float[image.length];
        int offset = kernelSize / 2;
        float[] window = new float[kernelSize * kernelSize];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int count = 0;

                // Collect neighborhood values
                for (int ky = 0; ky < kernelSize; ky++) {
                    for (int kx = 0; kx < kernelSize; kx++) {
                        int ny = y + ky - offset;
                        int nx = x + kx - offset;

                        if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
                            window[count++] = image[ny * width + nx];
                        }
                    }
                }

                // Find median
                if (count > 0) {
                    java.util.Arrays.sort(window, 0, count);
                    result[y * width + x] = window[count / 2];
                } else {
                    result[y * width + x] = image[y * width + x];
                }
            }
        }

        return result;
    }

    /**
     * Applies preprocessing pipeline to grayscale image
     *
     * @param image Grayscale image array (values 0.0-1.0)
     * @param width Image width
     * @param height Image height
     * @param separateCharacters Whether to apply character separation
     * @return Preprocessed grayscale array
     */
    public static float[] preprocessImage(float[] image, int width, int height, boolean separateCharacters) {
        validateInput(image, width, height);

        float[] result = image.clone();

        // Enhance contrast
        result = enhanceContrast(result);

        // Remove noise
        result = medianFilter(result, width, height, 3);

        // Optionally separate touching characters
        if (separateCharacters) {
            result = separateTouchingCharacters(result, width, height, 1);
        }

        return result;
    }

    /**
     * Gets the kernel matrix for a structuring element
     */
    private static boolean[][] getKernel(StructuringElement element) {
        switch (element) {
            case SQUARE_3X3:
                return new boolean[][] {
                    {true, true, true},
                    {true, true, true},
                    {true, true, true}
                };

            case SQUARE_5X5:
                return new boolean[][] {
                    {true, true, true, true, true},
                    {true, true, true, true, true},
                    {true, true, true, true, true},
                    {true, true, true, true, true},
                    {true, true, true, true, true}
                };

            case CROSS_3X3:
                return new boolean[][] {
                    {false, true, false},
                    {true,  true, true},
                    {false, true, false}
                };

            case HORIZONTAL_3X1:
                return new boolean[][] {
                    {true, true, true}
                };

            case VERTICAL_1X3:
                return new boolean[][] {
                    {true},
                    {true},
                    {true}
                };

            default:
                return new boolean[][] {
                    {true, true, true},
                    {true, true, true},
                    {true, true, true}
                };
        }
    }

    /**
     * Validates input parameters
     */
    private static void validateInput(float[] image, int width, int height) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        if (image.length != width * height) {
            throw new IllegalArgumentException(
                String.format("Image array size (%d) doesn't match dimensions (%dx%d=%d)",
                    image.length, width, height, width * height
                )
            );
        }
    }

}
