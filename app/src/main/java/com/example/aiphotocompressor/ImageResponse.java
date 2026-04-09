package com.example.aiphotocompressor;

public class ImageResponse {

    private Long id;
    private String imageName;
    private Double originalSize;
    private Double compressedSize;
    private Double savings;
    private String score;
    private String outputFormat;
    private String compressedImagePath;
    private String originalImagePath;
    private Long userId;
    private Long targetSizeKB;
    private String compressionMode;

    public Long getId() {
        return id;
    }

    public String getImageName() {
        return imageName;
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

    public String getScore() {
        return score;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getCompressedImagePath() {
        return compressedImagePath;
    }

    public String getOriginalImagePath() {
        return originalImagePath;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTargetSizeKB() {
        return targetSizeKB;
    }

    public String getCompressionMode() {
        return compressionMode;
    }
}