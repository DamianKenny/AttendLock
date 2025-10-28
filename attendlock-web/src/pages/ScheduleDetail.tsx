import React, { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { doc, getDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { useToast } from "@/hooks/use-toast";
import {
  ArrowLeft,
  Calendar,
  Building2,
  GraduationCap,
  Clock,
  Loader2,
  BookOpen,
} from "lucide-react";

interface LectureSchedule {
  date: string;
  startTime: string;
  endTime: string;
}

interface Subject {
  subjectName: string;
  totalClasses: number;
  credits: number;
  lectureSchedules: LectureSchedule[];
}

interface Schedule {
  id: string;
  faculty: string;
  programme: string;
  batch: string;
  subjects: Subject[];
}

const ScheduleDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const { toast } = useToast();

  const [schedule, setSchedule] = useState<Schedule | null>(
    location.state?.schedule || null
  );
  const [loading, setLoading] = useState(!schedule);

  useEffect(() => {
    if (id && !schedule) {
      loadScheduleData();
    }
  }, [id]);

  const loadScheduleData = async () => {
    if (!id) return;

    setLoading(true);
    try {
      const scheduleDoc = await getDoc(doc(db, "schedules", id));
      if (scheduleDoc.exists()) {
        const data = scheduleDoc.data();
        setSchedule({
          id: scheduleDoc.id,
          faculty: data.faculty || "N/A",
          programme: data.programme || "N/A",
          batch: data.batch || "N/A",
          subjects: data.subjects || [],
        });
      } else {
        toast({
          title: "Error",
          description: "Schedule not found",
          variant: "destructive",
        });
        navigate("/view-schedule");
      }
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to load schedule details",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const getTotalClasses = (): number => {
    if (!schedule) return 0;
    return schedule.subjects.reduce(
      (sum, subject) => sum + (subject.totalClasses || 0),
      0
    );
  };

  const getTotalCredits = (): number => {
    if (!schedule) return 0;
    return schedule.subjects.reduce(
      (sum, subject) => sum + (subject.credits || 0),
      0
    );
  };

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-96">
          <Loader2 className="w-12 h-12 text-primary animate-spin" />
        </div>
      </Layout>
    );
  }

  if (!schedule) {
    return (
      <Layout>
        <div className="text-center py-12">
          <p className="text-muted-foreground">Schedule not found</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-4xl mx-auto animate-fade-up">
        {/* Header */}
        <button
          onClick={() => navigate("/schedule")}
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back</span>
        </button>

        {/* Batch Info Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-8 mb-6">
          <div className="flex flex-col items-center">
            <div className="w-20 h-20 rounded-full bg-purple-500/20 flex items-center justify-center mb-4">
              <Calendar className="w-10 h-10 text-purple-400" />
            </div>
            <h1 className="text-2xl font-bold text-foreground mb-2 text-center">
              {schedule.batch}
            </h1>
            <p className="text-muted-foreground text-base">
              {schedule.subjects.length} Subject
              {schedule.subjects.length !== 1 ? "s" : ""}
            </p>
          </div>
        </div>

        {/* Programme Information Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6 mb-6">
          <h2 className="text-lg font-bold text-foreground mb-4">
            Programme Information
          </h2>
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <Building2 className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Faculty</p>
                <p className="text-sm text-foreground">{schedule.faculty}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <GraduationCap className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Programme</p>
                <p className="text-sm text-foreground">{schedule.programme}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Overall Statistics Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6 mb-6">
          <h2 className="text-lg font-bold text-foreground mb-4">
            Overall Statistics
          </h2>
          <div className="grid grid-cols-2 gap-6">
            <div className="text-center">
              <p className="text-xs text-muted-foreground mb-2">
                Total Classes
              </p>
              <p className="text-4xl font-bold text-purple-400">
                {getTotalClasses()}
              </p>
            </div>
            <div className="text-center">
              <p className="text-xs text-muted-foreground mb-2">
                Total Credits
              </p>
              <p className="text-4xl font-bold text-purple-400">
                {getTotalCredits()}
              </p>
            </div>
          </div>
        </div>

        {/* Subjects Cards */}
        <div className="space-y-6">
          {schedule.subjects.map((subject, index) => (
            <div
              key={index}
              className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6"
            >
              {/* Subject Header */}
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center">
                  <BookOpen className="w-6 h-6 text-blue-400" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-foreground">
                    {subject.subjectName}
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    {subject.totalClasses} Classes • {subject.credits} Credits
                  </p>
                </div>
              </div>

              {/* Lecture Schedule */}
              <div className="mt-4">
                <h4 className="text-sm font-semibold text-foreground mb-3">
                  Lecture Schedule
                </h4>
                {subject.lectureSchedules &&
                subject.lectureSchedules.length > 0 ? (
                  <div className="space-y-2">
                    {subject.lectureSchedules.map((lecture, lectureIndex) => (
                      <div
                        key={lectureIndex}
                        className="bg-card border border-border rounded-xl p-3 flex items-center gap-3"
                      >
                        <div className="w-10 h-10 rounded-full bg-purple-500/20 flex items-center justify-center">
                          <Clock className="w-5 h-5 text-purple-400" />
                        </div>
                        <div className="flex-1">
                          <p className="text-sm font-semibold text-foreground">
                            {lecture.date || "N/A"}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {lecture.startTime || "--:--"} -{" "}
                            {lecture.endTime || "--:--"}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    No lecture times scheduled
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </Layout>
  );
};

export default ScheduleDetail;
