#!/bin/bash

# Default values
RATE=5000
DURATION=80

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -r|--rate) RATE="$2"; shift ;;
        -d|--duration) DURATION="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

echo "Compiling..."
mkdir -p bin
javac -d bin -sourcepath src src/topk/TopKEvaluation.java

if [ $? -ne 0 ]; then
    echo "Compilation failed."
    exit 1
fi

echo "Running Benchmark (Rate=$RATE, Duration=$DURATION)..."
java -cp bin topk.TopKEvaluation $RATE $DURATION
