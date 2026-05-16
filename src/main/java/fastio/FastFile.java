package fastio;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A high-performance file handle for native-accelerated I/O.
 * Uses Win32 unbuffered I/O when native acceleration is available.
 */
public final class FastFile implements AutoCloseable {
    
    private final java.io.RandomAccessFile raf;
    private final java.nio.channels.FileChannel channel;
    private final String path;
    private final OpenMode mode;
    private final long nativeHandle;
    private boolean closed = false;
    
    FastFile(String path, OpenMode mode) throws IOException {
        this.path = path;
        this.mode = mode;
        
        if (FastIO.isNativeAvailable()) {
            int nativeMode = 0; // READ
            if (mode == OpenMode.WRITE) nativeMode = 1;
            else if (mode == OpenMode.READ_WRITE) nativeMode = 2;
            
            this.nativeHandle = FastIO.nativeOpen(path, nativeMode);
            if (this.nativeHandle == -1 || this.nativeHandle == 0) {
                // Fallback if native open fails
                this.raf = createRaf(path, mode);
                this.channel = raf.getChannel();
            } else {
                this.raf = null;
                this.channel = null;
            }
        } else {
            this.nativeHandle = 0;
            this.raf = createRaf(path, mode);
            this.channel = raf.getChannel();
        }
    }

    private static java.io.RandomAccessFile createRaf(String path, OpenMode mode) throws IOException {
        String rafMode;
        switch (mode) {
            case READ: rafMode = "r"; break;
            case WRITE: 
            case APPEND: 
            case READ_WRITE: rafMode = "rw"; break;
            default: rafMode = "r";
        }
        return new java.io.RandomAccessFile(path, rafMode);
    }
    
    public int read(ByteBuffer buffer) throws IOException {
        ensureOpen();
        if (nativeHandle != 0) {
            int n = FastIO.nativeRead(nativeHandle, buffer, buffer.position(), buffer.remaining());
            if (n > 0) buffer.position(buffer.position() + n);
            return n;
        }
        return channel.read(buffer);
    }
    
    public int write(ByteBuffer buffer) throws IOException {
        ensureOpen();
        if (nativeHandle != 0) {
            int n = FastIO.nativeWrite(nativeHandle, buffer, buffer.position(), buffer.remaining());
            if (n > 0) buffer.position(buffer.position() + n);
            return n;
        }
        return channel.write(buffer);
    }
    
    public void seek(long position) throws IOException {
        ensureOpen();
        if (nativeHandle != 0) {
            FastIO.nativeSeek(nativeHandle, position);
        } else {
            channel.position(position);
        }
    }
    
    public long size() throws IOException {
        ensureOpen();
        if (nativeHandle != 0) {
            return FastIO.nativeSize(nativeHandle);
        }
        return channel.size();
    }
    
    @Override
    public void close() throws IOException {
        if (!closed) {
            if (nativeHandle != 0) {
                FastIO.nativeClose(nativeHandle);
            } else {
                channel.close();
                raf.close();
            }
            closed = true;
        }
    }
    
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("File is closed: " + path);
        }
    }

    // Aligned buffer allocation
    public static ByteBuffer allocateAlignedBuffer(int size) {
        int alignedSize = ((size + 4095) / 4096) * 4096;
        return ByteBuffer.allocateDirect(alignedSize);
    }

    // Helper methods for single bytes/arrays
    public int read(byte[] bytes, int offset, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(length);
        int n = read(buffer);
        if (n > 0) {
            buffer.flip();
            buffer.get(bytes, offset, n);
        }
        return n;
    }

    public int write(byte[] bytes, int offset, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(length);
        buffer.put(bytes, offset, length);
        buffer.flip();
        return write(buffer);
    }
}
