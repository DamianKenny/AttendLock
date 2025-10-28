import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "@/components/Layout";
import { ArrowLeft, Plus, X, Calendar, Clock } from "lucide-react";
import { collection, addDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { toast } from "sonner";

interface LectureSchedule {
  date: string;
  startTime: string;
  endTime: string;
}

interface Subject {
  subjectName: string;
  totalClasses: string;
  credits: string;
  lectureSchedules: LectureSchedule[];
}

const CreateSchedule = () => {
  const navigate = useNavigate();

  const [faculty, setFaculty] = useState("");
  const [programme, setProgramme] = useState("");
  const [batch, setBatch] = useState("");
  const [subjects, setSubjects] = useState<Subject[]>([
    {
      subjectName: "",
      totalClasses: "",
      credits: "",
      lectureSchedules: [],
    },
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const faculties = [
    "School of Computing",
    "School of Engineering",
    "School of Business",
    "School of Language",
    "School of Design",
    "School of Humanities",
  ];

  const facultyProgrammes: Record<string, string[]> = {
    "School of Computing": [
      "BSc (Hons) in Ethical Hacking and Network Security",
      "BSc (Hons) in Information Technology for Business",
      "BSc (Hons) in Computing (Software Engineering Pathway)",
      "BA (Hons) in Creative Multimedia",
      "Higher National Diploma in Network Engineering (Part Time)",
      "Higher National Diploma in Software Engineering (Part Time)",
      "Higher National Diploma in Network Engineering (Full Time)",
      "Higher National Diploma in Computer Science with Artificial Intelligence",
      "Diploma in Information and Communication Technology",
      "Diploma in Network Engineering (Part Time)",
      "Diploma in Software Engineering (Full Time)",
      "Diploma in Computer Science with Artificial Intelligence",
    ],
    "School of Business": [
      "Advanced Diploma in Business Management",
      "Higher National Diploma in Business Management",
      "Advanced Diploma in Marketing Management",
      "Advanced Diploma in Human Resource Management",
      "Advanced Diploma in Project Management",
    ],
    "School of Engineering": [
      "Degree in Civil Engineering",
      "Degree in Electro-Mechanical Engineering",
      "Diploma in Civil Engineering",
      "Diploma in Electro-Mechanical Engineering",
      "Advanced Diploma in AI & Robotics",
    ],
    "School of Language": [
      "Diploma in English Language",
      "Certificate in English Language",
    ],
    "School of Design": [
      "Diploma in Fashion Design",
      "Higher National Diploma in Fashion Design",
      "Diploma in Interior Architecture",
    ],
    "School of Humanities": [
      "Diploma in Humanities",
      "Certificate in Humanities",
    ],
  };

  const programmeBatches: Record<string, string[]> = {
    "Higher National Diploma in Network Engineering (Part Time)": [
      "HNDNE242F",
      "HNDNE241F",
      "HNDNE232F",
    ],
    "Higher National Diploma in Software Engineering (Part Time)": [
      "HNDSE242F",
      "HNDSE241F",
      "HNDSE232F",
    ],
    "Higher National Diploma in Network Engineering (Full Time)": [
      "HNDNE242P",
      "HNDNE241P",
      "HNDNE232P",
    ],
    "BSc (Hons) in Ethical Hacking and Network Security": [
      "BSCEH242",
      "BSCEH241",
      "BSCEH232",
    ],
    "Diploma in Fashion Design": ["DFD242", "DFD241", "DFD232"],
    "Higher National Diploma in Business Management": [
      "HNDBM242",
      "HNDBM241",
      "HNDBM232",
    ],
    "Diploma in Civil Engineering": ["DCE242", "DCE241", "DCE232"],
  };

  const generateBatchCode = (programmeName: string): string => {
    const words = programmeName.split(" ");
    let code = "";
    for (const word of words) {
      if (
        word.length > 0 &&
        !["in", "of", "and"].includes(word.toLowerCase())
      ) {
        code += word.charAt(0);
      }
    }
    return code.toUpperCase();
  };

  const getBatches = (programmeName: string): string[] => {
    if (programmeBatches[programmeName]) {
      return programmeBatches[programmeName];
    }
    const code = generateBatchCode(programmeName);
    return [`${code}242`, `${code}241`, `${code}232`];
  };

  const addSubject = () => {
    setSubjects([
      ...subjects,
      {
        subjectName: "",
        totalClasses: "",
        credits: "",
        lectureSchedules: [],
      },
    ]);
  };

  const removeSubject = (index: number) => {
    if (subjects.length > 1) {
      setSubjects(subjects.filter((_, i) => i !== index));
    } else {
      toast.error("At least one subject is required");
    }
  };

  const updateSubject = (index: number, field: keyof Subject, value: any) => {
    const newSubjects = [...subjects];
    newSubjects[index] = { ...newSubjects[index], [field]: value };
    setSubjects(newSubjects);
  };

  const addLectureSchedule = (subjectIndex: number) => {
    const newSubjects = [...subjects];
    newSubjects[subjectIndex].lectureSchedules.push({
      date: "",
      startTime: "",
      endTime: "",
    });
    setSubjects(newSubjects);
  };

  const removeLectureSchedule = (
    subjectIndex: number,
    lectureIndex: number
  ) => {
    const newSubjects = [...subjects];
    newSubjects[subjectIndex].lectureSchedules = newSubjects[
      subjectIndex
    ].lectureSchedules.filter((_, i) => i !== lectureIndex);
    setSubjects(newSubjects);
  };

  const updateLectureSchedule = (
    subjectIndex: number,
    lectureIndex: number,
    field: keyof LectureSchedule,
    value: string
  ) => {
    const newSubjects = [...subjects];
    newSubjects[subjectIndex].lectureSchedules[lectureIndex] = {
      ...newSubjects[subjectIndex].lectureSchedules[lectureIndex],
      [field]: value,
    };
    setSubjects(newSubjects);
  };

  const validateInputs = (): boolean => {
    if (!faculty) {
      toast.error("Please select a faculty");
      return false;
    }
    if (!programme) {
      toast.error("Please select a programme");
      return false;
    }
    if (!batch) {
      toast.error("Please select a batch");
      return false;
    }
    if (subjects.length === 0) {
      toast.error("Please add at least one subject");
      return false;
    }

    for (let i = 0; i < subjects.length; i++) {
      const subject = subjects[i];

      if (!subject.subjectName.trim()) {
        toast.error(`Please enter name for Subject ${i + 1}`);
        return false;
      }
      if (!subject.totalClasses.trim()) {
        toast.error(`Please enter total classes for Subject ${i + 1}`);
        return false;
      }
      if (!subject.credits.trim()) {
        toast.error(`Please enter credits for Subject ${i + 1}`);
        return false;
      }
      if (subject.lectureSchedules.length === 0) {
        toast.error(
          `Please add at least one lecture schedule for Subject ${i + 1}`
        );
        return false;
      }

      for (let j = 0; j < subject.lectureSchedules.length; j++) {
        const lecture = subject.lectureSchedules[j];

        if (!lecture.date) {
          toast.error(
            `Please select date for Lecture ${j + 1} in Subject ${i + 1}`
          );
          return false;
        }
        if (!lecture.startTime) {
          toast.error(
            `Please select start time for Lecture ${j + 1} in Subject ${i + 1}`
          );
          return false;
        }
        if (!lecture.endTime) {
          toast.error(
            `Please select end time for Lecture ${j + 1} in Subject ${i + 1}`
          );
          return false;
        }
      }
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateInputs()) {
      return;
    }

    setIsSubmitting(true);

    try {
      const subjectsData = subjects.map((subject) => ({
        subjectName: subject.subjectName.trim(),
        totalClasses: parseInt(subject.totalClasses),
        credits: parseInt(subject.credits),
        lectureSchedules: subject.lectureSchedules,
      }));

      const scheduleData = {
        faculty,
        programme,
        batch,
        subjects: subjectsData,
        createdAt: Date.now(),
      };

      await addDoc(collection(db, "schedules"), scheduleData);

      toast.success("Schedule created successfully!");
      navigate("/dashboard");
    } catch (error) {
      console.error("Error creating schedule:", error);
      toast.error("Failed to create schedule");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-5xl mx-auto pb-8 animate-fade-up">
        <button
          onClick={() => navigate("/dashboard")}
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          <span className="font-cascadia">Back</span>
        </button>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Header */}
          <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-8">
            <h1 className="text-3xl font-bold text-foreground font-cascadia">
              Create Schedule
            </h1>
          </div>

          {/* Batch Information */}
          <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-8">
            <h2 className="text-xl font-bold text-foreground font-cascadia mb-6">
              Batch Information
            </h2>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-cascadia text-muted-foreground mb-2">
                  Faculty
                </label>
                <select
                  value={faculty}
                  onChange={(e) => {
                    setFaculty(e.target.value);
                    setProgramme("");
                    setBatch("");
                  }}
                  className="w-full px-4 py-3 bg-background/50 border border-border rounded-xl text-foreground font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all"
                  required
                >
                  <option value="">Select Faculty</option>
                  {faculties.map((f) => (
                    <option key={f} value={f}>
                      {f}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-cascadia text-muted-foreground mb-2">
                  Programme
                </label>
                <select
                  value={programme}
                  onChange={(e) => {
                    setProgramme(e.target.value);
                    setBatch("");
                  }}
                  disabled={!faculty}
                  className="w-full px-4 py-3 bg-background/50 border border-border rounded-xl text-foreground font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all disabled:opacity-50"
                  required
                >
                  <option value="">Select Programme</option>
                  {faculty &&
                    facultyProgrammes[faculty]?.map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-cascadia text-muted-foreground mb-2">
                  Batch
                </label>
                <select
                  value={batch}
                  onChange={(e) => setBatch(e.target.value)}
                  disabled={!programme}
                  className="w-full px-4 py-3 bg-background/50 border border-border rounded-xl text-foreground font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all disabled:opacity-50"
                  required
                >
                  <option value="">Select Batch</option>
                  {programme &&
                    getBatches(programme).map((b) => (
                      <option key={b} value={b}>
                        {b}
                      </option>
                    ))}
                </select>
              </div>
            </div>
          </div>

          {/* Subjects */}
          <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-8">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-bold text-foreground font-cascadia">
                Subjects
              </h2>
              <button
                type="button"
                onClick={addSubject}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-cascadia transition-colors"
              >
                <Plus className="w-4 h-4" />
                Add Subject
              </button>
            </div>

            <div className="space-y-6">
              {subjects.map((subject, subjectIndex) => (
                <div
                  key={subjectIndex}
                  className="backdrop-blur-xl bg-background/30 border border-border rounded-2xl p-6"
                >
                  {/* Subject Header */}
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-bold text-foreground font-cascadia">
                      Subject {subjectIndex + 1}
                    </h3>
                    <button
                      type="button"
                      onClick={() => removeSubject(subjectIndex)}
                      className="p-2 bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded-lg transition-colors"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Subject Name */}
                  <div className="mb-4">
                    <label className="block text-sm font-cascadia text-muted-foreground mb-2">
                      Subject Name
                    </label>
                    <input
                      type="text"
                      value={subject.subjectName}
                      onChange={(e) =>
                        updateSubject(
                          subjectIndex,
                          "subjectName",
                          e.target.value
                        )
                      }
                      placeholder="Enter subject name"
                      className="w-full px-4 py-3 bg-background/50 border border-border rounded-xl text-foreground font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all"
                      required
                    />
                  </div>

                  {/* Total Classes & Credits */}
                  <div className="grid grid-cols-2 gap-4 mb-4">
                    <div>
                      <label className="block text-sm font-cascadia text-muted-foreground mb-2">
                        Total Classes
                      </label>
                      <input
                        type="number"
                        value={subject.totalClasses}
                        onChange={(e) =>
                          updateSubject(
                            subjectIndex,
                            "totalClasses",
                            e.target.value
                          )
                        }
                        placeholder="Classes"
                        min="1"
                        className="w-full px-4 py-3 bg-background/50 border border-border rounded-xl text-foreground font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-cascadia text-muted-foreground mb-2">
                        Credits
                      </label>
                      <input
                        type="number"
                        value={subject.credits}
                        onChange={(e) =>
                          updateSubject(subjectIndex, "credits", e.target.value)
                        }
                        placeholder="Credits"
                        min="1"
                        className="w-full px-4 py-3 bg-background/50 border border-border rounded-xl text-foreground font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all"
                        required
                      />
                    </div>
                  </div>

                  {/* Lecture Schedules */}
                  <div className="border-t border-border pt-4">
                    <div className="flex items-center justify-between mb-4">
                      <h4 className="text-sm font-bold text-foreground font-cascadia">
                        Lecture Schedule
                      </h4>
                      <button
                        type="button"
                        onClick={() => addLectureSchedule(subjectIndex)}
                        className="flex items-center gap-2 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-sm rounded-lg font-cascadia transition-colors"
                      >
                        <Plus className="w-3 h-3" />
                        Add Lecture
                      </button>
                    </div>

                    <div className="space-y-3">
                      {subject.lectureSchedules.map((lecture, lectureIndex) => (
                        <div
                          key={lectureIndex}
                          className="bg-background/30 border border-border rounded-xl p-4"
                        >
                          <div className="flex items-center justify-between mb-3">
                            <span className="text-sm font-cascadia text-muted-foreground">
                              Lecture {lectureIndex + 1}
                            </span>
                            <button
                              type="button"
                              onClick={() =>
                                removeLectureSchedule(
                                  subjectIndex,
                                  lectureIndex
                                )
                              }
                              className="p-1.5 bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded-lg transition-colors"
                            >
                              <X className="w-3 h-3" />
                            </button>
                          </div>

                          <div className="space-y-3">
                            <div>
                              <label className="block text-xs font-cascadia text-muted-foreground mb-1.5 flex items-center gap-1">
                                <Calendar className="w-3 h-3" />
                                Date
                              </label>
                              <input
                                type="date"
                                value={lecture.date}
                                onChange={(e) =>
                                  updateLectureSchedule(
                                    subjectIndex,
                                    lectureIndex,
                                    "date",
                                    e.target.value
                                  )
                                }
                                min={new Date().toISOString().split("T")[0]}
                                className="w-full px-3 py-2 bg-background/50 border border-border rounded-lg text-foreground text-sm font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all"
                                required
                              />
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                              <div>
                                <label className="block text-xs font-cascadia text-muted-foreground mb-1.5 flex items-center gap-1">
                                  <Clock className="w-3 h-3" />
                                  Start Time
                                </label>
                                <input
                                  type="time"
                                  value={lecture.startTime}
                                  onChange={(e) =>
                                    updateLectureSchedule(
                                      subjectIndex,
                                      lectureIndex,
                                      "startTime",
                                      e.target.value
                                    )
                                  }
                                  className="w-full px-3 py-2 bg-background/50 border border-border rounded-lg text-foreground text-sm font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all"
                                  required
                                />
                              </div>
                              <div>
                                <label className="block text-xs font-cascadia text-muted-foreground mb-1.5 flex items-center gap-1">
                                  <Clock className="w-3 h-3" />
                                  End Time
                                </label>
                                <input
                                  type="time"
                                  value={lecture.endTime}
                                  onChange={(e) =>
                                    updateLectureSchedule(
                                      subjectIndex,
                                      lectureIndex,
                                      "endTime",
                                      e.target.value
                                    )
                                  }
                                  className="w-full px-3 py-2 bg-background/50 border border-border rounded-lg text-foreground text-sm font-cascadia focus:outline-none focus:ring-2 focus:ring-primary transition-all"
                                  required
                                />
                              </div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-4 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white rounded-2xl font-bold font-cascadia shadow-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? "Creating Schedule..." : "Create Schedule"}
          </button>
        </form>
      </div>
    </Layout>
  );
};

export default CreateSchedule;
