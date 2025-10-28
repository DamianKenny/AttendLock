package com.nibm.attendancetracker.common;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.admin.AddSubjectsToTeacherActivity;

import java.util.List;

public class SubjectsAdapter extends RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder> {

    private List<AddSubjectsToTeacherActivity.Subject> subjects;
    private List<AddSubjectsToTeacherActivity.Subject> selectedSubjects;
    private OnSubjectClickListener listener;

    public interface OnSubjectClickListener {
        void onSubjectClick(AddSubjectsToTeacherActivity.Subject subject);
    }

    public SubjectsAdapter(List<AddSubjectsToTeacherActivity.Subject> subjects, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        AddSubjectsToTeacherActivity.Subject subject = subjects.get(position);
        boolean isSelected = selectedSubjects != null && selectedSubjects.contains(subject);
        holder.bind(subject, isSelected, listener);
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    public void updateSelectedSubjects(List<AddSubjectsToTeacherActivity.Subject> selectedSubjects) {
        this.selectedSubjects = selectedSubjects;
        notifyDataSetChanged();
    }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        CardView cardSubject;
        ImageView ivSubjectIcon, ivCheckmark;
        TextView tvSubjectName, tvSubjectCategory;

        SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSubject = itemView.findViewById(R.id.card_subject);
            ivSubjectIcon = itemView.findViewById(R.id.iv_subject_icon);
            ivCheckmark = itemView.findViewById(R.id.iv_checkmark);
            tvSubjectName = itemView.findViewById(R.id.tv_subject_name);
            tvSubjectCategory = itemView.findViewById(R.id.tv_subject_category);
        }

        void bind(AddSubjectsToTeacherActivity.Subject subject, boolean isSelected, OnSubjectClickListener listener) {
            tvSubjectName.setText(subject.getName());
            tvSubjectCategory.setText(subject.getCategory());

            // Set card background color
            cardSubject.setCardBackgroundColor(Color.parseColor(subject.getColor()));

            // Show/hide checkmark
            ivCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Add selection effect
            cardSubject.setAlpha(isSelected ? 0.8f : 1.0f);

            cardSubject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubjectClick(subject);
                }
            });
        }
    }
}