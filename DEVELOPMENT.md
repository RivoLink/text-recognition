## Text Recognition
A lightweight neural network project for recognizing handwritten text using the MNIST dataset.

### Initialize Gradle project
```sh
gradle init --type java-application --dsl groovy
```

### Setup `neural-network.jar`

#### 1. Add Git Submodule
```sh
cd text-recognition

# Add neural-network as submodule
git submodule add https://github.com/RivoLink/neural-network libs/neural-network

# Initialize and update
git submodule init
git submodule update
```

#### 2. Build JAR and verify its contents
```sh
cd libs/neural-network

# Build with clean (recommended first time)
./scripts/build.sh --clean --target=11

# Output location: dist/neural-network.jar
# Also creates: dist/neural-network-sources.jar

# Check JAR contents
jar tf dist/neural-network.jar

# Check JAR size
ls -lh dist/neural-network.jar
```
