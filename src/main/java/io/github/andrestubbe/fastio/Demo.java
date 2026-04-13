package io.github.andrestubbe.fastio;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * FastIO Demo - Shows practical usage examples
 * 
 * Run: mvn exec:java -Dexec.mainClass="io.github.andrestubbe.fastio.Demo"
 */
public class Demo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              FastIO Demo                                     ║");
        System.out.println("║     Ultra-fast file I/O for Java                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Initialize
        FastIO.init();
        System.out.println("✓ FastIO initialized");
        System.out.println();
        
        // Demo 1: Fast file copy
        demoFastCopy();
        
        // Demo 2: Memory-mapped file access
        demoMemoryMappedFile();
        
        // Demo 3: CSV processing
        demoCSVProcessing();
        
        // Demo 4: Text scanning
        demoTextScanning();
        
        // Demo 5: Binary I/O
        demoBinaryIO();
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           Demo Complete!                                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
    
    private static void demoFastCopy() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ Demo 1: Fast File Copy                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        // Create a test file
        String source = "demo_source.dat";
        String target = "demo_target.dat";
        
        FastFile file = FastIO.openWrite(source);
        ByteBuffer data = ByteBuffer.allocateDirect(1024 * 1024);
        for (int i = 0; i < 100; i++) { // 100MB file
            data.clear();
            for (int j = 0; j < data.capacity() / 4; j++) {
                data.putInt(j);
            }
            data.flip();
            file.write(data);
        }
        file.close();
        
        System.out.println("Created 100MB test file: " + source);
        
        // Fast copy
        long start = System.nanoTime();
        FastIO.fastCopy(source, target);
        long elapsed = System.nanoTime() - start;
        
        double mbps = 100.0 / (elapsed / 1_000_000_000.0);
        System.out.printf("Fast copy completed in %.2f ms (%.0f MB/s)%n", 
            elapsed / 1_000_000.0, mbps);
        
        // Cleanup
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(source));
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(target));
        System.out.println("✓ Cleanup complete");
        System.out.println();
    }
    
    private static void demoMemoryMappedFile() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ Demo 2: Memory-Mapped File Access                            │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        String filename = "demo_mapped.dat";
        int size = 10 * 1024 * 1024; // 10MB
        
        // Create file
        FastFile file = FastIO.openWrite(filename);
        ByteBuffer data = ByteBuffer.allocateDirect(1024);
        for (int i = 0; i < size / 1024; i++) {
            data.clear();
            data.putInt(i);
            data.flip();
            file.write(data);
        }
        file.close();
        
        // Memory map and read
        long start = System.nanoTime();
        ByteBuffer mapped = FastIO.mapFile(filename, size);
        
        // Access random locations (zero-copy)
        int sum = 0;
        for (int i = 0; i < size / 4; i += 1000) {
            sum += mapped.getInt(i * 4);
        }
        
        long elapsed = System.nanoTime() - start;
        System.out.printf("Mapped 10MB file and read %d ints in %.2f ms%n",
            size / 4 / 1000, elapsed / 1_000_000.0);
        System.out.println("Checksum: " + sum);
        
        // Cleanup
        System.gc();
        Thread.sleep(50);
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filename));
        } catch (Exception e) {
            // Ignore
        }
        System.out.println("✓ Cleanup complete");
        System.out.println();
    }
    
    private static void demoCSVProcessing() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ Demo 3: CSV Processing                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        String filename = "demo_data.csv";
        
        // Create sample CSV
        try (java.io.PrintWriter writer = new java.io.PrintWriter(filename)) {
            writer.println("id,product,quantity,price,category");
            String[] categories = {"Electronics", "Clothing", "Food", "Books"};
            String[] products = {"Laptop", "T-Shirt", "Apple", "Novel"};
            java.util.Random rand = new java.util.Random(42);
            
            for (int i = 1; i <= 100000; i++) {
                int catIdx = rand.nextInt(categories.length);
                writer.printf("%d,%s,%d,%.2f,%s%n",
                    i,
                    products[catIdx] + "_" + i,
                    rand.nextInt(100) + 1,
                    rand.nextDouble() * 1000,
                    categories[catIdx]);
            }
        }
        
        System.out.println("Created CSV with 100,000 rows");
        
        // Fast CSV read
        long start = System.nanoTime();
        FastCSVReader csv = new FastCSVReader(filename);
        csv.setHasHeader(true);
        
        double totalValue = 0;
        int electronicsCount = 0;
        int rows = 0;
        
        while (csv.nextRow()) {
            int quantity = csv.getInt(2);
            double price = csv.getDouble(3);
            totalValue += quantity * price;
            
            if ("Electronics".equals(csv.getString(4))) {
                electronicsCount++;
            }
            rows++;
        }
        csv.close();
        
        long elapsed = System.nanoTime() - start;
        
        System.out.printf("Processed %,d rows in %.2f ms%n", rows, elapsed / 1_000_000.0);
        System.out.printf("Total inventory value: $%,.2f%n", totalValue);
        System.out.printf("Electronics items: %,d%n", electronicsCount);
        System.out.printf("Speed: %,.0f rows/second%n", rows / (elapsed / 1_000_000_000.0));
        
        // Cleanup (with small delay to ensure file handles are released)
        System.gc();
        Thread.sleep(100);
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filename));
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        System.out.println("✓ Cleanup complete");
        System.out.println();
    }
    
    private static void demoTextScanning() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ Demo 4: Text File Scanning                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        String filename = "demo_log.txt";
        
        // Create sample log file
        try (java.io.PrintWriter writer = new java.io.PrintWriter(filename)) {
            String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};
            java.util.Random rand = new java.util.Random(42);
            
            for (int i = 0; i < 50000; i++) {
                String level = levels[rand.nextInt(levels.length)];
                writer.printf("[%s] %s - %s: operation=%s duration=%dms%n",
                    level,
                    java.time.LocalDateTime.now(),
                    "Component" + rand.nextInt(10),
                    "task_" + rand.nextInt(100),
                    rand.nextInt(500));
            }
        }
        
        System.out.println("Created log file with 50,000 lines");
        
        // Fast text scan
        long start = System.nanoTime();
        FastTextReader reader = new FastTextReader(filename);
        reader.setBufferSize(256 * 1024); // 256KB buffer
        
        int infoCount = 0;
        int warnCount = 0;
        int errorCount = 0;
        int lines = 0;
        
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("[INFO]")) infoCount++;
            else if (line.contains("[WARN]")) warnCount++;
            else if (line.contains("[ERROR]")) errorCount++;
            lines++;
        }
        reader.close();
        
        long elapsed = System.nanoTime() - start;
        
        System.out.printf("Scanned %,d lines in %.2f ms%n", lines, elapsed / 1_000_000.0);
        System.out.printf("INFO: %,d | WARN: %,d | ERROR: %,d%n", infoCount, warnCount, errorCount);
        System.out.printf("Speed: %,.0f lines/second%n", lines / (elapsed / 1_000_000_000.0));
        
        // Cleanup
        System.gc();
        Thread.sleep(50);
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filename));
        } catch (Exception e) {
            // Ignore
        }
        System.out.println("✓ Cleanup complete");
        System.out.println();
    }
    
    private static void demoBinaryIO() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ Demo 5: Binary I/O with Aligned Buffers                      │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        String filename = "demo_binary.dat";
        int recordCount = 1000000;
        int recordSize = 64; // Aligned to typical sector size
        
        // Write with aligned buffers (optimal for unbuffered I/O)
        long startWrite = System.nanoTime();
        FastFile file = FastIO.openWrite(filename);
        ByteBuffer buffer = FastFile.allocateAlignedBuffer(64 * 1024); // 64KB aligned
        
        for (int i = 0; i < recordCount; i++) {
            if (!buffer.hasRemaining()) {
                buffer.flip();
                file.write(buffer);
                buffer.clear();
            }
            buffer.putInt(i);
            buffer.putLong(System.currentTimeMillis());
            buffer.putDouble(Math.random());
            // Pad to record size
            int pad = recordSize - 4 - 8 - 8;
            for (int p = 0; p < pad; p++) buffer.put((byte) 0);
        }
        
        if (buffer.position() > 0) {
            buffer.flip();
            file.write(buffer);
        }
        file.close();
        
        long writeTime = System.nanoTime() - startWrite;
        double writeMB = (recordCount * recordSize) / (1024.0 * 1024.0);
        
        System.out.printf("Wrote %,d records (%.1f MB) in %.2f ms%n",
            recordCount, writeMB, writeTime / 1_000_000.0);
        System.out.printf("Write speed: %.0f MB/s%n",
            writeMB / (writeTime / 1_000_000_000.0));
        
        // Read back
        long startRead = System.nanoTime();
        FastFile readFile = FastIO.openRead(filename);
        ByteBuffer readBuffer = FastFile.allocateAlignedBuffer(64 * 1024);
        
        int recordsRead = 0;
        long checksum = 0;
        
        while (readFile.read(readBuffer) > 0) {
            readBuffer.flip();
            while (readBuffer.remaining() >= recordSize) {
                checksum += readBuffer.getInt();
                readBuffer.getLong();
                readBuffer.getDouble();
                int pad = recordSize - 4 - 8 - 8;
                for (int p = 0; p < pad; p++) readBuffer.get();
                recordsRead++;
            }
            readBuffer.compact();
        }
        readFile.close();
        
        long readTime = System.nanoTime() - startRead;
        
        System.out.printf("Read %,d records in %.2f ms%n",
            recordsRead, readTime / 1_000_000.0);
        System.out.printf("Read speed: %.0f MB/s%n",
            writeMB / (readTime / 1_000_000_000.0));
        System.out.println("Checksum: " + checksum);
        
        // Cleanup
        System.gc();
        Thread.sleep(50);
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filename));
        } catch (Exception e) {
            // Ignore
        }
        System.out.println("✓ Cleanup complete");
        System.out.println();
    }
}
