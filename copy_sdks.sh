#!/bin/bash

# Define source paths (assuming home directory expansion)
SRC1=~/FitCloudPro-SDK-Android/libs/sdk-fitcloud-v3.0.2.aar
SRC2=~/FitCloudPro-SDK-Android/libs/sdk-base-v3.0.2.aar
SRC3=~/FitCloudPro-SDK-Android/files/libraryCore_rxjava3_v1.1.9.aar

# Define destination path
DEST=/home/shivprasad/AndroidStudioProjects/Nanu/app/libs/

# Create destination directory if it doesn't exist
mkdir -p "$DEST"

# Copy files
cp "$SRC1" "$DEST"
cp "$SRC2" "$DEST"
cp "$SRC3" "$DEST"

echo "✅ SDK files copied"
