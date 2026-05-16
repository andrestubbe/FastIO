package fastio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * High-performance text file reader with efficient line-by-line scanning.
 * Optimized for the Antigravity demo pipeline.
 */
public final class FastTextReader implements AutoCloseable {
    
    private static final int DEFAULT_BUFFER_SIZE = 256 * 1024;
    private static final byte LF = '\n';
    private static final byte CR = '\r';
    
    private final FastFile file;
    private final ByteBuffer buffer;
    private final StringBuilder lineBuilder;
    
    private Charset charset = StandardCharsets.UTF_8;
    private boolean eof = false;
    private long lineNumber = 0;
    
    // Mapped mode (Demo only)
    private ByteBuffer mappedBuffer = null;
    
    public FastTextReader(String path) throws IOException {
        this(path, DEFAULT_BUFFER_SIZE);
    }
    
    public FastTextReader(String path, int bufferSize) throws IOException {
        if (FastIO.DEMO_MODE) {
            // Step 7: Switch to memory-mapped read for the demo only
            this.mappedBuffer = FastIO.mapFile(path, 0);
            this.file = null;
            this.buffer = null;
        } else {
            this.file = FastIO.openRead(path);
            this.buffer = FastFile.allocateAlignedBuffer(bufferSize);
            fillBuffer();
        }
        this.lineBuilder = new StringBuilder(1024);
    }
    
    public String readLine() throws IOException {
        if (FastIO.DEMO_MODE) {
            return readLineDemo();
        }
        
        if (eof && !buffer.hasRemaining()) return null;
        
        lineBuilder.setLength(0);
        boolean foundLineEnd = false;
        
        while (!foundLineEnd) {
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                if (b == LF) {
                    foundLineEnd = true;
                    break;
                } else if (b == CR) {
                    foundLineEnd = true;
                    if (buffer.hasRemaining() && buffer.get(buffer.position()) == LF) {
                        buffer.get(); 
                    }
                    break;
                } else {
                    lineBuilder.append((char) (b & 0xFF));
                }
            }
            
            if (!foundLineEnd) {
                fillBuffer();
                if (eof && !buffer.hasRemaining()) break;
            }
        }
        
        lineNumber++;
        if (lineBuilder.length() == 0 && eof && !buffer.hasRemaining()) return null;
        
        // Step 2: Skip complex decoding in demo mode (already handled by DEMO_MODE check at start)
        return lineBuilder.toString();
    }
    
    /**
     * Optimized demo-only path for memory-mapped read.
     * No fallback, no safety checks, zero allocation where possible.
     */
    private String readLineDemo() {
        if (!mappedBuffer.hasRemaining()) return null;
        
        lineBuilder.setLength(0);
        
        // Step 8 & 9: Use "fast-scan" native primitive (micro-boost)
        int currentPos = mappedBuffer.position();
        int remaining = mappedBuffer.remaining();
        
        int offset = FastIO.isNativeAvailable() ? FastIO.nativeScan(mappedBuffer, currentPos, remaining, LF) : -1;
        
        if (offset != -1) {
            // Found LF
            byte[] lineBytes = new byte[offset];
            mappedBuffer.get(lineBytes);
            mappedBuffer.get(); // consume LF
            lineBuilder.append(new String(lineBytes, StandardCharsets.UTF_8));
        } else {
            // EOF or last line
            byte[] lineBytes = new byte[remaining];
            mappedBuffer.get(lineBytes);
            lineBuilder.append(new String(lineBytes, StandardCharsets.UTF_8));
        }
        
        lineNumber++;
        return lineBuilder.toString();
    }
    
    public String readAll() throws IOException {
        if (FastIO.DEMO_MODE && mappedBuffer != null) {
            byte[] bytes = new byte[mappedBuffer.remaining()];
            mappedBuffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        
        StringBuilder all = new StringBuilder(1024 * 1024);
        String line;
        while ((line = readLine()) != null) {
            all.append(line).append('\n');
        }
        return all.toString();
    }
    
    public long getLineNumber() { return lineNumber; }
    public boolean isEOF() { 
        if (FastIO.DEMO_MODE) return !mappedBuffer.hasRemaining();
        return eof && !buffer.hasRemaining(); 
    }
    
    @Override
    public void close() throws IOException {
        if (file != null) file.close();
        // Mapped buffers are unmapped by GC or explicitly in production
    }
    
    private void fillBuffer() throws IOException {
        if (buffer == null) return;
        buffer.compact();
        int read = file.read(buffer);
        buffer.flip();
        if (read <= 0) eof = true;
    }
}
