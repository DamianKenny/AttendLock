package com.nibm.attendancetracker.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.admin.AddScheduleToStudentActivity;

import java.util.ArrayList;
import java.util.List;

public class StudentCheckboxAdapter extends RecyclerView.Adapter<StudentCheckboxAdapter.ViewHolder> {
    private List<AddScheduleToStudentActivity.StudentItem> allStudents;
    private List<AddScheduleToStudentActivity.StudentItem> filteredStudents;
    private Runnable onSelectionChanged;

    public StudentCheckboxAdapter(List<AddScheduleToStudentActivity.StudentItem> students, Runnable onSelectionChanged) {
        this.allStudents = students;
        this.filteredStudents = new ArrayList<>(students);
        this.onSelectionChanged = onSelectionChanged;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AddScheduleToStudentActivity.StudentItem student = filteredStudents.get(position);

        holder.tvName.setText(student.name);
        holder.tvStudentId.setText(student.studentId);
        holder.checkbox.setChecked(student.isSelected);

        if (student.profileUrl != null && !student.profileUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(student.profileUrl)
                    .placeholder(R.drawable.ic_student)
                    .error(R.drawable.ic_student)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_student);
        }

        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            student.isSelected = isChecked;
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            holder.checkbox.setChecked(!holder.checkbox.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return filteredStudents.size();
    }

    public void filter(String query) {
        filteredStudents.clear();
        if (query.isEmpty()) {
            filteredStudents.addAll(allStudents);
        } else {
            String lowerQuery = query.toLowerCase();
            for (AddScheduleToStudentActivity.StudentItem student : allStudents) {
                if (student.name.toLowerCase().contains(lowerQuery) ||
                        student.studentId.toLowerCase().contains(lowerQuery)) {
                    filteredStudents.add(student);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void selectAll(boolean select) {
        for (AddScheduleToStudentActivity.StudentItem student : allStudents) {
            student.isSelected = select;
        }
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        int count = 0;
        for (AddScheduleToStudentActivity.StudentItem student : allStudents) {
            if (student.isSelected) count++;
        }
        return count;
    }

    public List<AddScheduleToStudentActivity.StudentItem> getSelectedStudents() {
        List<AddScheduleToStudentActivity.StudentItem> selected = new ArrayList<>();
        for (AddScheduleToStudentActivity.StudentItem student : allStudents) {
            if (student.isSelected) selected.add(student);
        }
        return selected;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        ImageView ivAvatar;
        TextView tvName, tvStudentId;

        ViewHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox_student);
            ivAvatar = itemView.findViewById(R.id.iv_student_avatar);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvStudentId = itemView.findViewById(R.id.tv_student_id);
        }
    }
}
