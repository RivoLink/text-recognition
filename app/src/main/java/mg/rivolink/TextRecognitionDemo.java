package mg.rivolink;

import mg.rivolink.ai.Network;
import mg.rivolink.mnist.model.MNISTPredictor;
import mg.rivolink.mnist.model.MNISTTrainer;
import mg.rivolink.mnist.text.ImageSegmenter;
import mg.rivolink.mnist.text.TextRecognizer;

import java.util.List;

/**
 * Complete demonstration of text recognition from larger images
 * Shows how TextRecognizer can recognize words and sentences
 */
public class TextRecognitionDemo {

    public static void main(String[] args) {
        System.out.println("=== Text Recognition Demo ===\n");

        // Create a neural network (in real use, load trained model)
        Network network = new Network.Builder()
            .inputSize(784)
            .hiddenSize(128)
            .addHiddenLayer(64)
            .outputSize(62)  // EMNIST: 10 digits + 52 letters
            .learningRate(0.3f)
            .build();

        // Create predictor
        MNISTPredictor predictor = new MNISTPredictor(network, MNISTTrainer.DatasetType.EMNIST);

        // Create text recognizer with language model
        TextRecognizer recognizer = new TextRecognizer(predictor);

        // Example 1: Recognize single word
        System.out.println("=== Example 1: Single Word ===");
        recognizeSingleWord(recognizer);

        // Example 2: Recognize sentence
        System.out.println("\n=== Example 2: Sentence ===");
        recognizeSentence(recognizer);

        // Example 3: Multiple lines
        System.out.println("\n=== Example 3: Multiple Lines ===");
        recognizeMultipleLines(recognizer);

        // Example 4: Show segmentation
        System.out.println("\n=== Example 4: Character Segmentation ===");
        demonstrateSegmentation();
    }

    /**
     * Example 1: Recognize a single word from image
     */
    private static void recognizeSingleWord(TextRecognizer recognizer) {
        // Create image with word "HELLO" (100x28 pixels)
        int width = 140;
        int height = 28;
        float[] image = createWordImage("HELLO", width, height);

        // Visualize
        System.out.println("Input image (140x28):");
        visualizeImage(image, width, height);

        // Recognize
        TextRecognizer.RecognitionResult result = 
            recognizer.recognizeText(image, width, height);

        System.out.println("\nResults:");
        System.out.println("  Raw text: " + result.rawText);
        System.out.println("  Corrected: " + result.correctedText);
        System.out.println("  Confidence: " + String.format("%.2f%%", result.averageConfidence * 100));
        System.out.println("  Characters found: " + result.charDetails.size());
    }

    /**
     * Example 2: Recognize a sentence from larger image
     */
    private static void recognizeSentence(TextRecognizer recognizer) {
        // Create image with sentence "THE CAT" (200x28 pixels)
        int width = 200;
        int height = 28;
        float[] image = createSentenceImage("THE CAT", width, height);

        // Visualize
        System.out.println("Input image (200x28):");
        visualizeImage(image, width, height);

        // Recognize
        TextRecognizer.RecognitionResult result = 
            recognizer.recognizeText(image, width, height);

        System.out.println("\nResults:");
        System.out.println("  Raw text: " + result.rawText);
        System.out.println("  Corrected: " + result.correctedText);
        System.out.println("  Confidence: " + String.format("%.2f%%", result.averageConfidence * 100));
        
        // Show individual character details
        System.out.println("\nCharacter details:");
        for (int i = 0; i < result.charDetails.size(); i++) {
            TextRecognizer.CharDetail detail = result.charDetails.get(i);
            System.out.println("    Char " + (i+1) + ": '" + detail.character + 
                             "' at (" + detail.bound.x + "," + detail.bound.y + 
                             ") confidence: " + String.format("%.2f%%", detail.confidence * 100));
        }
    }

    /**
     * Example 3: Recognize multiple lines
     */
    private static void recognizeMultipleLines(TextRecognizer recognizer) {
        // Create image with two lines (200x56 pixels)
        int width = 200;
        int height = 56;
        float[] image = createMultiLineImage(width, height);

        // Visualize
        System.out.println("Input image (200x56):");
        visualizeImage(image, width, height);

        // Recognize
        TextRecognizer.RecognitionResult result = 
            recognizer.recognizeText(image, width, height);

        System.out.println("\nResults:");
        System.out.println("  Raw text: " + result.rawText);
        System.out.println("  Corrected: " + result.correctedText);
        System.out.println("  Total characters: " + result.charDetails.size());
    }

    /**
     * Example 4: Demonstrate character segmentation
     */
    private static void demonstrateSegmentation() {
        int width = 150;
        int height = 28;
        float[] image = createWordImage("TEXT", width, height);

        // Visualize original
        System.out.println("Original image (150x28):");
        visualizeImage(image, width, height);

        // Segment into characters
        List<ImageSegmenter.CharacterBound> bounds = 
            ImageSegmenter.segmentCharacters(image, width, height);

        System.out.println("\nSegmented characters: " + bounds.size());
        for (int i = 0; i < bounds.size(); i++) {
            ImageSegmenter.CharacterBound bound = bounds.get(i);
            System.out.println("  Character " + (i+1) + ": " + bound);
            
            // Extract and show each character
            float[] charPixels = ImageSegmenter.extractCharacterPixels(image, width, bound);
            System.out.println("  28x28 normalized:");
            visualizeImage(charPixels, 28, 28);
        }
    }

    // ========================================
    // Helper Methods to Create Sample Images
    // ========================================

    /**
     * Create an image containing a single word
     */
    private static float[] createWordImage(String word, int width, int height) {
        float[] image = new float[width * height];
        
        int charWidth = 20;
        int charSpacing = 5;
        int startX = 10;
        
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            int x = startX + i * (charWidth + charSpacing);
            drawCharacter(image, width, height, c, x, 4);
        }
        
        return image;
    }

    /**
     * Create an image containing a sentence with spaces
     */
    private static float[] createSentenceImage(String sentence, int width, int height) {
        float[] image = new float[width * height];
        
        int charWidth = 20;
        int charSpacing = 5;
        int wordSpacing = 15;
        int x = 10;
        
        for (int i = 0; i < sentence.length(); i++) {
            char c = sentence.charAt(i);
            if (c == ' ') {
                x += wordSpacing;
            } else {
                drawCharacter(image, width, height, c, x, 4);
                x += charWidth + charSpacing;
            }
        }
        
        return image;
    }

    /**
     * Create an image with multiple lines of text
     */
    private static float[] createMultiLineImage(int width, int height) {
        float[] image = new float[width * height];
        
        // First line: "HELLO"
        String line1 = "HELLO";
        int y1 = 4;
        int x1 = 10;
        for (int i = 0; i < line1.length(); i++) {
            drawCharacter(image, width, height, line1.charAt(i), x1 + i * 25, y1);
        }
        
        // Second line: "WORLD"
        String line2 = "WORLD";
        int y2 = 32;
        int x2 = 10;
        for (int i = 0; i < line2.length(); i++) {
            drawCharacter(image, width, height, line2.charAt(i), x2 + i * 25, y2);
        }
        
        return image;
    }

    /**
     * Draw a simple character pattern
     * This is a simplified representation - in real use, you'd have actual character bitmaps
     */
    private static void drawCharacter(float[] image, int imgWidth, int imgHeight, 
                                     char c, int startX, int startY) {
        // Simple vertical line for all characters (demonstration only)
        // In real use, you'd have proper character patterns
        int charHeight = 20;
        int charWidth = 15;
        
        // Draw a simple pattern based on character
        switch (Character.toUpperCase(c)) {
            case 'H':
                // Two vertical lines and horizontal middle
                for (int y = 0; y < charHeight; y++) {
                    setPixel(image, imgWidth, imgHeight, startX, startY + y, 1.0f);
                    setPixel(image, imgWidth, imgHeight, startX + charWidth, startY + y, 1.0f);
                }
                for (int x = 0; x < charWidth; x++) {
                    setPixel(image, imgWidth, imgHeight, startX + x, startY + charHeight/2, 1.0f);
                }
                break;
            case 'E':
                // Vertical line and three horizontal
                for (int y = 0; y < charHeight; y++) {
                    setPixel(image, imgWidth, imgHeight, startX, startY + y, 1.0f);
                }
                for (int x = 0; x < charWidth; x++) {
                    setPixel(image, imgWidth, imgHeight, startX + x, startY, 1.0f);
                    setPixel(image, imgWidth, imgHeight, startX + x, startY + charHeight/2, 1.0f);
                    setPixel(image, imgWidth, imgHeight, startX + x, startY + charHeight-1, 1.0f);
                }
                break;
            case 'L':
                // Vertical line and bottom horizontal
                for (int y = 0; y < charHeight; y++) {
                    setPixel(image, imgWidth, imgHeight, startX, startY + y, 1.0f);
                }
                for (int x = 0; x < charWidth; x++) {
                    setPixel(image, imgWidth, imgHeight, startX + x, startY + charHeight-1, 1.0f);
                }
                break;
            case 'O':
                // Circle
                int centerX = startX + charWidth/2;
                int centerY = startY + charHeight/2;
                int radius = charHeight/2 - 2;
                for (int angle = 0; angle < 360; angle += 5) {
                    int x = centerX + (int)(radius * Math.cos(Math.toRadians(angle)));
                    int y = centerY + (int)(radius * Math.sin(Math.toRadians(angle)));
                    setPixel(image, imgWidth, imgHeight, x, y, 1.0f);
                }
                break;
            case 'T':
                // Top horizontal and vertical middle
                for (int x = 0; x < charWidth; x++) {
                    setPixel(image, imgWidth, imgHeight, startX + x, startY, 1.0f);
                }
                for (int y = 0; y < charHeight; y++) {
                    setPixel(image, imgWidth, imgHeight, startX + charWidth/2, startY + y, 1.0f);
                }
                break;
            case 'C':
                // Arc (left side of circle)
                centerX = startX + charWidth/2;
                centerY = startY + charHeight/2;
                radius = charHeight/2 - 2;
                for (int angle = 60; angle <= 300; angle += 5) {
                    int x = centerX + (int)(radius * Math.cos(Math.toRadians(angle)));
                    int y = centerY + (int)(radius * Math.sin(Math.toRadians(angle)));
                    setPixel(image, imgWidth, imgHeight, x, y, 1.0f);
                }
                break;
            case 'A':
                // Triangle shape
                for (int y = 0; y < charHeight; y++) {
                    int leftX = startX + charHeight/2 - y/2;
                    int rightX = startX + charHeight/2 + y/2;
                    setPixel(image, imgWidth, imgHeight, leftX, startY + y, 1.0f);
                    setPixel(image, imgWidth, imgHeight, rightX, startY + y, 1.0f);
                }
                // Middle bar
                for (int x = 0; x < charWidth; x++) {
                    setPixel(image, imgWidth, imgHeight, startX + x, startY + charHeight*2/3, 1.0f);
                }
                break;
            case 'W':
                // W shape
                for (int y = 0; y < charHeight; y++) {
                    setPixel(image, imgWidth, imgHeight, startX, startY + y, 1.0f);
                    setPixel(image, imgWidth, imgHeight, startX + charWidth/3, startY + charHeight - y/2, 1.0f);
                    setPixel(image, imgWidth, imgHeight, startX + 2*charWidth/3, startY + charHeight - y/2, 1.0f);
                    setPixel(image, imgWidth, imgHeight, startX + charWidth, startY + y, 1.0f);
                }
                break;
            case 'R':
                // Vertical line
                for (int y = 0; y < charHeight; y++) {
                    setPixel(image, imgWidth, imgHeight, startX, startY + y, 1.0f);
                }
                // Top arc
                for (int angle = -90; angle <= 90; angle += 5) {
                    int x = startX + charWidth/2 + (int)(charWidth/3 * Math.cos(Math.toRadians(angle)));
                    int y = startY + charHeight/4 + (int)(charHeight/4 * Math.sin(Math.toRadians(angle)));
                    setPixel(image, imgWidth, imgHeight, x, y, 1.0f);
                }
                // Diagonal leg
                for (int i = 0; i < charHeight/2; i++) {
                    setPixel(image, imgWidth, imgHeight, startX + i, startY + charHeight/2 + i, 1.0f);
                }
                break;
            case 'D':
                // Vertical line
                for (int y = 0; y < charHeight; y++) {
                    setPixel(image, imgWidth, imgHeight, startX, startY + y, 1.0f);
                }
                // Right arc
                for (int angle = -90; angle <= 90; angle += 5) {
                    int x = startX + (int)((charWidth/2) * Math.cos(Math.toRadians(angle)));
                    int y = startY + charHeight/2 + (int)((charHeight/2) * Math.sin(Math.toRadians(angle)));
                    setPixel(image, imgWidth, imgHeight, x + charWidth/2, y, 1.0f);
                }
                break;
            default:
                // Default: vertical line
                for (int y = 0; y < charHeight; y++) {
                    setPixel(image, imgWidth, imgHeight, startX + charWidth/2, startY + y, 1.0f);
                }
        }
    }

    /**
     * Set a pixel in the image (with bounds checking)
     */
    private static void setPixel(float[] image, int width, int height, int x, int y, float value) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            image[y * width + x] = value;
        }
    }

    /**
     * Visualize image in console
     */
    private static void visualizeImage(float[] image, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float pixel = image[y * width + x];
                System.out.print(pixel > 0.5f ? "█" : (pixel > 0.1f ? "▓" : " "));
            }
            System.out.println();
        }
    }
}
