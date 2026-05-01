package com.tbnasa.plantage.model;

/**
 * Garden (Bahçe) model — a category for memories.
 */
public class Garden {
    public long id;
    public String name;
    public String icon;
    public long createdAt;

    public Garden(long id, String name, String icon, long createdAt) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.createdAt = createdAt;
    }

    public Garden(long id, String name, String icon) {
        this(id, name, icon, System.currentTimeMillis());
    }
}
