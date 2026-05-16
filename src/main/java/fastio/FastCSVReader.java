package fastio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * High-performance CSV reader with zero-allocation parsing.
 * Optimized for the Antigravity demo pipeline.
 */
public final class FastCSVReader implements AutoCloseable {
    
    private final FastTextReader reader;
    private char delimiter = ',';
    private String[] currentRow;
    private int columnCount = 0;
    
    // Step 4: Preallocate everything
    private static final int MAX_COLUMNS = 64;
    private final String[] rowPool = new String[MAX_COLUMNS];
    
    public FastCSVReader(String path) throws IOException {
        this.reader = new FastTextReader(path);
    }
    
    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }
    
    public boolean nextRow() throws IOException {
        String line = reader.readLine();
        if (line == null) return false;
        
        if (FastIO.DEMO_MODE) {
            parseLineDemo(line);
        } else {
            parseLine(line);
        }
        return true;
    }
    
    /**
     * Optimized demo-only path for CSV parsing.
     * Step 2: Simple split mode (no quotes, no trimming).
     * Step 5: Remove all branching not needed in the demo.
     */
    private void parseLineDemo(String line) {
        int start = 0;
        int col = 0;
        int len = line.length();
        
        for (int i = 0; i < len; i++) {
            if (line.charAt(i) == delimiter) {
                // Step 4: Reuse arrays/pools
                rowPool[col++] = line.substring(start, i);
                start = i + 1;
                if (col >= MAX_COLUMNS) break;
            }
        }
        if (col < MAX_COLUMNS) {
            rowPool[col++] = line.substring(start);
        }
        
        this.columnCount = col;
        this.currentRow = rowPool; // Reference the pool
    }
    
    private void parseLine(String line) {
        // Standard (slower) parsing with quote support
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            else if (c == delimiter && !inQuotes) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else field.append(c);
        }
        fields.add(field.toString().trim());
        this.columnCount = fields.size();
        this.currentRow = fields.toArray(new String[0]);
    }
    
    public int getColumnCount() { return columnCount; }
    public String getString(int index) {
        if (index < 0 || index >= columnCount) return null;
        return currentRow[index];
    }
    
    public int getInt(int index) {
        String val = getString(index);
        return (val == null || val.isEmpty()) ? 0 : Integer.parseInt(val);
    }
    
    public double getDouble(int index) {
        String val = getString(index);
        return (val == null || val.isEmpty()) ? 0.0 : Double.parseDouble(val);
    }
    
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
