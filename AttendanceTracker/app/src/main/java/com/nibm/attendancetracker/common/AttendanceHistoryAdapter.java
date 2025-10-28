package com.nibm.attendancetracker.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.nibm.attendancetracker.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.AttendanceHistoryViewHolder> {

    private List<SubjectDetailedAttendanceActivity.AttendanceRecord> attendanceHistory;

    public AttendanceHistoryAdapter(List<SubjectDetailedAttendanceActivity.AttendanceRecord> attendanceHistory) {
        this.attendanceHistory = attendanceHistory;
    }


    @NonNull
    @Override
    public AttendanceHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_history, parent, false);
        return new AttendanceHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceHistoryViewHolder holder, int position) {
        SubjectDetailedAttendanceActivity.AttendanceRecord record = attendanceHistory.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return attendanceHistory.size();
    }

    static class AttendanceHistoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardAttendanceRecord;
        ImageView ivStatusIcon;
        TextView tvDate, tvDay, tvStatus, tvTime;

        AttendanceHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAttendanceRecord = itemView.findViewById(R.id.card_attendance_record);
            ivStatusIcon = itemView.findViewById(R.id.iv_status_icon);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(SubjectDetailedAttendanceActivity.AttendanceRecord record) {
            // Format date
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

            try {
                Date date = inputFormat.parse(record.getDate());
                tvDay.setText(dayFormat.format(date));
                tvDate.setText(dateFormat.format(date));
            } catch (ParseException e) {
                tvDay.setText("---");
                tvDate.setText(record.getDate());
            }

            tvStatus.setText(record.getStatus());
            tvTime.setText(record.getTime());

            // Set status icon and colors
            switch (record.getStatus()) {
                case "Present":
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(itemView.getContext().getColor(R.color.success));
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.success));
                    break;
                case "Absent":
                    ivStatusIcon.setImageResource(R.drawable.ic_cancel);
                    ivStatusIcon.setColorFilter(itemView.getContext().getColor(R.color.error));
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.error));
                    break;
                case "Late":
                    ivStatusIcon.setImageResource(R.drawable.ic_schedule2);
                    ivStatusIcon.setColorFilter(itemView.getContext().getColor(R.color.warning));
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.warning));
                    break;
            }
        }
    }
}
