package com.nibm.attendancetracker.teacher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.models.Teacher;

import java.util.ArrayList;
import java.util.List;

public class TeachersAdapter extends RecyclerView.Adapter<TeachersAdapter.TeacherViewHolder> {

    private List<Teacher> teachersList;
    private List<Teacher> filteredTeachersList;
    private final OnTeacherClickListener clickListener;
    private Context context;
    private static final String TAG = "TeachersAdapter";

    public interface OnTeacherClickListener {
        void onTeacherClick(Teacher teacher);
    }

    public TeachersAdapter(List<Teacher> teachersList, OnTeacherClickListener clickListener) {
        this.teachersList = new ArrayList<>(teachersList);
        this.filteredTeachersList = new ArrayList<>(teachersList);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_teacher, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        Teacher teacher = filteredTeachersList.get(position);

        if (teacher == null) {
            Log.e(TAG, "Teacher is null at position: " + position);
            return;
        }

        // Set teacher data
        holder.tvTeacherName.setText(teacher.getName() != null ? teacher.getName() : "No Name");
        holder.tvTeacherDept.setText(teacher.getDepartment() != null ? teacher.getDepartment() : "No Department");
        holder.tvTeacherEmail.setText(teacher.getEmail() != null ? teacher.getEmail() : "No Email");

        // Load profile picture
        String profileUrl = teacher.getProfilePictureUrl();
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Log.d(TAG, "Loading profile picture for " + teacher.getName() + ": " + profileUrl);
            Glide.with(context)
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_teacher)
                    .error(R.drawable.ic_teacher)
                    .circleCrop()
                    .into(holder.ivTeacherAvatar);
        } else {
            Log.w(TAG, "No profile picture URL for " + teacher.getName());
            holder.ivTeacherAvatar.setImageResource(R.drawable.ic_teacher);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTeacherClick(teacher);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredTeachersList != null ? filteredTeachersList.size() : 0;
    }

    public void filter(String query) {
        filteredTeachersList.clear();
        if (query == null || query.isEmpty()) {
            filteredTeachersList.addAll(teachersList);
        } else {
            String queryLower = query.toLowerCase();
            for (Teacher teacher : teachersList) {
                if ((teacher.getName() != null && teacher.getName().toLowerCase().contains(queryLower)) ||
                        (teacher.getDepartment() != null && teacher.getDepartment().toLowerCase().contains(queryLower))) {
                    filteredTeachersList.add(teacher);
                }
            }
        }
        notifyDataSetChanged();
        Log.d(TAG, "Filter completed. Results: " + filteredTeachersList.size());
    }

    // Add this method to update the list
    public void updateTeachersList(List<Teacher> newTeachers) {
        this.teachersList.clear();
        this.teachersList.addAll(newTeachers);
        this.filteredTeachersList.clear();
        this.filteredTeachersList.addAll(newTeachers);
        notifyDataSetChanged();
        Log.d(TAG, "Teachers list updated. Total teachers: " + teachersList.size());
    }

    static class TeacherViewHolder extends RecyclerView.ViewHolder {
        TextView tvTeacherName, tvTeacherDept, tvTeacherEmail;
        ImageView ivTeacherAvatar;

        TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeacherName = itemView.findViewById(R.id.tv_teacher_name);
            tvTeacherDept = itemView.findViewById(R.id.tv_teacher_dept);
            tvTeacherEmail = itemView.findViewById(R.id.tv_teacher_email);
            ivTeacherAvatar = itemView.findViewById(R.id.iv_teacher_avatar);

            // Add null checks
            if (tvTeacherName == null || tvTeacherDept == null || tvTeacherEmail == null || ivTeacherAvatar == null) {
                Log.e("TeachersAdapter", "One or more views are null in ViewHolder");
            }
        }
    }
}