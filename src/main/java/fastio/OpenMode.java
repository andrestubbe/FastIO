package fastio;

/**
 * File open modes for FastFile operations.
 */
public enum OpenMode {
    /** Read-only access */
    READ,
    
    /** Write-only access, creates or truncates */
    WRITE,
    
    /** Read and write access */
    READ_WRITE,
    
    /** Append mode, writes always at end */
    APPEND
}
