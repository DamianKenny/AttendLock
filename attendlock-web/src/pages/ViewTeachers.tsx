import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { collection, getDocs, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { Input } from "@/components/ui/input";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Search, ChevronRight, Loader2 } from "lucide-react";

interface Teacher {
  id: string;
  name: string;
  firstName?: string;
  lastName?: string;
  email: string;
  department: string;
  employeeId?: string;
  phone?: string;
  dob?: string;
  joinDate?: string;
  qualification?: string;
  profilePictureUrl?: string;
  password?: string;
  assignedSubjects?: string[];
  assignedSubjectNames?: string[];
}

const ViewTeachers = () => {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [filteredTeachers, setFilteredTeachers] = useState<Teacher[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTeachers();
  }, []);

  useEffect(() => {
    filterTeachers(searchQuery);
  }, [searchQuery, teachers]);

  const loadTeachers = async () => {
    setLoading(true);
    try {
      const q = query(collection(db, "users"), where("role", "==", "teacher"));
      const querySnapshot = await getDocs(q);

      const teachersList: Teacher[] = [];
      querySnapshot.forEach((doc) => {
        const data = doc.data();
        teachersList.push({
          id: doc.id,
          name: data.name || "Unknown",
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email || "No email",
          department: data.department || "Unknown",
          employeeId: data.employeeId,
          phone: data.phone,
          dob: data.dob,
          joinDate: data.joinDate,
          qualification: data.qualification,
          profilePictureUrl: data.profilePictureUrl,
          password: data.password,
          assignedSubjects: data.assignedSubjects || [],
          assignedSubjectNames: data.assignedSubjectNames || [],
        });
      });

      setTeachers(teachersList);
      setFilteredTeachers(teachersList);

      if (teachersList.length === 0) {
        toast({
          title: "No Teachers Found",
          description: "No teachers found in database",
        });
      } else {
        toast({
          title: "Success",
          description: `Loaded ${teachersList.length} teachers`,
        });
      }
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.message || "Failed to load teachers",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const filterTeachers = (query: string) => {
    if (!query.trim()) {
      setFilteredTeachers(teachers);
      return;
    }

    const filtered = teachers.filter(
      (teacher) =>
        teacher.name.toLowerCase().includes(query.toLowerCase()) ||
        teacher.email.toLowerCase().includes(query.toLowerCase()) ||
        teacher.department.toLowerCase().includes(query.toLowerCase()) ||
        (teacher.employeeId &&
          teacher.employeeId.toLowerCase().includes(query.toLowerCase()))
    );
    setFilteredTeachers(filtered);
  };

  const handleTeacherClick = (teacher: Teacher) => {
    navigate(`/teacher-detail/${teacher.id}`, { state: { teacher } });
  };

  return (
    <Layout>
      <div className="max-w-6xl mx-auto animate-fade-up">
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate("/dashboard")}
            className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors "
          >
            <ArrowLeft className="w-5 h-5" />
            <span>Back</span>
          </button>
          <h1 className="text-2xl font-bold text-foreground">View Teachers</h1>
        </div>

        {/* search bar */}
        <div className="mb-6">
          <div className="backdrop-blur-xl bg-card/50 border border-border rounded-2xl p-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Search teachers..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 bg-secondary border-border"
              />
            </div>
          </div>
        </div>

        {/* teachers list */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl">
          <div className="p-6">
            {loading ? (
              <div className="flex flex-col items-center justify-center py-12">
                <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
                <p className="text-muted-foreground">Loading teachers...</p>
              </div>
            ) : filteredTeachers.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12">
                <div className="w-24 h-24 rounded-full bg-purple-500/20 flex items-center justify-center mb-4">
                  <span className="text-4xl">👨‍🏫</span>
                </div>
                <p className="text-lg text-muted-foreground">
                  No teachers found
                </p>
                {searchQuery && (
                  <p className="text-sm text-muted-foreground mt-2">
                    Try adjusting your search
                  </p>
                )}
              </div>
            ) : (
              <div className="space-y-3">
                {filteredTeachers.map((teacher) => (
                  <div
                    key={teacher.id}
                    onClick={() => handleTeacherClick(teacher)}
                    className="bg-card border border-border rounded-xl p-4 cursor-pointer hover:border-primary transition-all flex items-center gap-4 group"
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
                      <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors">
                        {teacher.name}
                      </h3>
                      <p className="text-sm text-muted-foreground">
                        {teacher.department}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {teacher.email}
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

export default ViewTeachers;
