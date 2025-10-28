import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  collection,
  getDocs,
  doc,
  getDoc,
  updateDoc,
  arrayUnion,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { GradientButton } from "@/components/ui/button2";
import { Input } from "@/components/ui/input";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Search, X, Check } from "lucide-react";

interface Teacher {
  id: string;
  name: string;
  department: string;
  email: string;
  profilePictureUrl?: string;
}

interface Subject {
  id: string;
  name: string;
  category: string;
  color: string;
  isSelected: boolean;
}

const SUBJECT_COLORS = [
  "#FB923C",
  "#60A5FA",
  "#34D399",
  "#F472B6",
  "#8B5CF6",
  "#F59E0B",
  "#EF4444",
  "#10B981",
  "#6366F1",
  "#EC4899",
];

const AssignSubjects = () => {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [filteredTeachers, setFilteredTeachers] = useState<Teacher[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [filteredSubjects, setFilteredSubjects] = useState<Subject[]>([]);
  const [selectedTeacher, setSelectedTeacher] = useState<Teacher | null>(null);
  const [selectedSubjects, setSelectedSubjects] = useState<Subject[]>([]);
  const [assignedSubjectIds, setAssignedSubjectIds] = useState<Set<string>>(
    new Set()
  );
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("all");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchTeachers();
    fetchSubjects();
  }, []);

  useEffect(() => {
    filterTeachers(searchQuery);
  }, [searchQuery, teachers]);

  useEffect(() => {
    if (selectedTeacher) {
      filterSubjects(activeFilter);
    }
  }, [activeFilter, subjects, assignedSubjectIds]);

  const fetchTeachers = async () => {
    try {
      const querySnapshot = await getDocs(collection(db, "users"));
      const teachersList: Teacher[] = [];

      querySnapshot.forEach((doc) => {
        const data = doc.data();
        if (data.role === "teacher") {
          teachersList.push({
            id: doc.id,
            name: data.name || "Unknown",
            department: data.department || "Unknown",
            email: data.email || "No email",
            profilePictureUrl: data.profilePictureUrl || "",
          });
        }
      });

      setTeachers(teachersList);
      setFilteredTeachers(teachersList);
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to load teachers",
        variant: "destructive",
      });
    }
  };

  const fetchSubjects = async () => {
    try {
      const querySnapshot = await getDocs(collection(db, "subjects"));
      const subjectsList: Subject[] = [];
      let colorIndex = 0;

      querySnapshot.forEach((doc) => {
        const data = doc.data();
        const category = determineCategoryFromData(data);

        subjectsList.push({
          id: doc.id,
          name: data.subjectName || "Unknown Subject",
          category: category,
          color: SUBJECT_COLORS[colorIndex % SUBJECT_COLORS.length],
          isSelected: false,
        });
        colorIndex++;
      });

      setSubjects(subjectsList);
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to load subjects",
        variant: "destructive",
      });
    }
  };

  const determineCategoryFromData = (data: any): string => {
    if (data.category) {
      return data.category.toLowerCase();
    }
    if (data.course) {
      return data.course.toLowerCase();
    }

    const dept = data.department?.toLowerCase() || "";
    if (
      dept.includes("computer") ||
      dept.includes("information technology") ||
      dept.includes("software") ||
      dept.includes("computing")
    ) {
      return "computing";
    }
    if (
      dept.includes("engineering") ||
      dept.includes("mechanical") ||
      dept.includes("civil") ||
      dept.includes("electrical")
    ) {
      return "engineering";
    }

    return "engineering";
  };

  const filterTeachers = (query: string) => {
    if (!query.trim()) {
      setFilteredTeachers(teachers);
      return;
    }

    const filtered = teachers.filter(
      (teacher) =>
        teacher.name.toLowerCase().includes(query.toLowerCase()) ||
        teacher.department.toLowerCase().includes(query.toLowerCase()) ||
        teacher.email.toLowerCase().includes(query.toLowerCase())
    );
    setFilteredTeachers(filtered);
  };

  const selectTeacher = async (teacher: Teacher) => {
    setSelectedTeacher(teacher);
    setSelectedSubjects([]);
    await fetchAssignedSubjects(teacher.id);
  };

  const fetchAssignedSubjects = async (teacherId: string) => {
    try {
      const teacherDoc = await getDoc(doc(db, "users", teacherId));
      if (teacherDoc.exists()) {
        const data = teacherDoc.data();
        const assigned = data.assignedSubjects || [];
        setAssignedSubjectIds(new Set(assigned));
      }
    } catch (error) {
      console.error("Error fetching assigned subjects:", error);
    }
  };

  const clearTeacherSelection = () => {
    setSelectedTeacher(null);
    setSelectedSubjects([]);
    setAssignedSubjectIds(new Set());
    setActiveFilter("all");
  };

  const filterSubjects = (category: string) => {
    setActiveFilter(category);

    const filtered = subjects.filter((subject) => {
      if (assignedSubjectIds.has(subject.id)) {
        return false;
      }
      if (category === "all") {
        return true;
      }
      return subject.category === category.toLowerCase();
    });

    setFilteredSubjects(filtered);
  };

  const toggleSubjectSelection = (subject: Subject) => {
    if (!selectedTeacher) {
      toast({
        title: "No Teacher Selected",
        description: "Please select a teacher first",
        variant: "destructive",
      });
      return;
    }

    const isSelected = selectedSubjects.some((s) => s.id === subject.id);

    if (isSelected) {
      setSelectedSubjects(selectedSubjects.filter((s) => s.id !== subject.id));
    } else {
      setSelectedSubjects([...selectedSubjects, subject]);
    }
  };

  const assignSubjects = async () => {
    if (!selectedTeacher) {
      toast({
        title: "Error",
        description: "Please select a teacher",
        variant: "destructive",
      });
      return;
    }

    if (selectedSubjects.length === 0) {
      toast({
        title: "Error",
        description: "Please select at least one subject",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);

    try {
      const teacherRef = doc(db, "users", selectedTeacher.id);

      await updateDoc(teacherRef, {
        assignedSubjects: arrayUnion(...selectedSubjects.map((s) => s.id)),
        assignedSubjectNames: arrayUnion(
          ...selectedSubjects.map((s) => s.name)
        ),
      });

      toast({
        title: "Success",
        description: `Successfully assigned ${selectedSubjects.length} subject(s) to ${selectedTeacher.name}`,
      });

      clearTeacherSelection();
      navigate("/dashboard");
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.message || "Failed to assign subjects",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-6xl mx-auto animate-fade-up">
        <button
          onClick={() => navigate("/dashboard")}
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back</span>
        </button>

        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6">
          {/* Header */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-foreground mb-2">
              Assign Subjects
            </h1>
            <p className="text-sm text-muted-foreground">
              Select teacher and assign subjects
            </p>
          </div>

          {/* Search Teacher */}
          <div className="mb-6">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Search for teachers..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 bg-secondary border-border"
              />
            </div>
          </div>

          {/* Teachers List */}
          <div className="mb-6">
            <h3 className="text-base font-semibold text-foreground mb-4">
              Select Teacher
            </h3>
            <div className="max-h-[200px] overflow-y-auto space-y-2 pr-2">
              {filteredTeachers.map((teacher) => (
                <div
                  key={teacher.id}
                  onClick={() => selectTeacher(teacher)}
                  className="bg-card border border-border rounded-xl p-4 cursor-pointer hover:border-primary transition-all flex items-center gap-4"
                >
                  <div className="w-12 h-12 rounded-full bg-purple-500/20 flex items-center justify-center text-purple-400 font-semibold overflow-hidden">
                    {teacher.profilePictureUrl ? (
                      <img
                        src={teacher.profilePictureUrl}
                        alt={teacher.name}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      teacher.name.charAt(0).toUpperCase()
                    )}
                  </div>
                  <div className="flex-1">
                    <h4 className="font-semibold text-foreground">
                      {teacher.name}
                    </h4>
                    <p className="text-sm text-muted-foreground">
                      {teacher.department}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {teacher.email}
                    </p>
                  </div>
                  <ArrowLeft className="w-5 h-5 text-muted-foreground rotate-180" />
                </div>
              ))}
            </div>
          </div>

          {/* Selected Teacher Card */}
          {selectedTeacher && (
            <div className="mb-6">
              <div className="bg-blue-500/20 border border-blue-500/30 rounded-xl p-4 flex items-center gap-4">
                <div className="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center text-white font-semibold overflow-hidden">
                  {selectedTeacher.profilePictureUrl ? (
                    <img
                      src={selectedTeacher.profilePictureUrl}
                      alt={selectedTeacher.name}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    selectedTeacher.name.charAt(0).toUpperCase()
                  )}
                </div>
                <div className="flex-1">
                  <h4 className="font-semibold text-white text-lg">
                    {selectedTeacher.name}
                  </h4>
                  <p className="text-sm text-white/80">
                    {selectedTeacher.department}
                  </p>
                </div>
                <button
                  onClick={clearTeacherSelection}
                  className="p-2 hover:bg-white/10 rounded-lg transition-colors"
                >
                  <X className="w-5 h-5 text-white/80" />
                </button>
              </div>
            </div>
          )}

          {/* Available Subjects */}
          {selectedTeacher && (
            <>
              <div className="mb-6">
                <h3 className="text-base font-semibold text-foreground mb-4">
                  Available Subjects
                </h3>

                {/* Category Filters */}
                <div className="flex gap-2 mb-4">
                  <button
                    onClick={() => filterSubjects("all")}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      activeFilter === "all"
                        ? "bg-blue-500 text-white"
                        : "bg-secondary text-muted-foreground hover:bg-secondary/80"
                    }`}
                  >
                    All
                  </button>
                  <button
                    onClick={() => filterSubjects("engineering")}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      activeFilter === "engineering"
                        ? "bg-blue-500 text-white"
                        : "bg-secondary text-muted-foreground hover:bg-secondary/80"
                    }`}
                  >
                    Engineering
                  </button>
                  <button
                    onClick={() => filterSubjects("computing")}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      activeFilter === "computing"
                        ? "bg-blue-500 text-white"
                        : "bg-secondary text-muted-foreground hover:bg-secondary/80"
                    }`}
                  >
                    Computing
                  </button>
                </div>

                {/* Subjects Grid */}
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {filteredSubjects.map((subject) => {
                    const isSelected = selectedSubjects.some(
                      (s) => s.id === subject.id
                    );
                    return (
                      <div
                        key={subject.id}
                        onClick={() => toggleSubjectSelection(subject)}
                        style={{ backgroundColor: subject.color }}
                        className="relative rounded-2xl p-4 h-[120px] cursor-pointer transition-transform hover:scale-105 flex flex-col items-center justify-center"
                      >
                        {isSelected && (
                          <div className="absolute top-2 right-2">
                            <Check className="w-6 h-6 text-white" />
                          </div>
                        )}
                        <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center mb-2">
                          <span className="text-white text-lg">📚</span>
                        </div>
                        <h4 className="text-white text-sm font-semibold text-center line-clamp-2 mb-1">
                          {subject.name}
                        </h4>
                        <p className="text-white/80 text-xs capitalize">
                          {subject.category}
                        </p>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Selected Subjects Count */}
              <div className="mb-6">
                <div className="bg-card border border-border rounded-xl p-4 flex items-center justify-between">
                  <span className="text-foreground font-medium">
                    Selected Subjects
                  </span>
                  <div className="bg-gradient-to-r from-blue-500 to-purple-500 text-white rounded-lg px-4 py-2 font-bold">
                    {selectedSubjects.length}
                  </div>
                </div>
              </div>

              <div className="flex gap-[17%] pt-7">
                <GradientButton
                  className="w-[34.5%]"
                  type="button"
                  onClick={() => navigate("/dashboard")}
                  size="sm"
                >
                  Cancel
                </GradientButton>
                <GradientButton
                  type="submit"
                  size="sm"
                  className="w-[34.5%]"
                  onClick={assignSubjects}
                >
                  Assign Subjects
                </GradientButton>
              </div>
            </>
          )}

          {!selectedTeacher && (
            <div className="text-center py-12">
              <p className="text-muted-foreground">
                Select a teacher to begin assigning subjects
              </p>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default AssignSubjects;
