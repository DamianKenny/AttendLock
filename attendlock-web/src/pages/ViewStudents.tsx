import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { collection, getDocs, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Search, ChevronRight, Loader2, User } from "lucide-react";

interface Student {
  id: string;
  name: string;
  firstName?: string;
  lastName?: string;
  email: string;
  studentId: string;
  roll?: string;
  batch?: string;
  programme?: string;
  faculty?: string;
  courses?: string;
  phone?: string;
  dob?: string;
  gender?: string;
  address?: string;
  admissionDate?: string;
  parentName?: string;
  parentEmail?: string;
  profilePictureUrl?: string;
  password?: string;
}

const ViewStudents = () => {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [students, setStudents] = useState<Student[]>([]);
  const [filteredStudents, setFilteredStudents] = useState<Student[]>([]);
  const [programmes, setProgrammes] = useState<string[]>(["All Programmes"]);
  const [selectedProgramme, setSelectedProgramme] = useState("All Programmes");
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStudents();
  }, []);

  useEffect(() => {
    filterStudents();
  }, [selectedProgramme, searchQuery, students]);

  const loadStudents = async () => {
    setLoading(true);
    try {
      const q = query(collection(db, "users"), where("role", "==", "student"));
      const querySnapshot = await getDocs(q);

      const studentsList: Student[] = [];
      const uniqueProgrammes = new Set<string>();

      querySnapshot.forEach((doc) => {
        const data = doc.data();
        studentsList.push({
          id: doc.id,
          name: data.name || "Unknown",
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email || "No email",
          studentId: data.studentId || "N/A",
          roll: data.roll,
          batch: data.batch,
          programme: data.programme,
          faculty: data.faculty,
          courses: data.courses,
          phone: data.phone,
          dob: data.dob,
          gender: data.gender,
          address: data.address,
          admissionDate: data.admissionDate,
          parentName: data.parentName,
          parentEmail: data.parentEmail,
          profilePictureUrl: data.profilePictureUrl,
          password: data.password,
        });

        if (data.programme) {
          uniqueProgrammes.add(data.programme);
        }
      });

      setStudents(studentsList);
      setProgrammes(["All Programmes", ...Array.from(uniqueProgrammes)]);
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to load students",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const filterStudents = () => {
    let filtered = students;

    // Filter by programme
    if (selectedProgramme !== "All Programmes") {
      filtered = filtered.filter(
        (student) => student.programme === selectedProgramme
      );
    }

    // Filter by search query
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (student) =>
          student.studentId?.toLowerCase().includes(query) ||
          student.name?.toLowerCase().includes(query)
      );
    }

    setFilteredStudents(filtered);
  };

  const handleStudentClick = (student: Student) => {
    navigate(`/student-detail/${student.id}`, { state: { student } });
  };

  return (
    <Layout>
      <div className="max-w-6xl mx-auto animate-fade-up">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate("/dashboard")}
            className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span>Back</span>
          </button>
          <h1 className="text-2xl font-bold text-foreground">View Students</h1>
        </div>

        {/* Programme Filter */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-2xl p-4 mb-4">
          <Select
            value={selectedProgramme}
            onValueChange={setSelectedProgramme}
          >
            <SelectTrigger className="bg-secondary border-border">
              <SelectValue placeholder="Select programme" />
            </SelectTrigger>
            <SelectContent>
              {programmes.map((programme) => (
                <SelectItem key={programme} value={programme}>
                  {programme}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Search Bar */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-2xl p-4 mb-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search by Student ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 bg-secondary border-border"
            />
          </div>
        </div>

        {/* Student Count */}
        <p className="text-sm text-muted-foreground mb-4 px-2">
          {filteredStudents.length}{" "}
          {filteredStudents.length === 1 ? "student" : "students"}
        </p>

        {/* Students List */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl">
          <div className="p-6">
            {loading ? (
              <div className="flex flex-col items-center justify-center py-12">
                <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
                <p className="text-muted-foreground">Loading students...</p>
              </div>
            ) : filteredStudents.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12">
                <div className="w-24 h-24 rounded-full bg-blue-500/20 flex items-center justify-center mb-4">
                  <User className="w-12 h-12 text-blue-400" />
                </div>
                <p className="text-lg text-muted-foreground">
                  No students found
                </p>
                {searchQuery && (
                  <p className="text-sm text-muted-foreground mt-2">
                    Try adjusting your search
                  </p>
                )}
              </div>
            ) : (
              <div className="space-y-3">
                {filteredStudents.map((student) => (
                  <div
                    key={student.id}
                    onClick={() => handleStudentClick(student)}
                    className="bg-card border border-border rounded-xl p-4 cursor-pointer hover:border-primary transition-all flex items-center gap-4 group"
                  >
                    <div className="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400 font-semibold overflow-hidden">
                      {student.profilePictureUrl ? (
                        <img
                          src={student.profilePictureUrl}
                          alt={student.name}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        student.name.charAt(0).toUpperCase()
                      )}
                    </div>
                    <div className="flex-1">
                      <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors">
                        {student.name}
                      </h3>
                      <p className="text-sm text-muted-foreground">
                        {student.studentId}
                      </p>
                      <p className="text-xs text-muted-foreground truncate">
                        {student.programme || "No programme"}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {student.batch || "No batch"}
                      </p>
                    </div>
                    <ChevronRight className="w-5 h-5 text-muted-foreground group-hover:text-primary transition-colors" />
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

export default ViewStudents;
