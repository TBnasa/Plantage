package com.example.orbitfocus.model;

public class Memory {
    public long id;
    public String text;
    public String imagePath; // Comma-separated paths for multiple images
    public String date;
    public float leafX, leafY;

    public Memory(long id, String text, String imagePath, String date) {
        this.id = id;
        this.text = text;
        this.imagePath = imagePath;
        this.date = date;
    }

    // Get first image for thumbnail
    public String getFirstImage() {
        if (imagePath == null || imagePath.isEmpty())
            return null;
        String[] paths = imagePath.split(",");
        return paths.length > 0 ? paths[0] : null;
    }

    // Get all images as array
    public String[] getAllImages() {
        if (imagePath == null || imagePath.isEmpty())
            return new String[0];
        return imagePath.split(",");
    }

    // Add new image path
    public String addImagePath(String newPath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return newPath;
        }
        return imagePath + "," + newPath;
    }
}
