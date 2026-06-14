# The Philosophy of FastIO

> [!IMPORTANT]
> **"Keine Kopien. Niemals. Kritischer JNI-Pfad. Native-First Performance."**

FastIO is built on the principle that modern Java applications are fundamentally bottlenecked by the I/O layer. The standard `java.nio` API, while powerful, is limited by JVM buffering, heap allocations, and OS abstraction overhead. FastIO removes these layers entirely.

## Core Tenets

1.  **Native-First Execution**
    Bypass the JVM I/O stack entirely. Use Windows `CreateFile`, `ReadFile`, `WriteFile`, and `MapViewOfFile` directly via JNI for maximum throughput.

2.  **Unbuffered Direct I/O**
    `FILE_FLAG_NO_BUFFERING` eliminates the OS page cache double-copy. Data flows directly from NVMe storage to application memory in a single pass.

3.  **Zero-Copy Architecture**
    All read/write paths use `DirectByteBuffer` — data never touches the Java heap. No GC pressure, no copy overhead, no JVM allocation.

4.  **Deterministic Latency**
    Overlapped I/O (`FILE_FLAG_OVERLAPPED`) enables true async operations without blocking threads — critical for high-throughput pipelines.

5.  **Blueprint Consistency**
    As part of the **FastJava** ecosystem, FastIO adheres to the standardized architecture:
    *   **Native Backend**: Direct C++ implementation via JNI.
    *   **Unified Loading**: Powered by `FastCore`.
    *   **Premium Quality**: Built for high-performance systems and autonomous agents.

---
**⚡ FastIO — Pushing Java file I/O to the physical limits of NVMe storage.**
