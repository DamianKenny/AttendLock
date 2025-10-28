package com.nibm.attendancetracker.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.admin.ViewScheduleActivity;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private final List<ViewScheduleActivity.Schedule> schedulesList;
    private final OnScheduleClickListener clickListener;

    public interface OnScheduleClickListener {
        void onScheduleClick(ViewScheduleActivity.Schedule schedule);
        void onDeleteClick(ViewScheduleActivity.Schedule schedule);
    }

    public ScheduleAdapter(List<ViewScheduleActivity.Schedule> schedulesList, OnScheduleClickListener clickListener) {
        this.schedulesList = schedulesList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        ViewScheduleActivity.Schedule schedule = schedulesList.get(position);

        // Display batch and subject count
        int subjectCount = schedule.getSubjects() != null ? schedule.getSubjects().size() : 0;
        String batchTitle = schedule.getBatch() + " - " + subjectCount +
                " Subject" + (subjectCount != 1 ? "s" : "");
        holder.tvSubjectName.setText(batchTitle);

        // Display all subject names (comma-separated)
        holder.tvBatch.setText(schedule.getSubjectNamesString());

        // Display totals from all subjects
        holder.tvTotalClasses.setText(String.valueOf(schedule.getTotalClasses()));
        holder.tvCredits.setText(String.valueOf(schedule.getTotalCredits()));

        // Display total lecture count from all subjects
        int totalLectures = schedule.getTotalLectures();
        holder.tvLectureCount.setText(totalLectures + (totalLectures == 1 ? " day" : " days"));

        // Display programme
        holder.tvProgramme.setText(schedule.getProgramme());

        // Set click listeners
        holder.itemView.setOnClickListener(v -> clickListener.onScheduleClick(schedule));

        holder.btnDelete.setOnClickListener(v -> {
            v.setTag(position);
            clickListener.onDeleteClick(schedule);
        });
    }

    @Override
    public int getItemCount() {
        return schedulesList.size();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName, tvBatch, tvTotalClasses, tvCredits, tvLectureCount, tvProgramme;
        ImageView btnDelete;

        ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tv_subject_name);
            tvBatch = itemView.findViewById(R.id.tv_batch);
            tvTotalClasses = itemView.findViewById(R.id.tv_total_classes);
            tvCredits = itemView.findViewById(R.id.tv_credits);
            tvLectureCount = itemView.findViewById(R.id.tv_lecture_count);
            tvProgramme = itemView.findViewById(R.id.tv_programme);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
