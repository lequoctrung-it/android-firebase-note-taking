package com.example.myapplication.Listeners;

import com.example.myapplication.Entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
    void onPinClicked(Note note, int position);
}
