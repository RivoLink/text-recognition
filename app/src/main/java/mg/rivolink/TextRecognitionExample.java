package mg.rivolink;

import mg.rivolink.ai.Network;
import mg.rivolink.mnist.model.MNISTPredictor;
import mg.rivolink.mnist.model.MNISTTrainer;
import mg.rivolink.mnist.text.SimpleLanguageModel;
import mg.rivolink.mnist.text.TextRecognizer;

/**
 * Example usage of the complete text recognition system
 */
public class TextRecognitionExample {

    public static void main(String[] args) {
        System.out.println("=== Text Recognition System Example ===\n");

        try {
            // Create a simple neural network for demonstration
            Network network = new Network.Builder()
                .inputSize(784)
                .hiddenSize(128)
                .addHiddenLayer(64)
                .outputSize(62)  // EMNIST: 10 digits + 52 letters
                .learningRate(0.3f)
                .build();

            System.out.println("Created neural network:");
            System.out.println("  Input: 784 (28x28 pixels)");
            System.out.println("  Hidden1: 128 neurons");
            System.out.println("  Hidden2: 64 neurons");
            System.out.println("  Output: 62 classes (0-9, A-Z, a-z)\n");

            // Create predictor
            MNISTPredictor predictor = new MNISTPredictor(network, MNISTTrainer.DatasetType.EMNIST);
            System.out.println("Created EMNIST predictor\n");

            // Create language model
            SimpleLanguageModel languageModel = new SimpleLanguageModel();
            System.out.println("Loaded language model with common English words\n");

            // Create text recognizer
            TextRecognizer recognizer = new TextRecognizer(predictor, languageModel);
            System.out.println("Created text recognizer pipeline\n");

            // Example: Test single character prediction
            System.out.println("=== Example: Single Character ===");
            float[] sampleImage = createSampleImage();  // Create a sample 28x28 image
            String recognized = recognizer.recognizeSingleCharacter(sampleImage);
            System.out.println("Recognized character: " + recognized);
            System.out.println("(Note: Network is untrained, prediction is random)\n");

            // Show usage instructions
            printUsageInstructions();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static float[] createSampleImage() {
        // Create a blank 28x28 image (all zeros)
        return new float[784];
    }

    private static void printUsageInstructions() {
        System.out.println("=== Usage Instructions ===\n");
        
        System.out.println("1. Train the model:");
        System.out.println("   ./gradlew downloadEmnist");
        System.out.println("   ./gradlew trainEmnist\n");
        
        System.out.println("2. Load trained model and use:");
        System.out.println("   Network network = loadTrainedModel(\"models/emnist_model.ser\");");
        System.out.println("   MNISTPredictor predictor = new MNISTPredictor(network, EMNIST);");
        System.out.println("   TextRecognizer recognizer = new TextRecognizer(predictor);\n");
        
        System.out.println("3. Recognize text from image:");
        System.out.println("   float[] imagePixels = loadImagePixels(\"image.png\");");
        System.out.println("   TextRecognizer.RecognitionResult result =");
        System.out.println("       recognizer.recognizeText(imagePixels, width, height);");
        System.out.println("   System.out.println(\"Raw: \" + result.rawText);");
        System.out.println("   System.out.println(\"Corrected: \" + result.correctedText);\n");
        
        System.out.println("Components:");
        System.out.println("  - ImageSegmenter: Separates characters using connected components");
        System.out.println("  - MNISTPredictor: Recognizes individual characters with EMNIST");
        System.out.println("  - SimpleLanguageModel: Validates words and corrects spelling");
        System.out.println("  - TextRecognizer: Orchestrates the pipeline");
    }
}
