#!/bin/bash

# Set the target directory
TARGET_DIR="/Users/lucianopena/iceberg_polaris_lakehouse/spark/.venv/lib/python3.9/site-packages/pyspark/jars/"

# Download Iceberg Spark Runtime JAR
echo "Downloading Iceberg Spark Runtime JAR..."
curl -o iceberg-spark-runtime-3.5_2.12-1.5.2.jar https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-3.5_2.12/1.5.2/iceberg-spark-runtime-3.5_2.12-1.5.2.jar

# Download Hadoop AWS JAR
echo "Downloading Hadoop AWS JAR..."
curl -o hadoop-aws-3.4.0.jar https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/3.4.0/hadoop-aws-3.4.0.jar

# Download AWS SDK Bundle JAR
echo "Downloading AWS SDK Bundle JAR..."
curl -o bundle-2.23.19.jar https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/2.23.19/bundle-2.23.19.jar

# Download AWS SDK URL Connection Client JAR
echo "Downloading AWS SDK URL Connection Client JAR..."
curl -o url-connection-client-2.23.19.jar https://repo1.maven.org/maven2/software/amazon/awssdk/url-connection-client/2.23.19/url-connection-client-2.23.19.jar

# Copy JAR files to the target directory
echo "Copying JAR files to $TARGET_DIR..."
cp iceberg-spark-runtime-3.5_2.12-1.5.2.jar "$TARGET_DIR"
cp hadoop-aws-3.4.0.jar "$TARGET_DIR"
cp bundle-2.23.19.jar "$TARGET_DIR"
cp url-connection-client-2.23.19.jar "$TARGET_DIR"

# Clean up downloaded JAR files from the current directory
rm -f iceberg-spark-runtime-3.5_2.12-1.5.2.jar
rm -f hadoop-aws-3.4.0.jar
rm -f bundle-2.23.19.jar
rm -f url-connection-client-2.23.19.jar

echo "All JAR files have been downloaded and copied successfully!"
