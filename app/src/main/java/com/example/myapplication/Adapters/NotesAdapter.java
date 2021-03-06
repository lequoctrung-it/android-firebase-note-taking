package com.example.myapplication.Adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Entities.Note;
import com.example.myapplication.Listeners.NotesListener;
import com.example.myapplication.R;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder>{

    private List<Note> notes;
    private NotesListener notesListener;
    private Timer timer;
    private List<Note> notesSource;
    private Context context;

    public NotesAdapter(List<Note> notes, NotesListener notesListener, Context context) {
        this.notes = notes;
        this.notesListener = notesListener;
        notesSource = notes;
        this.context = context;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_note, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(notes.get(position), context);
        holder.llTitleContainer.setOnClickListener(v -> {
            notesListener.onNoteClicked(notes.get(position), position);
        });
        holder.rivNote.setOnClickListener(v -> {
            notesListener.onNoteClicked(notes.get(position), position);
        });
        holder.ivPin.setOnClickListener(v -> {
            notes.get(position).setPin(false);
            notesListener.onPinClicked(notes.get(position), position);
        });
        holder.ivUnpin.setOnClickListener(v -> {
            notes.get(position).setPin(true);
            notesListener.onPinClicked(notes.get(position), position);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDateTime;
        LinearLayout llNote, llTitleContainer;
        RoundedImageView rivNote;
        ImageView ivPin, ivUnpin;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            llNote = itemView.findViewById(R.id.ll_note);
            rivNote = itemView.findViewById(R.id.riv_note);
            ivPin = itemView.findViewById(R.id.iv_pin);
            ivUnpin = itemView.findViewById(R.id.iv_unpin);
            llTitleContainer = itemView.findViewById(R.id.ll_title_container);
        }

        void setNote(Note note, Context context) {
            // Display note title
            tvTitle.setText(note.getTitle());

            // Display date time
            tvDateTime.setText(note.getDateTime());

            // Set layout to display note with/without media
            if (note.getImagePath() != null) {
                if (isVideoFile(note.getImagePath())) {
                    // Create an thumbnail for video
//                    Bitmap bMap = ThumbnailUtils.createVideoThumbnail(note.getImagePath(), MediaStore.Video.Thumbnails.MINI_KIND);
//                    rivNote.setImageBitmap(bMap);
                }else {
                    Glide.with(context)
                            .load(note.getImagePath())
                            .into(rivNote);
                }
                rivNote.setVisibility(View.VISIBLE);
            }else {
                rivNote.setVisibility(View.GONE);
            }

            // Visible icon pin/unpin
            if (note.isPin()) {
                ivPin.setVisibility(View.VISIBLE);
                ivUnpin.setVisibility(View.GONE);
            } else {
                ivPin.setVisibility(View.GONE);
                ivUnpin.setVisibility(View.VISIBLE);
            }
        }
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

    public void searchNotes(final String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty()) {
                    notes = notesSource;
                } else {
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note : notesSource) {
                        if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            temp.add(note);
                        }
                    }
                    notes = temp;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }, 500);
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
