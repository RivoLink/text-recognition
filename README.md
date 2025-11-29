## Text Recognition
A lightweight neural network project for recognizing handwritten text using the MNIST dataset.

### Dataset
Run the dataset downloader task via:

```bash
./gradlew downloadDataset -Pdataset=emnist
```

Switch to MNIST by using `-Pdataset=mnist`  
The downloaded dataset is stored under `app/data`

### Training
Train the neural network with:

```bash
./gradlew trainModel -Pdataset=emnist
```

Swap to MNIST training by passing `-Pdataset=mnist`  
Model checkpoints are written to `app/models`

### Evaluation
Evaluate the neural network with:

```bash
./gradlew evaluateModel -Pdataset=emnist
```

Switch to MNIST by using `-Pdataset=mnist`

### Segmentation
Segment an image with:

```bash
./gradlew segmentImage -Pimage=<path>

# examples
./gradlew segmentImage -Pimage=images/explain.png
./gradlew segmentImage -Pimage=images/reproduce.jpeg
```

### Pre-processing
Pre-process before segment an image with:

```bash
./gradlew preprocessImage -Pimage=<path>

# examples
./gradlew preprocessImage -Pimage=images/explain.png
./gradlew preprocessImage -Pimage=images/reproduce.jpeg
```

### Language Model
Run the language model demo with:

```bash
./gradlew demoLanguageModel
```

### Text Recognition
Run the end-to-end text recognition (defaults to EMNIST):

```bash
./gradlew recognizeText -Pimage=<path>
```

Switch to MNIST by passing `-Pdataset=mnist`

```bash
# example
./gradlew recognizeText -Pimage=images/explain.png
./gradlew recognizeText -Pdataset=mnist -Pimage=images/digits.png
```
