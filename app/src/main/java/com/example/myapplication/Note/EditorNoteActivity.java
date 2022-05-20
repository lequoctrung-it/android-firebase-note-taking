package com.example.myapplication.Note;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Entities.Note;
import com.example.myapplication.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditorNoteActivity extends AppCompatActivity {
    // Declare and initialize constant variables
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    public static final int REQUEST_CODE_SELECT_IMAGE = 2;
    public static final int REQUEST_CODE_UPDATE_NOTE = 3;
    public static final int REQUEST_CODE_ADD_NOTE = 4;

    // Declare variables for views
    private EditText etNoteTitle, etNoteSubtitle, etNoteContent;
    private TextView tvDateTime;
    private View viewSubtitleIndicator;
    private ImageView ivNote, ivBack, ivSave;
    private VideoView vvNote;

    private AlertDialog dialogDeleteNote;

    // Others
    private String selectedNoteColor;
    private String selectedImagePath;
    private Note currentNote;
    private Dialog dialogLoading;


    // Declare and initialize a Cloud Firestore instance
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_note);

        initializeViews();

        manipulateViews();

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            currentNote = (Note) getIntent().getSerializableExtra("note");
            setViewForCurrentNote();
        }

        initializeModal();
        setSubtitleIndicatorColor();
    }

    private void initializeViews() {
        etNoteTitle = findViewById(R.id.et_input_note_title);
        etNoteSubtitle = findViewById(R.id.et_input_note_subtitle);
        etNoteContent = findViewById(R.id.et_note);
        tvDateTime = findViewById(R.id.tv_date_time);
        viewSubtitleIndicator = findViewById(R.id.v_subtitle_indicator);
        ivNote = findViewById(R.id.riv_note);
        vvNote = findViewById(R.id.videoView);
        ivBack = findViewById(R.id.iv_back);
        ivSave = findViewById(R.id.iv_save);

        tvDateTime.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date()));
        selectedNoteColor = "#333333";
        selectedImagePath = "";
    }

    private void manipulateViews() {
        // Back button onclick
        ivBack.setOnClickListener(v -> {
            onBackPressed();
        });

        // Save button onclick
        ivSave.setOnClickListener(v -> {
            saveNote();
        });

        // Remove image onclick
        findViewById(R.id.iv_remove_image).setOnClickListener(v -> {
            ivNote.setImageBitmap(null);
            ivNote.setVisibility(View.GONE);
            findViewById(R.id.iv_remove_image).setVisibility(View.GONE);
            selectedImagePath = "";
        });

        // Remove video onclick
        findViewById(R.id.iv_remove_video).setOnClickListener(v -> {
            vvNote.setVideoURI(null);
            vvNote.setVisibility(View.GONE);
            findViewById(R.id.iv_remove_video).setVisibility(View.GONE);
            selectedImagePath = "";
        });
    }

    private void setViewForCurrentNote() {
        etNoteTitle.setText(currentNote.getTitle());
        etNoteSubtitle.setText(currentNote.getSubtitle());
        etNoteContent.setText(currentNote.getNoteText());
        tvDateTime.setText(currentNote.getDateTime());

        if (currentNote.getImagePath() != null && !currentNote.getImagePath().trim().isEmpty()) {
            // Check the media is video or image
            if (isVideoFile(currentNote.getImagePath())) {
                // Case: video
                MediaController mc = new MediaController(this);
                vvNote.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mc.setAnchorView(vvNote);
                        vvNote.start();
                    }
                });
                vvNote.setMediaController(mc);
                vvNote.setVideoURI(Uri.parse(currentNote.getImagePath()));

                vvNote.setVisibility(View.VISIBLE);
                ivNote.setVisibility(View.GONE);
                findViewById(R.id.iv_remove_image).setVisibility(View.GONE);
                findViewById(R.id.iv_remove_video).setVisibility(View.VISIBLE);
            }else {
                // Case: image
                Glide.with(getApplicationContext())
                        .load(currentNote.getImagePath())
                        .into(ivNote);

                ivNote.setVisibility(View.VISIBLE);
                vvNote.setVisibility(View.GONE);
                findViewById(R.id.iv_remove_image).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_remove_video).setVisibility(View.GONE);
            }
            selectedImagePath = currentNote.getImagePath();
        }
    }

    private void saveNote() {
        // Validate input
        if (etNoteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (etNoteSubtitle.getText().toString().trim().isEmpty() && etNoteContent.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create object note to store data
        final Note note = new Note();
        note.setTitle(etNoteTitle.getText().toString());
        note.setSubtitle(etNoteSubtitle.getText().toString());
        note.setNoteText(etNoteContent.getText().toString());
        note.setDateTime(tvDateTime.getText().toString());
        note.setTimestamp(new Timestamp(System.currentTimeMillis()).getTime());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        // Update note by set the ID to override note
        if (currentNote != null) {
            note.setNoteId(currentNote.getNoteId());
        }

        // Check update or add
        if (currentNote != null) {
            // Case: update note
            // Upload media to storage
            if (!note.getImagePath().isEmpty()) {
                uploadMediaToStorage(REQUEST_CODE_UPDATE_NOTE, note);
            } else {
                updateNoteToFirestore(note);
            }
        } else {
            // Case: add note
            // Upload media to storage
            if (!note.getImagePath().isEmpty()) {
                uploadMediaToStorage(REQUEST_CODE_ADD_NOTE, note);
            } else {
                addNoteToFirestore(note);
            }
        }
    }

    private void uploadMediaToStorage(int requestCode, Note note) {
        Uri file = Uri.fromFile(new File(note.getImagePath()));
        StorageReference mediaRef = storageRef.child("media/" + file.getLastPathSegment());
        UploadTask uploadTask = mediaRef.putFile(file);

        uploadTask
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Dismiss loading dialog
                        dialogLoading.dismiss();

                        // Store public image url to note
                        note.setImagePath("https://storage.googleapis.com/todolist-auth-1b6a9.appspot.com/" + "media/" + file.getLastPathSegment().replace(" ", "%20"));
                        if (requestCode == REQUEST_CODE_ADD_NOTE) {
                            addNoteToFirestore(note);
                        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                            updateNoteToFirestore(note);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Upload media fail", String.valueOf(e));
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        // Loading progress bar goes here
                        showLoadingDialog();
                    }
                });
    }

    private void updateNoteToFirestore(Note note) {
        DocumentReference ref = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("notes").document(note.getNoteId());
        ref
                .set(note)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setResult(RESULT_OK, new Intent());
                        finish();
                        Log.d("Firestore", "Document update successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Document update failed: " + e);
                    }
                });
    }

    private void addNoteToFirestore(Note note) {
        DocumentReference ref = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("notes").document();
        note.setNoteId(ref.getId());
        ref
                .set(note)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setResult(RESULT_OK, new Intent());
                        finish();
                        Log.d("Firestore", "Document add successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Document add failed: " + e);
                    }
                });
    }

    private void initializeModal() {
        // On click to open/close modal
        final LinearLayout llModal = findViewById(R.id.ll_modal);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(llModal);
        llModal.findViewById(R.id.tv_modal).setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        final ImageView ivColor1 = llModal.findViewById(R.id.iv_color_1);
        final ImageView ivColor2 = llModal.findViewById(R.id.iv_color_2);
        final ImageView ivColor3 = llModal.findViewById(R.id.iv_color_3);
        final ImageView ivColor4 = llModal.findViewById(R.id.iv_color_4);
        final ImageView ivColor5 = llModal.findViewById(R.id.iv_color_5);

        // Handle color picker
        llModal.findViewById(R.id.v_color_1).setOnClickListener(v -> {
            selectedNoteColor = "#333333";
            ivColor1.setImageResource(R.drawable.ic_done);
            ivColor2.setImageResource(0);
            ivColor3.setImageResource(0);
            ivColor4.setImageResource(0);
            ivColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        llModal.findViewById(R.id.v_color_2).setOnClickListener(v -> {
            selectedNoteColor = "#FDBE3B";
            ivColor1.setImageResource(0);
            ivColor2.setImageResource(R.drawable.ic_done);
            ivColor3.setImageResource(0);
            ivColor4.setImageResource(0);
            ivColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        llModal.findViewById(R.id.v_color_3).setOnClickListener(v -> {
            selectedNoteColor = "#FF4842";
            ivColor1.setImageResource(0);
            ivColor2.setImageResource(0);
            ivColor3.setImageResource(R.drawable.ic_done);
            ivColor4.setImageResource(0);
            ivColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        llModal.findViewById(R.id.v_color_4).setOnClickListener(v -> {
            selectedNoteColor = "#3A52Fc";
            ivColor1.setImageResource(0);
            ivColor2.setImageResource(0);
            ivColor3.setImageResource(0);
            ivColor4.setImageResource(R.drawable.ic_done);
            ivColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        llModal.findViewById(R.id.v_color_5).setOnClickListener(v -> {
            selectedNoteColor = "#000000";
            ivColor1.setImageResource(0);
            ivColor2.setImageResource(0);
            ivColor3.setImageResource(0);
            ivColor4.setImageResource(0);
            ivColor5.setImageResource(R.drawable.ic_done);
            setSubtitleIndicatorColor();
        });

        // Set view for note color in case updating or view note
        if (currentNote != null && currentNote.getColor() != null && !currentNote.getColor().trim().isEmpty()) {
            switch (currentNote.getColor()) {
                case "#FDBE3B":
                    llModal.findViewById(R.id.v_color_2).performClick();
                    break;
                case "#FF4842":
                    llModal.findViewById(R.id.v_color_3).performClick();
                    break;
                case "#3A52Fc":
                    llModal.findViewById(R.id.v_color_4).performClick();
                    break;
                case "#000000":
                    llModal.findViewById(R.id.v_color_5).performClick();
                    break;
            }
        }

        // Add image on click
        llModal.findViewById(R.id.ll_add_image).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            // Check Read external storage permission and request permission
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        EditorNoteActivity.this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION
                );
            }else {
                selectImage();
            }
        });

        // Handle delete button in case current note is available
        if (currentNote != null) {
            llModal.findViewById(R.id.ll_delete_note).setVisibility(View.VISIBLE);
            llModal.findViewById(R.id.ll_delete_note).setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        }
    }

    private void showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(EditorNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, (ViewGroup) findViewById(R.id.cl_delete_note_container));
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.tv_delete_note).setOnClickListener(v -> {
                @SuppressLint("StaticFieldLeak")
                class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        // Delete note on firestore
                        db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("notes").document(currentNote.getNoteId())
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        currentNote.setDeleteTimestamp(new Timestamp(System.currentTimeMillis()).getTime());
                                        insertDeleteNoteToRecycleBinFirebase(currentNote);
                                        Log.d("TAG", "DocumentSnapshot successfully deleted!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("TAG", "Error deleting document", e);
                                    }
                                });
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        super.onPostExecute(unused);
                        dialogDeleteNote.dismiss();
                        setResult(
                                RESULT_OK,
                                new Intent()
                                        .putExtra("isNoteDeleted", true)
                        );
                        finish();
                    }
                }

                new DeleteNoteTask().execute();
            });

            view.findViewById(R.id.tv_cancel).setOnClickListener(v -> {
                dialogDeleteNote.dismiss();
            });
        }
        dialogDeleteNote.show();
    }

    private void insertDeleteNoteToRecycleBinFirebase(Note note) {
        DocumentReference ref = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("recycle bin").document(note.getNoteId());
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

    private void showLoadingDialog() {
        if (dialogLoading == null) {
            dialogLoading = new Dialog(EditorNoteActivity.this);
            dialogLoading.setContentView(R.layout.layout_loading_dialog);
            dialogLoading.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialogLoading.create();
            dialogLoading.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // After select media handle
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectMediaUri = data.getData();
                if (selectMediaUri != null) {
                    try {
                        // Check if media is video or image
                        if (isVideoFile(getPathFromUri(selectMediaUri))) {
                            // Case: video
                            MediaController mc = new MediaController(this);
                            vvNote.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mediaPlayer) {
                                    mc.setAnchorView(vvNote);
                                    vvNote.start();
                                }
                            });
                            vvNote.setMediaController(mc);
                            vvNote.setVideoURI(selectMediaUri);

                            vvNote.setVisibility(View.VISIBLE);
                            ivNote.setVisibility(View.GONE);
                            findViewById(R.id.iv_remove_image).setVisibility(View.GONE);
                            findViewById(R.id.iv_remove_video).setVisibility(View.VISIBLE);
                        }else {
                            // Case: image
                            InputStream inputStream = getContentResolver().openInputStream(selectMediaUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            ivNote.setImageBitmap(bitmap);

                            ivNote.setVisibility(View.VISIBLE);
                            vvNote.setVisibility(View.GONE);
                            findViewById(R.id.iv_remove_image).setVisibility(View.VISIBLE);
                            findViewById(R.id.iv_remove_video).setVisibility(View.GONE);
                        }
                        selectedImagePath = getPathFromUri(selectMediaUri);
                    }catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        }else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    public static boolean isVideoFile(String url) {
        String ext = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "mp4":
            case "mkv":
            case "mov":
            case "mpg":
            case "webm":
            case "avi":
            case "m4v":
                return true;
            default:
                return false;
        }
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    public void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/* video/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }
}