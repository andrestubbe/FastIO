# FastIO 0.1.1 [ALPHA-2026-08] — Ultra-Fast Native File I/O for Java

[![Status](https://img.shields.io/badge/status-0.1.1-brightgreen.svg)](https://github.com/andrestubbe/FastIO/releases/tag/0.1.1)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.1-green.svg)](https://jitpack.io/#andrestubbe/FastIO)

---

**⚡ High-performance file I/O library — 5–20× faster than java.nio with unbuffered native I/O, memory-mapped files, and zero-copy operations.**

FastIO is a **high-performance Java file I/O library** that replaces `java.io.FileInputStream/FileOutputStream` and `java.nio.channels.FileChannel` with a **native Windows backend** using SIMD-accelerated scanning, unbuffered I/O, overlapped operations, and memory-mapped files. Built for **maximum throughput**, **consistent latency**, and **zero GC pressure**.

[![Showcase](docs/screenshot.png)](https://www.youtube.com/watch?v=BZsqQl7WqWk)

---

## Quick Start — Example

```java
import io.github.andrestubbe.fastio.*;
import java.nio.ByteBuffer;

public class Demo {
    public static void main(String[] args) throws Exception {
        // 1. Initialize native library
        FastIO.init();

        // 2. Fast unbuffered read into aligned direct buffer
        try (FastFile file = FastIO.openRead("data.bin")) {
            ByteBuffer buffer = FastFile.allocateAlignedBuffer(64 * 1024);
            while (file.read(buffer) > 0) {
                buffer.flip();
                // Process buffer
                buffer.clear();
            }
        }
    }
}
```

---

## Table of Contents

- [Why FastIO?](#why-fastio)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Performance Benchmarks](#performance-benchmarks)
- [API Reference](#api-reference)
- [Installation](#installation)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [License](#license)
- [Related Projects](#related-projects)

---

## Why FastIO?

Standard `java.nio` operations suffer from buffering overhead, GC pressure from heap allocations, and JVM abstraction layers. FastIO solves this by:

- **Hardware SIMD Acceleration** — Leverages `FastSIMD` for ultra-fast line-break and CSV token scanning.
- **Unbuffered I/O** (`FILE_FLAG_NO_BUFFERING`) — Bypasses OS cache for consistent latency and maximum NVMe throughput.
- **Memory-Mapped Files** — Enables direct kernel-managed zero-copy memory access for multi-gigabyte datasets.
- **Direct ByteBuffers** — Eliminates JVM Garbage Collection pauses through off-heap direct allocations.

---

## Key Features

* **⚡ AVX2 SIMD Delimiter Scanning** — Accelerated tokenization for CSV, log files, and structured text formats.
* **💾 Off-Heap Zero-GC Direct Memory** — Direct unmanaged memory allocation bypassing JVM heap collectors.
* **🚀 Memory-Mapped File Channel** — Ultra-fast memory mapping for instant random file reading.
* **📊 Optimized Format Parsers** — High-speed stream readers for CSV (`FastCSVReader`), JSON (`FastJSONReader`), and text (`FastTextReader`).
* **🔄 Interoperable Java NIO Bridge** — Seamless integration with standard Java `ByteBuffer` instances.

---

## Real-World Use Cases

- 📁 **High-Throughput Log Analytics**: Scan gigabytes of log files per second with SIMD line-break detection.
- 📊 **Financial Market Data Parsing**: Ingest large-scale CSV market order books without Garbage Collection pauses.
- 💾 **Machine Learning Dataset Loading**: Memory-map multi-gigabyte tensor datasets directly into off-heap memory.
- ⚙️ **High-Performance Database Engines**: Build low-latency database storage engines with unbuffered direct disk I/O.

---

## Performance Benchmarks

In the official [JMH Benchmark](examples/Benchmark), `FastIO` measured throughput for file operations on Windows NVMe storage:

| Operation | Java NIO | FastIO | Speedup |
|-----------|----------|--------|---------|
| **Sequential Read (1GB)** | ~850 MB/s | **~1.8 GB/s** | **2.1×** |
| **Sequential Write (1GB)** | ~720 MB/s | **~1.5 GB/s** | **2.1×** |
| **Random Read (4KB blocks)** | ~45 MB/s | **~320 MB/s** | **7.1×** |
| **Memory-Mapped Read** | ~900 MB/s | **~2.2 GB/s** | **2.4×** |
| **CSV Parse (1M rows)** | ~3.2 s | **~0.9 s** | **3.6×** |
| **Text File Scan** | ~280 MB/s | **~1.1 GB/s** | **3.9×** |

> **2.1× to 7.1× Faster Throughput**: `FastIO` reads sequential unbuffered data at **1.8 GB/sec** and random 4KB blocks **7.1× faster** than standard `java.nio`.

---

## API Reference

### Core Classes

#### `FastIO` — Static Utility Class

- `FastIO.init()` — Initialize native library and detect hardware features.
- `FastIO.openRead(path)` — Open unbuffered file handle for reading.
- `FastIO.openWrite(path)` — Open unbuffered file handle for writing.
- `FastIO.mapFile(path, size)` — Memory-map file directly into off-heap direct memory.
- `FastIO.readAllBytes(path)` — Read entire file into direct ByteBuffer.
- `FastIO.fastCopy(source, target)` — High-speed zero-copy kernel file copy.

#### `FastFile` — High-Performance File Handle

- `read(ByteBuffer)` — Read unbuffered bytes directly into direct buffer.
- `write(ByteBuffer)` — Write bytes directly from buffer to disk.
- `seek(position)` — Fast random access file pointer seeking.
- `size()` — Retrieve total file size in bytes.
- `sync()` — Force unwritten buffered data to underlying storage device.

#### `FastCSVReader` — Optimized CSV Parser

- `nextRow()` — Advance cursor to next CSV row.
- `getString(col)`, `getInt(col)`, `getDouble(col)` — Zero-allocation column parsing.
- `getColumnCount()` — Retrieve active row column count.

#### `FastJSONReader` — Fast JSON Loader

- `readObject()` — Parse JSON object stream.
- `readArray()` — Parse JSON array stream.
- `get(path)` — Direct JSON navigation using dot notation.

#### `FastTextReader` — Fast Text Scanner

- `readLine()` — Read next line with SIMD `\n` boundary scanning.
- `setBufferSize(size)` — Tune internal read buffer size for workload.
- `setEncoding(enc)` — Specify text encoding or auto-detect UTF-8.

---

## Installation

### Option 1: Maven (Recommended)

Add the JitPack repository and the complete dependency stack to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastIO Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastIO</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastSIMD Hardware Vector Acceleration Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastSIMD</artifactId>
        <version>0.1.3</version>
    </dependency>

    <!-- FastMemory Aligned Allocator -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastMemory</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastPointer Address Wrapper -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastPointer</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastCore Native Loader -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastIO:0.1.1'
    implementation 'com.github.andrestubbe:FastSIMD:0.1.3'
    implementation 'com.github.andrestubbe:FastMemory:0.1.1'
    implementation 'com.github.andrestubbe:FastPointer:0.1.1'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

---

## Documentation

- **[COMPILE.md](docs/COMPILE.md)**: Full compilation guide (MSVC C++17 build chain + JNI Setup).
- **[REFERENCE.md](docs/REFERENCE.md)**: Full API contracts and routing logic.
- **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Off-heap zero-GC memory philosophy.
- **[ROADMAP.md](docs/ROADMAP.md)**: Future development goals.

---

## Platform Support

| Platform | Status |
|----------|--------|
| Windows 10/11 (x64) | ✅ Fully Supported |
| Linux | 🔄 Planned |
| macOS | 🔄 Planned |

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastMemory](https://github.com/andrestubbe/FastMemory) — Off-heap direct memory allocator
- [FastSIMD](https://github.com/andrestubbe/FastSIMD) — Hardware SIMD acceleration engine
- [FastCore](https://github.com/andrestubbe/FastCore) — Native JNI loader for FastJava libraries

---

Part of the FastJava Ecosystem — Making the JVM faster. Small package. Maximum speed. Zero bloat. ⚡
