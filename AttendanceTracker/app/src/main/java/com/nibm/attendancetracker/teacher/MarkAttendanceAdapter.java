package com.nibm.attendancetracker.teacher;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.nibm.attendancetracker.R;

import java.util.List;

public class MarkAttendanceAdapter extends RecyclerView.Adapter<MarkAttendanceAdapter.AttendanceViewHolder> {

    private List<MarkAttendanceActivity.SubjectMarkAttendance> subjectList;
    private OnAttendanceMarkListener listener;

    public interface OnAttendanceMarkListener {
        void onAttendanceMark(MarkAttendanceActivity.SubjectMarkAttendance subject, String status);
    }

    public MarkAttendanceAdapter(List<MarkAttendanceActivity.SubjectMarkAttendance> subjectList,
                                 OnAttendanceMarkListener listener) {
        this.subjectList = subjectList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mark_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        MarkAttendanceActivity.SubjectMarkAttendance subject = subjectList.get(position);

        holder.tvSubjectName.setText(subject.getSubjectName());
        holder.cardSubject.setCardBackgroundColor(Color.parseColor(subject.getColor()));

        // Update status text
        holder.tvStatus.setText(subject.getAttendanceStatus());

        // Handle button clicks
        holder.btnPresent.setOnClickListener(v -> {
            subject.setAttendanceStatus("Present");
            listener.onAttendanceMark(subject, "Present");
            notifyItemChanged(position);
        });

        holder.btnAbsent.setOnClickListener(v -> {
            subject.setAttendanceStatus("Absent");
            listener.onAttendanceMark(subject, "Absent");
            notifyItemChanged(position);
        });

        holder.btnLate.setOnClickListener(v -> {
            subject.setAttendanceStatus("Late");
            listener.onAttendanceMark(subject, "Late");
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return subjectList.size();
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        CardView cardSubject;
        TextView tvSubjectName, tvStatus;
        Button btnPresent, btnAbsent, btnLate;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSubject = itemView.findViewById(R.id.card_subject);
            tvSubjectName = itemView.findViewById(R.id.tv_subject_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnPresent = itemView.findViewById(R.id.btn_present);
            btnAbsent = itemView.findViewById(R.id.btn_absent);
            btnLate = itemView.findViewById(R.id.btn_late);
        }
    }
}
