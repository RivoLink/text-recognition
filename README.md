# Text Recognition v1.0.0
A lightweight neural network project for recognizing handwritten text using the MNIST dataset.

## Dataset Download
Run the dataset downloader task via:

```bash
./gradlew downloadDataset -Pdataset=emnist
```

Switch to MNIST by using `-Pdataset=mnist`  
The downloaded dataset is stored under `app/data`

## Model Training
Train the model with:

```bash
./gradlew trainModel -Pdataset=emnist
```

Swap to MNIST training by passing `-Pdataset=mnist`  
Model checkpoints are written to `app/models`

## Model Evaluation
Evaluate the model with:

```bash
./gradlew evaluateModel -Pdataset=emnist
```

Switch to MNIST by using `-Pdataset=mnist`

## Model Retraining
Resume training of the model with:

```bash
./gradlew retrainModel -Pdataset=emnist -Pepochs=2
```

Swap to MNIST training by passing `-Pdataset=mnist`  
Model must already exist in `app/models` to retrain it.

## Image Segmentation
Segment an image with:

```bash
./gradlew segmentImage -Pimage=<path>

# examples
./gradlew segmentImage -Pimage=images/explain.png
./gradlew segmentImage -Pimage=images/reproduce.jpeg
```

## Image Pre-processing
Pre-process before segmenting an image with:

```bash
./gradlew preprocessImage -Pimage=<path>

# examples
./gradlew preprocessImage -Pimage=images/explain.png
./gradlew preprocessImage -Pimage=images/reproduce.jpeg
```

## Language Model Demo
Run the language model demo with:

```bash
./gradlew demoLanguageModel
```

## Text Recognition Pipeline
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
