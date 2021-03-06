package com.example.myapplication.Entities;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Note implements Serializable {
    public Note() {
        this.isPin = false;
    }

    public Note(String title, String dateTime, String noteText, String imagePath, String musicPath) {
        this.title = title;
        this.dateTime = dateTime;
        this.noteText = noteText;
        this.imagePath = imagePath;
        this.musicPath = musicPath;
        this.isPin = false;
    }

    private String id;

    private String title;

    private String dateTime;

    private long timestamp;

    private long deleteTimestamp;

    private String noteText;

    private String imagePath;

    private String musicPath;

    private boolean isPin;

    private String notePass;

    public String getNotePass() {
        return notePass;
    }

    public void setNotePass(String notePass) {
        this.notePass = notePass;
    }

    public boolean isPin() {
        return isPin;
    }

    public void setPin(boolean pin) {
        isPin = pin;
    }

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

    public long getDeleteTimestamp() {
        return deleteTimestamp;
    }

    public void setDeleteTimestamp(long deleteTimestamp) {
        this.deleteTimestamp = deleteTimestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return title + " : " + dateTime;
    }
}
