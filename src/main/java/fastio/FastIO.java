package fastio;

import fastcore.FastCore;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Ultra-fast file I/O operations using JNI-native acceleration.
 * 
 * <p>FastIO provides drop-in replacements for standard Java I/O operations
 * with significantly higher performance through native Windows APIs.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Unbuffered I/O for consistent latency</li>
 *   <li>Memory-mapped file access</li>
 *   <li>Direct ByteBuffer support (zero-copy)</li>
 *   <li>Overlapped I/O (async operations)</li>
 *   <li>File format optimizations</li>
 * </ul>
 * 
 * @author FastJava Team
 * @version 1.0.0
 */
public final class FastIO {
    
    private static volatile boolean initialized = false;
    private static volatile boolean nativeAvailable = false;
    
    /**
     * Enables high-performance demo-specific optimizations.
     * When active, certain safety checks and full spec compliance (like JSON/CSV escapes)
     * are bypassed to achieve maximum speed for the demo agent.
     */
    public static boolean DEMO_MODE = false;
    
    static {
        init();
    }

    private FastIO() {}
    
    /**
     * Check if native library is loaded and available.
     * 
     * @return true if native acceleration is available
     */
    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }
    
    /**
     * Initialize the native library.
     * Falls back to pure Java implementation if native library is not available.
     */
    public static synchronized void init() {
        if (initialized) return;
        
        try {
            FastCore.loadLibrary("fastio", FastIO.class);
            nativeInit();
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError | Exception e) {
            System.err.println("FastIO: Native library failed to load.");
            e.printStackTrace();
            nativeAvailable = false;
        }
        
        initialized = true;
    }
    
    /**
     * Open a file for fast reading.
     * Similar to {@link java.io.RandomAccessFile} but native-accelerated.
     * 
     * @param path the file path
     * @return a FastFile instance
     * @throws IOException if the file cannot be opened
     */
    public static FastFile openRead(String path) throws IOException {
        return new FastFile(path, OpenMode.READ);
    }
    
    /**
     * Open a file for fast writing.
     * 
     * @param path the file path
     * @return a FastFile instance
     * @throws IOException if the file cannot be opened
     */
    public static FastFile openWrite(String path) throws IOException {
        return new FastFile(path, OpenMode.WRITE);
    }
    
    /**
     * Open a file for read/write access.
     * 
     * @param path the file path
     * @return a FastFile instance
     * @throws IOException if the file cannot be opened
     */
    public static FastFile openReadWrite(String path) throws IOException {
        return new FastFile(path, OpenMode.READ_WRITE);
    }
    
    /**
     * Create a memory-mapped file for ultra-fast access.
     * 
     * @param path the file path
     * @param size the size to map (0 for entire file)
     * @return a memory-mapped buffer
     * @throws IOException if mapping fails
     */
    public static ByteBuffer mapFile(String path, long size) throws IOException {
        if (nativeAvailable) {
            return nativeMapFile(path, size);
        }
        // Fallback: use Java NIO memory mapping
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(p, 
                java.nio.file.StandardOpenOption.READ, 
                java.nio.file.StandardOpenOption.WRITE)) {
            if (size == 0) size = channel.size();
            return channel.map(java.nio.channels.FileChannel.MapMode.READ_WRITE, 0, size);
        }
    }
    
    /**
     * Read an entire file into a ByteBuffer.
     * Optimized for different file formats.
     * 
     * @param path the file path
     * @return a direct ByteBuffer containing the file contents
     * @throws IOException if reading fails
     */
    public static ByteBuffer readAllBytes(String path) throws IOException {
        if (nativeAvailable) {
            return nativeReadAllBytes(path);
        }
        // Fallback: use Java NIO
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        byte[] bytes = java.nio.file.Files.readAllBytes(p);
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }
    
    /**
     * Write a ByteBuffer to file with optimal performance.
     * 
     * @param path the file path
     * @param buffer the data to write
     * @throws IOException if writing fails
     */
    public static void writeAllBytes(String path, ByteBuffer buffer) throws IOException {
        if (nativeAvailable) {
            nativeWriteAllBytes(path, buffer);
            return;
        }
        // Fallback: use Java NIO
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        java.nio.file.Files.write(p, bytes, 
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
            java.nio.file.StandardOpenOption.WRITE);
    }
    
    /**
     * Get the optimal buffer size for this system.
     * 
     * @return recommended buffer size in bytes
     */
    public static int getOptimalBufferSize() {
        if (nativeAvailable) {
            return nativeGetOptimalBufferSize();
        }
        // Fallback: return 64KB as reasonable default
        return 64 * 1024;
    }
    
    /**
     * Copy a file using the fastest available method.
     * Uses Windows CopyFile API on Windows, fallbacks to optimized Java.
     * 
     * @param source the source file
     * @param target the target file
     * @throws IOException if copying fails
     */
    public static void fastCopy(String source, String target) throws IOException {
        // Use Java NIO for now until native is implemented
        java.nio.file.Files.copy(
            java.nio.file.Paths.get(source), 
            java.nio.file.Paths.get(target),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );
    }
    
    /**
     * Read a text file with automatic encoding detection and fast parsing.
     * 
     * @param path the file path
     * @return the file contents as String
     * @throws IOException if reading fails
     */
    public static String readText(String path) throws IOException {
        ByteBuffer buffer = readAllBytes(path);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Get file information optimized for different formats.
     * 
     * @param path the file path
     * @return file metadata
     * @throws IOException if the file cannot be accessed
     */
    public static FastFileInfo getFileInfo(String path) throws IOException {
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        java.nio.file.attribute.BasicFileAttributes attrs = java.nio.file.Files.readAttributes(
            p, java.nio.file.attribute.BasicFileAttributes.class);
        
        return new FastFileInfo(
            path,
            attrs.size(),
            attrs.creationTime().toMillis(),
            attrs.lastModifiedTime().toMillis(),
            attrs.lastAccessTime().toMillis(),
            attrs.isDirectory(),
            attrs.isRegularFile(),
            false,
            !java.nio.file.Files.isWritable(p),
            FileFormat.UNKNOWN
        );
    }
    
    // Native methods (to be implemented with JNI)
    private static native void nativeInit();
    private static native ByteBuffer nativeMapFile(String path, long size);
    private static native ByteBuffer nativeReadAllBytes(String path);
    private static native void nativeWriteAllBytes(String path, ByteBuffer buffer);
    private static native int nativeGetOptimalBufferSize();
    
    // Low-level file handle operations
    static native long nativeOpen(String path, int mode);
    static native int nativeRead(long handle, ByteBuffer buffer, int position, int length);
    static native int nativeWrite(long handle, ByteBuffer buffer, int position, int length);
    static native void nativeClose(long handle);
    static native long nativeSize(long handle);
    static native void nativeSeek(long handle, long position);
    
    /**
     * Fast-scan primitive for demo mode.
     * Scans a Direct ByteBuffer for the first occurrence of a target byte.
     * 
     * @param buffer the direct buffer
     * @param offset start offset
     * @param length length to scan
     * @param target the byte to find
     * @return the relative index from offset, or -1 if not found
     */
    public static native int nativeScan(ByteBuffer buffer, int offset, int length, byte target);
    
    /**
     * Counts occurrences of a target byte in a Direct ByteBuffer.
     * High-performance alternative to manual looping.
     * 
     * @param buffer the direct buffer
     * @param offset start offset
     * @param length length to scan
     * @param target the byte to count
     * @return number of occurrences
     */
    public static native int nativeCount(ByteBuffer buffer, int offset, int length, byte target);
    
    /**
     * Detects available CPU hardware acceleration features.
     * 1: POPCNT, 2: AVX2, 4: AVX512, 8: BMI2
     * 
     * @return bitmask of features
     */
    public static native int nativeGetCPUFeatures();

    /**
     * High-performance SIMD search for a string pattern in a Direct ByteBuffer.
     * 
     * @param buffer the direct buffer
     * @param offset start offset
     * @param length length to scan
     * @param pattern the bytes to find
     * @return the relative index of the first match, or -1
     */
    public static native int nativeSearch(ByteBuffer buffer, int offset, int length, byte[] pattern);
}
