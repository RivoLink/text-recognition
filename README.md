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
