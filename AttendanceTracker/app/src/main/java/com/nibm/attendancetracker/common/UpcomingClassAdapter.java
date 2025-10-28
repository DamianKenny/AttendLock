package com.nibm.attendancetracker.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nibm.attendancetracker.R;

import java.util.List;

public class UpcomingClassAdapter extends RecyclerView.Adapter<UpcomingClassAdapter.ViewHolder> {
    private List<UpcomingClass> classes;

    public UpcomingClassAdapter(List<UpcomingClass> classes) {
        this.classes = classes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpcomingClass upcomingClass = classes.get(position);

        holder.tvSubjectName.setText(upcomingClass.subjectName);
        holder.tvTime.setText(upcomingClass.startTime);
        holder.tvDate.setText(upcomingClass.formattedDate);
        holder.tvTimeRange.setText(upcomingClass.startTime + " - " + upcomingClass.endTime);
        holder.tvDuration.setText(upcomingClass.duration);
        holder.tvStatus.setText(upcomingClass.status);

        // Set status color
        if ("Ongoing".equals(upcomingClass.status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_red_bg);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_green_bg);
        }
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvDuration, tvSubjectName, tvDate, tvTimeRange, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvSubjectName = itemView.findViewById(R.id.tv_subject_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTimeRange = itemView.findViewById(R.id.tv_time_range);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }

    public static class UpcomingClass {
        public String subjectName;
        public String date;
        public String startTime;
        public String endTime;
        public String formattedDate;
        public String duration;
        public String status;
    }
}
