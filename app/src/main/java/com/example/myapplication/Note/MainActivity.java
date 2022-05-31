package com.example.myapplication.Note;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.myapplication.Account.LoginActivity;
import com.example.myapplication.Adapters.NotesAdapter;
import com.example.myapplication.Entities.Note;
import com.example.myapplication.Listeners.NotesListener;
import com.example.myapplication.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {
    // Declare and initialize constant variables
    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    private static final int REQUEST_CODE_UNDO_NOTE = 4;

    // Declare variables for views
    private RecyclerView rcNotes;
    private ImageView ivAddNewNote, ivChangeLayout, ivLogout, ivRecycleBin;
    private EditText etSearch, etNotePass;
    private AlertDialog dialogNotePass;


    // Others
    private NotesAdapter notesAdapter;
    private List<Note> noteList;
    private int noteClickedPosition = -1;
    private boolean flag = true;

    // Declare and initialize a Cloud Firestore instance
    FirebaseFirestore db = FirebaseFirestore.getInstance();

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

        // Logout button
        ivLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
        });

        // Recycle bin button
        ivRecycleBin.setOnClickListener(v -> {
            startActivityForResult(
                    new Intent(getApplicationContext(), RecycleBinActivity.class),
                    REQUEST_CODE_UNDO_NOTE
            );
        });

        rcNotes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && ivAddNewNote.isShown())
                    ivAddNewNote.setVisibility(View.GONE);
            };

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE)
                    ivAddNewNote.setVisibility(View.VISIBLE);
                super.onScrollStateChanged(recyclerView, newState);
            };
        });
    }

    private void initializeViews() {
        noteList = new ArrayList<>();
        ivAddNewNote = findViewById(R.id.iv_add_new_note);
        ivChangeLayout = findViewById(R.id.iv_change_layout);
        etSearch = findViewById(R.id.et_search);
        rcNotes = findViewById(R.id.rc_notes);
        notesAdapter = new NotesAdapter(noteList, this, this);
        ivLogout = findViewById(R.id.iv_logout);
        ivRecycleBin = findViewById(R.id.iv_recycle_bin);
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        if (note.getNotePass().isEmpty()){
            startActivityForResult(
                    new Intent(getApplicationContext(), EditorNoteActivity.class)
                            .putExtra("isViewOrUpdate", true)
                            .putExtra("note", note),
                    REQUEST_CODE_UPDATE_NOTE
            );
        }else if(dialogNotePass == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_dialog, (ViewGroup) findViewById(R.id.cl_note_password_container));
            builder.setView(view);
            dialogNotePass = builder.create();
            if (dialogNotePass.getWindow() != null) {
                dialogNotePass.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.tv_note_pass).setOnClickListener(v -> {
                etNotePass = view.findViewById(R.id.et_note_password);
                Log.d("sss", String.valueOf(etNotePass));
                if (etNotePass.getText().toString().trim().equals(note.getNotePass())){
                    dialogNotePass.dismiss();
                    startActivityForResult(
                            new Intent(getApplicationContext(), EditorNoteActivity.class)
                                    .putExtra("isViewOrUpdate", true)
                                    .putExtra("note", note),
                            REQUEST_CODE_UPDATE_NOTE
                    );
                }else{
                    Toast.makeText(this, "Access Denied!", Toast.LENGTH_SHORT).show();
                }
            });

            view.findViewById(R.id.tv_cancel_password).setOnClickListener(v -> {
                dialogNotePass.dismiss();
            });

            dialogNotePass.show();
        }
//        startActivityForResult(
//                new Intent(getApplicationContext(), EditorNoteActivity.class)
//                        .putExtra("isViewOrUpdate", true)
//                        .putExtra("note", note),
//                REQUEST_CODE_UPDATE_NOTE
//        );
    }

    @Override
    public void onPinClicked(Note note, int position) {
        if (note.isPin()) {
            // Case: note pinned
            // Update field pin on firestore
            updatePinNote(note);

            // Handling UI
            findViewById(R.id.iv_pin).setVisibility(View.VISIBLE);
            findViewById(R.id.iv_unpin).setVisibility(View.GONE);

            // Handling notes list
            getNotes(REQUEST_CODE_UNDO_NOTE, false);
        } else {
            // Case: note unpinned
            // Update field pin on firestore
            updatePinNote(note);

            // Handling UI
            findViewById(R.id.iv_pin).setVisibility(View.GONE);
            findViewById(R.id.iv_unpin).setVisibility(View.VISIBLE);

            // Handling notes list
            getNotes(REQUEST_CODE_UNDO_NOTE, false);
        }
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
        } else if (requestCode == REQUEST_CODE_UNDO_NOTE && resultCode == RESULT_OK) {
            if (data.getBooleanExtra("isNoteUndo", false)) {
                getNotes(REQUEST_CODE_UNDO_NOTE, false);
            }
        }
    }

    public void getNotes(final int requestCode, final boolean isNoteDeleted) {
        Query docRef = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("notes").orderBy("timestamp", Query.Direction.DESCENDING);
        docRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    // Create an empty notes list
                    List<Note> notes = new ArrayList<>();
                    List<Note> pinNotes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Note note = document.toObject(Note.class);
                        if (note.isPin()) {
                            pinNotes.add(note);
                            continue;
                        }
                        notes.add(note);
                    }
                    notes.addAll(0, pinNotes);
                    // UI handling
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
                            noteList.add(noteClickedPosition, notes.get(0));
                            notesAdapter.notifyItemChanged(noteClickedPosition);
                        }
                    } else if (requestCode == REQUEST_CODE_UNDO_NOTE) {
                        noteList.clear();
                        noteList.addAll(notes);
                        notesAdapter.notifyDataSetChanged();
                    }

                } else {
                    Log.d("TAG", "Error getting documents: ", task.getException());
                }
            }
        });
    };

    public void updatePinNote(Note note) {
        DocumentReference noteRef = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("notes").document(note.getNoteId());
        noteRef.update("pin", note.isPin())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("updatePinNote", "Update pin success!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("updatePinNote", "Update pin fail!");
                    }
                });
    }
}