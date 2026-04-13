package io.github.andrestubbe.fastio;

/**
 * Supported file formats for optimized I/O operations.
 */
public enum FileFormat {
    /** Unknown or binary format */
    UNKNOWN,
    
    /** Plain text (ASCII/UTF-8/UTF-16) */
    TEXT_PLAIN,
    
    /** Comma-separated values */
    CSV,
    
    /** JSON format */
    JSON,
    
    /** XML format */
    XML,
    
    /** Java properties file */
    PROPERTIES,
    
    /** ZIP archive */
    ZIP,
    
    /** GZIP compressed */
    GZIP,
    
    /** JPEG image */
    JPEG,
    
    /** PNG image */
    PNG,
    
    /** BMP image */
    BMP,
    
    /** TIFF image */
    TIFF,
    
    /** WAV audio */
    WAV,
    
    /** MP3 audio */
    MP3,
    
    /** MP4 video */
    MP4,
    
    /** AVI video */
    AVI,
    
    /** PDF document */
    PDF,
    
    /** Microsoft Word */
    DOCX,
    
    /** Microsoft Excel */
    XLSX,
    
    /** SQLite database */
    SQLITE,
    
    /** Java class file */
    CLASS,
    
    /** JAR archive */
    JAR,
    
    /** Executable */
    EXE,
    
    /** Dynamic-link library */
    DLL
}
