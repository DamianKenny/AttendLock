import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { collection, getDocs, doc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Plus, Filter, Loader2, X, Calendar } from "lucide-react";

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
  createdAt: number;
}

const ViewSchedule = () => {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [filteredSchedules, setFilteredSchedules] = useState<Schedule[]>([]);
  const [batches, setBatches] = useState<string[]>(["All Batches"]);
  const [selectedBatch, setSelectedBatch] = useState("All Batches");
  const [loading, setLoading] = useState(true);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  useEffect(() => {
    loadSchedules();
  }, []);

  useEffect(() => {
    filterSchedules();
  }, [selectedBatch, schedules]);

  const loadSchedules = async () => {
    setLoading(true);
    try {
      const querySnapshot = await getDocs(collection(db, "schedules"));
      const schedulesList: Schedule[] = [];
      const uniqueBatches = new Set<string>();

      querySnapshot.forEach((doc) => {
        const data = doc.data();
        schedulesList.push({
          id: doc.id,
          faculty: data.faculty || "N/A",
          programme: data.programme || "N/A",
          batch: data.batch || "N/A",
          subjects: data.subjects || [],
          createdAt: data.createdAt || Date.now(),
        });

        if (data.batch) {
          uniqueBatches.add(data.batch);
        }
      });

      setSchedules(schedulesList);
      setBatches(["All Batches", ...Array.from(uniqueBatches)]);
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to load schedules",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const filterSchedules = () => {
    if (selectedBatch === "All Batches") {
      setFilteredSchedules(schedules);
    } else {
      const filtered = schedules.filter(
        (schedule) => schedule.batch === selectedBatch
      );
      setFilteredSchedules(filtered);
    }
  };

  const handleScheduleClick = (schedule: Schedule) => {
    navigate(`/schedule-detail/${schedule.id}`, { state: { schedule } });
  };

  const handleDelete = async (scheduleId: string, e: React.MouseEvent) => {
    e.stopPropagation();

    if (window.confirm("Are you sure you want to delete this schedule?")) {
      setDeleteId(scheduleId);
      try {
        await deleteDoc(doc(db, "schedules", scheduleId));
        toast({
          title: "Success",
          description: "Schedule deleted successfully",
        });
        loadSchedules();
      } catch (error: any) {
        toast({
          title: "Error",
          description: "Failed to delete schedule",
          variant: "destructive",
        });
      } finally {
        setDeleteId(null);
      }
    }
  };

  const getTotalClasses = (subjects: Subject[]): number => {
    return subjects.reduce(
      (sum, subject) => sum + (subject.totalClasses || 0),
      0
    );
  };

  const getTotalCredits = (subjects: Subject[]): number => {
    return subjects.reduce((sum, subject) => sum + (subject.credits || 0), 0);
  };

  const getTotalLectures = (subjects: Subject[]): number => {
    return subjects.reduce(
      (sum, subject) => sum + (subject.lectureSchedules?.length || 0),
      0
    );
  };

  return (
    <Layout>
      <div className="max-w-6xl mx-auto animate-fade-up">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate("/dashboard")}
              className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
              <span>Back</span>
            </button>
            <h1 className="text-2xl font-bold text-foreground">Schedules</h1>
          </div>
          <button
            onClick={() => navigate("/create-schedule")}
            className="flex items-center gap-2 bg-purple-500 hover:bg-purple-600 text-white px-4 py-2 rounded-full transition-colors"
          >
            <Plus className="w-5 h-5" />
            <span>Add</span>
          </button>
        </div>

        {/* Filter Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-2xl p-4 mb-6">
          <div className="flex items-center gap-3">
            <Filter className="w-5 h-5 text-muted-foreground" />
            <Select value={selectedBatch} onValueChange={setSelectedBatch}>
              <SelectTrigger className="bg-secondary border-border">
                <SelectValue placeholder="Select batch" />
              </SelectTrigger>
              <SelectContent>
                {batches.map((batch) => (
                  <SelectItem key={batch} value={batch}>
                    {batch}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Schedules List */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl">
          <div className="p-6">
            {loading ? (
              <div className="flex flex-col items-center justify-center py-12">
                <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
                <p className="text-muted-foreground">Loading schedules...</p>
              </div>
            ) : filteredSchedules.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12">
                <div className="w-24 h-24 rounded-full bg-purple-500/20 flex items-center justify-center mb-4">
                  <Calendar className="w-12 h-12 text-purple-400" />
                </div>
                <p className="text-lg text-muted-foreground">
                  No schedules found
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {filteredSchedules.map((schedule) => (
                  <div
                    key={schedule.id}
                    onClick={() => handleScheduleClick(schedule)}
                    className="bg-card border border-border rounded-xl p-4 cursor-pointer hover:border-primary transition-all"
                  >
                    {/* Header */}
                    <div className="flex items-center gap-3 mb-3">
                      <div className="w-10 h-10 rounded-full bg-purple-500/20 flex items-center justify-center">
                        <Calendar className="w-5 h-5 text-purple-400" />
                      </div>
                      <div className="flex-1">
                        <h3 className="font-semibold text-foreground">
                          {schedule.batch} - {schedule.subjects.length} Subject
                          {schedule.subjects.length !== 1 ? "s" : ""}
                        </h3>
                        <p className="text-sm text-muted-foreground">
                          {schedule.subjects
                            .map((s) => s.subjectName)
                            .join(", ")}
                        </p>
                      </div>
                      <button
                        onClick={(e) => handleDelete(schedule.id, e)}
                        disabled={deleteId === schedule.id}
                        className="p-2 hover:bg-destructive/10 rounded-lg transition-colors disabled:opacity-50"
                      >
                        {deleteId === schedule.id ? (
                          <Loader2 className="w-5 h-5 text-destructive animate-spin" />
                        ) : (
                          <X className="w-5 h-5 text-destructive" />
                        )}
                      </button>
                    </div>

                    {/* Details Grid */}
                    <div className="grid grid-cols-3 gap-4 mb-3">
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">
                          Classes
                        </p>
                        <p className="text-base font-bold text-foreground">
                          {getTotalClasses(schedule.subjects)}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">
                          Credits
                        </p>
                        <p className="text-base font-bold text-foreground">
                          {getTotalCredits(schedule.subjects)}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">
                          Lectures
                        </p>
                        <p className="text-base font-bold text-foreground">
                          {getTotalLectures(schedule.subjects)} days
                        </p>
                      </div>
                    </div>

                    {/* Programme */}
                    <p className="text-xs text-muted-foreground truncate">
                      {schedule.programme}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default ViewSchedule;
