package com.example.myapplication.DAO;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.Entities.Note;

import java.util.List;

@Dao
public interface NoteDAO {

    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<Note> getAllNotes();

    // onConflictStrategy.REPLACE will override new note if the id is already in the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Delete
    void deleteNote(Note note);
}
