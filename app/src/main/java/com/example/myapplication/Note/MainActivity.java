package com.example.myapplication.Note;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.myapplication.Adapters.NotesAdapter;
import com.example.myapplication.Database.NotesDatabase;
import com.example.myapplication.Entities.Note;
import com.example.myapplication.Listeners.NotesListener;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {
    // Declare and initialize constant variables
    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;

    // Declare variables for views
    private RecyclerView rcNotes;
    private ImageView ivAddNewNote, ivChangeLayout;
    private EditText etSearch;

    // Others
    private NotesAdapter notesAdapter;
    private List<Note> noteList;
    private int noteClickedPosition = -1;
    private boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();

        manipulateViews();

        getNotes(REQUEST_CODE_SHOW_NOTES, false);
    }

    private void manipulateViews() {
        rcNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rcNotes.setAdapter(notesAdapter);

        // FAB add new note
        ivAddNewNote.setOnClickListener(v -> {
            startActivityForResult(new Intent(getApplicationContext(), EditorNoteActivity.class), REQUEST_CODE_ADD_NOTE);
        });

        // Switch layout button
        ivChangeLayout.setOnClickListener(v -> {
            if (flag == true) {
                rcNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
                ivChangeLayout.setImageResource(R.drawable.ic_list);
            } else {
                rcNotes.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
                ivChangeLayout.setImageResource( R.drawable.ic_grid);
            }
            flag = !flag;
        });

        // Search bar
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNotes(editable.toString());
                }
            }
        });
    }

    private void initializeViews() {
        noteList = new ArrayList<>();
        ivAddNewNote = findViewById(R.id.iv_add_new_note);
        ivChangeLayout = findViewById(R.id.iv_change_layout);
        etSearch = findViewById(R.id.et_search);
        rcNotes = findViewById(R.id.rc_notes);
        notesAdapter = new NotesAdapter(noteList, this);
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        startActivityForResult(
                new Intent(getApplicationContext(), EditorNoteActivity.class)
                        .putExtra("isViewOrUpdate", true)
                        .putExtra("note", note),
                REQUEST_CODE_UPDATE_NOTE
        );
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted) {
        // Just like addNote we have to use AsyncTask to getNote
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {

            @Override
            protected List<Note> doInBackground(Void... voids) {
                //getAllNotes query
                return NotesDatabase.getDatabase(getApplicationContext()).noteDAO().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                // Render UI
                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    // Case show notes: show all.
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    // Case add note: just insert note to position 0 and scroll to top
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    rcNotes.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    // Case update note: remove the old note and add new note
                    noteList.remove(noteClickedPosition);
                    if (isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }
            }
        }

        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            // Render list notes right after add action successful
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        }
    }
}