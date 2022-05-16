package com.example.myapplication.Entities;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Note implements Serializable {
    public Note() {
    }

    public Note(String title, String dateTime, String subtitle, String noteText, String imagePath, String color, String musicPath) {
        this.title = title;
        this.dateTime = dateTime;
        this.subtitle = subtitle;
        this.noteText = noteText;
        this.imagePath = imagePath;
        this.color = color;
        this.musicPath = musicPath;
    }

    private String id;

    private String title;

    private String dateTime;

    private long timestamp;

    private String subtitle;

    private String noteText;

    private String imagePath;

    private String color;

    private String musicPath;

    public String getNoteId() {
        return id;
    }

    public void setNoteId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return title + " : " + dateTime;
    }
}
