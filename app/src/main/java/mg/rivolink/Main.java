package mg.rivolink;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import mg.rivolink.ai.Network;
import mg.rivolink.image.helper.ImagePreprocessor;
import mg.rivolink.image.helper.ImageSegmenter;
import mg.rivolink.image.helper.ImageSegmenter.CharacterBound;
import mg.rivolink.io.NetworkIO;
import mg.rivolink.lang.helper.LanguageModel;
import mg.rivolink.lang.helper.WeightedLanguageModel;
import mg.rivolink.mnist.data.MNISTDataset;
import mg.rivolink.mnist.helper.MNISTPredictor;

public final class Main {

    private static final int ASCII_SIZE = 5;
    private static final String MODEL_DIR = "data/models/";

    private Main() {
        // utility class
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        MNISTDataset.Type datasetType = resolveDatasetType(args);
        String imagePath = resolveImagePath(args);

        File imageFile = new File(imagePath);
        validateImageFile(imageFile, imagePath);

        String modelPath = buildModelPath(datasetType);
        ensureModelExists(modelPath, datasetType);

        System.out.println("=== Text Recognition Demo ===");
        System.out.println();

        System.out.println("Dataset : " + datasetType.description);
        System.out.println("Model   : " + modelPath);
        System.out.println("Image   : " + imagePath);
        System.out.println();

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            System.err.println("Error: Failed to read image file");
            System.exit(1);
        }

        int width = image.getWidth();
        int height = image.getHeight();

        System.out.println("Converting image to grayscale...");
        long startNs = System.nanoTime();
        float[] grayscale = ImageSegmenter.convertToGrayscale(image);
        System.out.println("Conversion complete in " + elapsedMs(startNs) + " ms.");
        System.out.println();

        System.out.println("Pre-processing image...");
        startNs = System.nanoTime();
        grayscale = ImagePreprocessor.preprocessImage(grayscale, width, height, false);
        System.out.println("Pre-processing complete in " + elapsedMs(startNs) + " ms.");
        System.out.println();

        System.out.println("Segmenting characters...");
        startNs = System.nanoTime();
        List<CharacterBound> characters = ImageSegmenter.segmentCharactersGrayscale(grayscale, width, height);
        long segmentationMs = elapsedMs(startNs);
        if (characters.isEmpty()) {
            System.out.println("No characters detected. Check image quality or contrast.");
            return;
        }
        System.out.println("Found " + characters.size() + " character(s) in " + segmentationMs + " ms.");
        System.out.println();

        Network network = NetworkIO.load(modelPath);
        MNISTPredictor predictor = new MNISTPredictor(network, datasetType);

        StringBuilder recognized = new StringBuilder();
        List<float[]> asciiCharacters = new ArrayList<>(characters.size());

        startNs = System.nanoTime();
        for (CharacterBound bound : characters) {
            float[] normalized = ImageSegmenter.extractAndNormalize(grayscale, width, height, bound);
            MNISTPredictor.PredictionResult prediction = predictor.predictWithConfidence(normalized);

            recognized.append(prediction.prediction);
            asciiCharacters.add(extractForAscii(grayscale, width, height, bound));
        }
        System.out.println("Character recognition completed in " + elapsedMs(startNs) + " ms.");

        printAsciiCharacters(asciiCharacters);

        LanguageModel languageModel = new WeightedLanguageModel();
        System.out.println();

        String predictedText = recognized.toString();
        String suggestedText = suggestText(languageModel, predictedText);

        System.out.println("Predicted text : " + predictedText);
        System.out.println("Suggested text : " + suggestedText);
    }

    private static MNISTDataset.Type resolveDatasetType(String[] args) {
        if (args != null && args.length > 1) {
            String requested = args[0].trim().toLowerCase(Locale.ROOT);
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

    private static String resolveImagePath(String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("Usage: Main [dataset: mnist|emnist] <image-path>");
            System.exit(1);
        }
        if (args.length == 1) {
            return args[0];
        }
        return args[1];
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

    private static String buildModelPath(MNISTDataset.Type datasetType) {
        String modelFile = datasetType.name().toLowerCase(Locale.ROOT) + "-model.bin";
        return MODEL_DIR + modelFile;
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static void ensureModelExists(String modelPath, MNISTDataset.Type datasetType) {
        if (new File(modelPath).exists()) {
            return;
        }
        String datasetKey = datasetType.name().toLowerCase(Locale.ROOT);
        System.err.println("Model not found: " + modelPath);
        System.err.println("Train the model with:");
        System.err.println("  ./gradlew trainModel -Pdataset=" + datasetKey);
        System.exit(1);
    }

    private static float[] extractForAscii(
        float[] image, int width, int height, CharacterBound bound
    ) {
        float[] normalized = ImageSegmenter.extractAndNormalize(image, width, height, bound);
        return downsample(normalized, 28, 28, ASCII_SIZE);
    }

    private static float[] downsample(float[] image, int width, int height, int targetSize) {
        float[] result = new float[targetSize * targetSize];

        for (int ty = 0; ty < targetSize; ty++) {
            int yStart = (int)((long)ty * height / targetSize);
            int yEnd = (int)((long)(ty + 1) * height / targetSize);
            yEnd = Math.max(yEnd, yStart + 1);

            for (int tx = 0; tx < targetSize; tx++) {
                int xStart = (int)((long)tx * width / targetSize);
                int xEnd = (int)((long)(tx + 1) * width / targetSize);
                xEnd = Math.max(xEnd, xStart + 1);

                float sum = 0f;
                int count = 0;
                for (int y = yStart; y < yEnd; y++) {
                    for (int x = xStart; x < xEnd; x++) {
                        int index = y * width + x;
                        sum += image[index];
                        count++;
                    }
                }
                result[ty * targetSize + tx] = sum / count;
            }
        }

        return result;
    }

    private static void printAsciiCharacters(List<float[]> characters) {
        if (characters.isEmpty()) {
            return;
        }

        System.out.println("Segmented characters (5x5):");
        System.out.println();

        for (int row = 0; row < ASCII_SIZE; row++) {
            for (float[] character : characters) {
                for (int col = 0; col < ASCII_SIZE; col++) {
                    float value = character[row * ASCII_SIZE + col];
                    System.out.print(asciiChar(value));
                }
                System.out.print("  ");
            }
            System.out.println();
        }

        System.out.println();
        for (int i = 0; i < characters.size(); i++) {
            System.out.printf("%-7s", "C" + (i + 1));
        }
        System.out.println();
        System.out.println();
    }

    private static char asciiChar(float value) {
        char[] asciiChars = {' ', '.', ':', '-', '=', '+', '*', '#', '@'};
        int index = (int)(value * (asciiChars.length - 1));
        index = Math.max(0, Math.min(asciiChars.length - 1, index));
        return asciiChars[index];
    }

    private static String suggestText(LanguageModel languageModel, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (!text.contains(" ")) {
            return languageModel.suggestCorrection(text);
        }

        String[] tokens = text.split("\\s+");
        StringBuilder corrected = new StringBuilder();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            String clean = token.replaceAll("[^a-zA-Z]", "");

            String suggestion = clean.isEmpty() ? token : languageModel.suggestCorrection(clean);

            if (i > 0) {
                corrected.append(' ');
            }
            corrected.append(suggestion);
        }

        return corrected.toString();
    }

}
