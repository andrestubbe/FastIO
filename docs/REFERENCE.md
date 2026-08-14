# FastIO API Reference Manual

`FastIO` provides high-performance native unbuffered file I/O, memory-mapped files, and SIMD-accelerated text and CSV scanning.

---

## 1. Core Classes & Initialization

### `openRead`
```java
public static FastFile openRead(String path) throws IOException
```
Opens a file for high-speed unbuffered native reading.

---

### `openWrite`
```java
public static FastFile openWrite(String path) throws IOException
```
Opens a file for high-speed unbuffered native writing.

---

### `mapFile`
```java
public static ByteBuffer mapFile(String path, long size) throws IOException
```
Memory-maps a file directly into off-heap direct memory for zero-copy access.

---

## 2. FastFile Operations

### `read`
```java
public int read(ByteBuffer dst) throws IOException
```
Reads data from the file into the direct `ByteBuffer`.

---

### `write`
```java
public int write(ByteBuffer src) throws IOException
```
Writes data from the `ByteBuffer` directly to the file on disk.

---

## 3. Specialized Readers

### `FastCSVReader`
Zero-allocation CSV parser using SIMD-accelerated delimiter scanning.

### `FastJSONReader`
High-speed JSON loader with lazy object parsing.

### `FastTextReader`
Ultra-fast text file scanner with AVX2 line-break scanning.
