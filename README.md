## Text Recognition
A lightweight neural network project for recognizing handwritten text using the MNIST dataset.

### Components

1. **ImageSegmenter** - Separates characters using connected components
2. **MNISTPredictor** - Recognizes individual characters with EMNIST
3. **SimpleLanguageModel** - Validates words and corrects spelling
4. **TextRecognizer** - Orchestrates the complete pipeline

### Usage Example
```java
// Load trained model
Network network = loadTrainedModel("models/emnist_model.ser");
MNISTPredictor predictor = new MNISTPredictor(network, DatasetType.EMNIST);

// Create text recognizer
TextRecognizer recognizer = new TextRecognizer(predictor);

// Recognize text from image
float[] imagePixels = loadImagePixels("image.png");
TextRecognizer.RecognitionResult result = 
    recognizer.recognizeText(imagePixels, width, height);

// Get results
System.out.println("Raw: " + result.rawText);
System.out.println("Corrected: " + result.correctedText);
// Raw: 'tne quick brown fox'
// Corrected: 'the quick brown fox'
```

### Quick Start
```bash
# Download EMNIST dataset
./gradlew downloadEmnist

# Train model
./gradlew trainEmnist

# Run application
./gradlew run
```
