package io.github.andrestubbe.fastio;

/**
 * File metadata with format-specific information.
 */
public final class FastFileInfo {
    
    private final String path;
    private final long size;
    private final long creationTime;
    private final long lastModified;
    private final long lastAccessed;
    private final boolean isDirectory;
    private final boolean isFile;
    private final boolean isHidden;
    private final boolean isReadOnly;
    private final FileFormat detectedFormat;
    
    public FastFileInfo(String path, long size, long creationTime, 
                        long lastModified, long lastAccessed,
                        boolean isDirectory, boolean isFile, 
                        boolean isHidden, boolean isReadOnly,
                        FileFormat detectedFormat) {
        this.path = path;
        this.size = size;
        this.creationTime = creationTime;
        this.lastModified = lastModified;
        this.lastAccessed = lastAccessed;
        this.isDirectory = isDirectory;
        this.isFile = isFile;
        this.isHidden = isHidden;
        this.isReadOnly = isReadOnly;
        this.detectedFormat = detectedFormat;
    }
    
    public String getPath() { return path; }
    public long getSize() { return size; }
    public long getCreationTime() { return creationTime; }
    public long getLastModified() { return lastModified; }
    public long getLastAccessed() { return lastAccessed; }
    public boolean isDirectory() { return isDirectory; }
    public boolean isFile() { return isFile; }
    public boolean isHidden() { return isHidden; }
    public boolean isReadOnly() { return isReadOnly; }
    public FileFormat getDetectedFormat() { return detectedFormat; }
    
    @Override
    public String toString() {
        return String.format("FastFileInfo[path=%s, size=%d, format=%s, modified=%s]",
            path, size, detectedFormat, 
            java.time.Instant.ofEpochMilli(lastModified));
    }
}
