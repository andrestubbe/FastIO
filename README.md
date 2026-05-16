# FastIO v0.1.0 — Ultra-Fast Native File I/O for Java

**⚡ High-performance file I/O library — 5-20× faster than java.nio with unbuffered native I/O and zero-copy operations.**

[![Build](https://img.shields.io/github/actions/workflow/status/andrestubbe/FastIO/maven.yml?branch=main)](https://github.com/andrestubbe/FastIO/actions)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows-lightgrey.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JitPack](https://jitpack.io/v/andrestubbe/FastIO.svg)](https://jitpack.io/#andrestubbe/FastIO)

---

FastIO is a **high-performance Java file I/O library** that replaces `java.io` and `java.nio` with a **native Windows backend**. Built for maximum throughput and zero-copy data processing.

```java
// Quick Start — Example
import fastio.FastIO;
import java.nio.ByteBuffer;

public class Demo {
    public static void main(String[] args) {
        // Read entire file into a direct buffer (zero-copy)
        ByteBuffer data = FastIO.readAllBytes("config.json");
        System.out.println("Read " + data.remaining() + " bytes.");
    }
}
```

---

## Table of Contents
- [Key Features](#key-features)
- [Performance](#performance)
- [Installation](#installation)
- [License](#license)
- [Related Projects](#related-projects)

---

## Key Features
- **🚀 Native Performance** — Direct Win32 access via JNI.
- **⚡ Zero Overhead** — Memory-mapped files and direct ByteBuffers.
- **📦 Zero Dependencies** — Only requires `FastCore`.
- **🛠️ Format Optimizations** — Built-in high-speed readers.

---

## Installation

FastIO requires **two** dependencies: the module itself and `FastCore` (the native loader).

### Maven (JitPack)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- 1. The FastIO Module -->
    <dependency>
        <groupId>io.github.andrestubbe</groupId>
        <artifactId>fastio</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- 2. FastCore (Mandatory Native Loader) -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastcore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Gradle (JitPack)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'io.github.andrestubbe:fastio:0.1.0'
    implementation 'com.github.andrestubbe:fastcore:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)
Download the latest JARs directly to add them to your classpath:

1. 📦 [**fastio-v0.1.0.jar**](https://github.com/andrestubbe/FastIO/releases/download/v0.1.0/fastio-0.1.0.jar)
2. ⚙️ [**fastcore-v0.1.0.jar**](https://github.com/andrestubbe/fastcore/releases/download/v0.1.0/fastcore-0.1.0.jar)

---

## License
MIT License — See [LICENSE](LICENSE) for details.

---

## Related Projects
- [FastCore](https://github.com/andrestubbe/FastCore) — Native Library Loader
- [FastJSON](https://github.com/andrestubbe/FastJSON) — Native JSON acceleration

---
**Made with ⚡ by Andre Stubbe**
