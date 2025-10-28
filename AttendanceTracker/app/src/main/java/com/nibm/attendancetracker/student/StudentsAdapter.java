package com.nibm.attendancetracker.student;

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
import com.nibm.attendancetracker.models.Student; // ✅ Use the proper Student model

import java.util.List;

public class StudentsAdapter extends RecyclerView.Adapter<StudentsAdapter.StudentViewHolder> {

    private static final String TAG = "StudentsAdapter";
    private final List<Student> studentsList;
    private final OnStudentClickListener clickListener;
    private Context context;

    public interface OnStudentClickListener {
        void onStudentClick(Student student);
    }

    public StudentsAdapter(List<Student> studentsList, OnStudentClickListener clickListener) {
        this.studentsList = studentsList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentsList.get(position);

        holder.tvStudentName.setText(student.getName());
        holder.tvStudentId.setText(student.getStudentId());
        holder.tvStudentProgramme.setText(student.getProgramme());
        holder.tvStudentBatch.setText(student.getBatch());

        String profileUrl = student.getProfilePictureUrl();

        if (profileUrl != null && !profileUrl.isEmpty()) {
            Log.d(TAG, "Loading profile picture for " + student.getName() + ": " + profileUrl);

            Glide.with(context)
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_student)
                    .error(R.drawable.ic_student)
                    .circleCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(
                                GlideException e,
                                Object model,
                                Target<Drawable> target,
                                boolean isFirstResource
                        ) {
                            Log.e(TAG, "Failed to load profile picture for " + student.getName() +
                                    ": " + (e != null ? e.getMessage() : "Unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(
                                Drawable resource,
                                Object model,
                                Target<Drawable> target,
                                DataSource dataSource,
                                boolean isFirstResource
                        ) {
                            Log.d(TAG, "Successfully loaded profile picture for " + student.getName());
                            return false;
                        }
                    })
                    .into(holder.ivStudentAvatar);
        } else {
            Log.w(TAG, "No profile picture URL for " + student.getName());
            holder.ivStudentAvatar.setImageResource(R.drawable.ic_student);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onStudentClick(student));
    }

    @Override
    public int getItemCount() {
        return studentsList.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentId, tvStudentProgramme, tvStudentBatch;
        ImageView ivStudentAvatar;

        StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvStudentId = itemView.findViewById(R.id.tv_student_id);
            tvStudentProgramme = itemView.findViewById(R.id.tv_student_programme);
            tvStudentBatch = itemView.findViewById(R.id.tv_student_batch);
            ivStudentAvatar = itemView.findViewById(R.id.iv_student_avatar);
        }
    }
}
