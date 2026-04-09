package com.example.aiphotocompressor;

public class DocumentResponse {

    private Long id;
    private Long userId;
    private Long targetSizeKB;

    private String originalFileName;
    private String compressedFileName;
    private String compressedFilePath;

    private Double originalSize;
    private Double compressedSize;
    private Double savings;

    private String fileType;

    // ✅ NEW FIELD
    private String status;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTargetSizeKB() {
        return targetSizeKB;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getCompressedFileName() {
        return compressedFileName;
    }

    public String getCompressedFilePath() {
        return compressedFilePath;
    }

    public Double getOriginalSize() {
        return originalSize;
    }

    public Double getCompressedSize() {
        return compressedSize;
    }

    public Double getSavings() {
        return savings;
    }

    public String getFileType() {
        return fileType;
    }

    public String getStatus() {
        return status;
    }
}