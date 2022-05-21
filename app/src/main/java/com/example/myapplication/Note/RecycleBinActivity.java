package com.example.myapplication.Note;

import androidx.annotation.NonNull;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RecycleBinActivity extends AppCompatActivity implements NotesListener {
    // Declare and initialize constant variables
    public static final int REQUEST_CODE_UNDO_NOTE = 1;
    public static final int REQUEST_CODE_PERMANENTLY_DELETE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    public static final int REQUEST_CODE_DELETE_OUT_OF_DATE_NOTE = 4;
    public static final int ONE_DAY_TIMESTAMP = 60*60*24;

    // Declare variables for views
    private RecyclerView rcNotes;
    private ImageView ivChangeLayout, ivLogout, ivBack;
    private EditText etSearch;
    private AlertDialog dialogPermanentlyDeleteNote;

    // Others
    private NotesAdapter notesAdapter;
    private List<Note> noteList;
    private boolean flag = true;
    private String storageRootUrl = "https://storage.googleapis.com/todolist-auth-1b6a9.appspot.com/media/";

    // Declare and initialize a Cloud Firestore instance
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle_bin);

        initializeViews();

        manipulateViews();

        getRecycleBinNotes(REQUEST_CODE_SHOW_NOTES);
    }

    private void manipulateViews() {
        rcNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rcNotes.setAdapter(notesAdapter);

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

        // Back button
        ivBack.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void backToPreviousScreen(boolean isNoteUndo) {
        setResult(
                RESULT_OK,
                new Intent()
                        .putExtra("isNoteUndo", isNoteUndo)
        );
        finish();
    }

    private void initializeViews() {
        noteList = new ArrayList<>();
        ivChangeLayout = findViewById(R.id.iv_rb_change_layout);
        etSearch = findViewById(R.id.et_rb_search);
        rcNotes = findViewById(R.id.rc_rb_notes);
        notesAdapter = new NotesAdapter(noteList, this, this);
        ivLogout = findViewById(R.id.iv_rb_logout);
        ivBack = findViewById(R.id.iv_rb_back);
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        if (dialogPermanentlyDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(RecycleBinActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_permanently_delete_note, (ViewGroup) findViewById(R.id.cl_permanently_delete_note_container));
            builder.setView(view);
            dialogPermanentlyDeleteNote = builder.create();
            if (dialogPermanentlyDeleteNote.getWindow() != null) {
                dialogPermanentlyDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.tv_permanently_delete_note).setOnClickListener(v -> {
                // Delete note permanently
                deleteNoteFromRecycleBin(REQUEST_CODE_PERMANENTLY_DELETE_NOTE, note);
                dialogPermanentlyDeleteNote.dismiss();
            });

            view.findViewById(R.id.tv_undo_note).setOnClickListener(v -> {
                // Undo deleted note
                deleteNoteFromRecycleBin(REQUEST_CODE_UNDO_NOTE, note);
                dialogPermanentlyDeleteNote.dismiss();
            });

            view.findViewById(R.id.tv_permanently_cancel).setOnClickListener(v -> {
                dialogPermanentlyDeleteNote.dismiss();
            });
        }
        dialogPermanentlyDeleteNote.show();
    }

    private void deleteNoteFromRecycleBin(int requestCode, Note note) {
        db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("recycle bin").document(note.getNoteId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (requestCode == REQUEST_CODE_PERMANENTLY_DELETE_NOTE) {
                            // Case: permanently delete note
                            if (!note.getImagePath().isEmpty()) {
                                // Delete media in storage in case note contain media
                                String fileName = note.getImagePath().split(storageRootUrl)[1].replace("%20", " ");
                                deleteMediaInStorage(fileName);
                            }
                            backToPreviousScreen(false);
                        } else if (requestCode == REQUEST_CODE_UNDO_NOTE) {
                            // Case: undo note

                            // Reset delete timestamp
                            note.setDeleteTimestamp(0);

                            // Move note from recycle bin to note list
                            insertDeleteNoteToNoteListFirebase(note);
                            backToPreviousScreen(true);
                        } else if (requestCode == REQUEST_CODE_DELETE_OUT_OF_DATE_NOTE) {
                            // Case: delete out of date note
                            if (!note.getImagePath().isEmpty()) {
                                // Delete media in storage in case note contain media
                                String fileName = note.getImagePath().split(storageRootUrl)[1].replace("%20", " ");
                                deleteMediaInStorage(fileName);
                            }
                        }
                        Log.d("TAG", "DocumentSnapshot successfully undo!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG", "Error deleting document", e);
                    }
                });
    }

    private void insertDeleteNoteToNoteListFirebase(Note note) {
        DocumentReference ref = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("notes").document(note.getNoteId());
        ref
                .set(note)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Firestore", "Document add to recycle bin successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Document add failed: " + e);
                    }
                });
    }

    private void deleteMediaInStorage(String fileName) {
        StorageReference fileRef = storageRef.child("media/" + fileName);
        fileRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d("Storage Delete", "File delete successfully");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("Storage Delete", "Delete fail: " + e);
            }
        });
    }

    public void getRecycleBinNotes(final int requestCode) {
        Query docRef = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("recycle bin").orderBy("deleteTimestamp", Query.Direction.DESCENDING);
        docRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    // Create an empty notes list
                    List<Note> notes = new ArrayList<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Note note = document.toObject(Note.class);
                        if (new Timestamp(System.currentTimeMillis()).getTime() - note.getDeleteTimestamp() > ONE_DAY_TIMESTAMP) {
                            deleteNoteFromRecycleBin(REQUEST_CODE_DELETE_OUT_OF_DATE_NOTE, note);
                            continue;
                        }
                        notes.add(note);
                    }

                    // UI handling
                    if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                        // Case show notes: show all.
                        noteList.addAll(notes);
                        notesAdapter.notifyDataSetChanged();
                    }

                } else {
                    Log.d("TAG", "Error getting documents: ", task.getException());
                }
            }
        });
    };
}