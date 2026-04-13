package io.github.andrestubbe.fastio;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A high-performance file handle for native-accelerated I/O.
 * Similar to RandomAccessFile but faster and with more features.
 */
public final class FastFile implements AutoCloseable {
    
    private final java.io.RandomAccessFile raf;
    private final java.nio.channels.FileChannel channel;
    private final String path;
    private final OpenMode mode;
    private boolean closed = false;
    
    FastFile(String path, OpenMode mode) throws IOException {
        this.path = path;
        this.mode = mode;
        
        String rafMode;
        switch (mode) {
            case READ: rafMode = "r"; break;
            case WRITE: 
            case APPEND: rafMode = "rw"; break;
            case READ_WRITE: rafMode = "rw"; break;
            default: rafMode = "r";
        }
        
        this.raf = new java.io.RandomAccessFile(path, rafMode);
        this.channel = raf.getChannel();
    }
    
    /**
     * Allocate a buffer aligned for optimal unbuffered I/O performance.
     * The buffer will be aligned to 4KB boundaries suitable for direct I/O.
     * 
     * @param size the buffer size (will be rounded up to multiple of 4096)
     * @return an aligned direct ByteBuffer
     */
    public static ByteBuffer allocateAlignedBuffer(int size) {
        int alignedSize = ((size + 4095) / 4096) * 4096;
        return ByteBuffer.allocateDirect(alignedSize);
    }
    
    /**
     * Read bytes into a buffer.
     * 
     * @param buffer the destination buffer
     * @return number of bytes actually read
     * @throws IOException if reading fails
     */
    public int read(ByteBuffer buffer) throws IOException {
        ensureOpen();
        return channel.read(buffer);
    }
    
    /**
     * Read bytes into a byte array.
     * 
     * @param bytes the destination array
     * @param offset the offset in the array
     * @param length the number of bytes to read
     * @return number of bytes actually read
     * @throws IOException if reading fails
     */
    public int read(byte[] bytes, int offset, int length) throws IOException {
        ensureOpen();
        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
        return read(buffer);
    }
    
    /**
     * Read a single byte.
     * 
     * @return the byte value, or -1 if EOF
     * @throws IOException if reading fails
     */
    public int read() throws IOException {
        ensureOpen();
        byte[] single = new byte[1];
        int n = read(single, 0, 1);
        return n > 0 ? (single[0] & 0xFF) : -1;
    }
    
    /**
     * Write bytes from a buffer.
     * 
     * @param buffer the source buffer
     * @return number of bytes written
     * @throws IOException if writing fails
     */
    public int write(ByteBuffer buffer) throws IOException {
        ensureOpen();
        return channel.write(buffer);
    }
    
    /**
     * Write bytes from an array.
     * 
     * @param bytes the source array
     * @param offset the offset in the array
     * @param length the number of bytes to write
     * @return number of bytes written
     * @throws IOException if writing fails
     */
    public int write(byte[] bytes, int offset, int length) throws IOException {
        ensureOpen();
        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
        return write(buffer);
    }
    
    /**
     * Write a single byte.
     * 
     * @param b the byte to write
     * @throws IOException if writing fails
     */
    public void write(int b) throws IOException {
        ensureOpen();
        byte[] single = new byte[] { (byte) b };
        write(single, 0, 1);
    }
    
    /**
     * Get current file position.
     * 
     * @return the current position
     */
    public long getPosition() throws IOException {
        return channel.position();
    }
    
    /**
     * Set file position (seek).
     * 
     * @param position the new position
     * @throws IOException if seeking fails
     */
    public void seek(long position) throws IOException {
        ensureOpen();
        channel.position(position);
    }
    
    /**
     * Get file size.
     * 
     * @return the file size in bytes
     * @throws IOException if the operation fails
     */
    public long size() throws IOException {
        ensureOpen();
        return channel.size();
    }
    
    /**
     * Truncate or extend the file.
     * 
     * @param newSize the new size
     * @throws IOException if the operation fails
     */
    public void setSize(long newSize) throws IOException {
        ensureOpen();
        channel.truncate(newSize);
    }
    
    /**
     * Force all writes to disk (fsync).
     * 
     * @throws IOException if the operation fails
     */
    public void sync() throws IOException {
        ensureOpen();
        channel.force(true);
    }
    
    /**
     * Check if file is open.
     * 
     * @return true if open
     */
    public boolean isOpen() {
        return !closed;
    }
    
    /**
     * Get the file path.
     * 
     * @return the path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Get the open mode.
     * 
     * @return the mode
     */
    public OpenMode getMode() {
        return mode;
    }
    
    @Override
    public void close() throws IOException {
        if (!closed) {
            channel.close();
            raf.close();
            closed = true;
        }
    }
    
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("File is closed: " + path);
        }
    }
}
