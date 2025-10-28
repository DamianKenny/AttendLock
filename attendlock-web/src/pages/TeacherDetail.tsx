import React, { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { doc, getDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { useToast } from "@/hooks/use-toast";
import {
  ArrowLeft,
  Edit,
  Mail,
  Phone,
  Calendar,
  Building2,
  Award,
  Loader2,
} from "lucide-react";

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
  assignedSubjectNames?: string[];
}

const TeacherDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const { toast } = useToast();

  const [teacher, setTeacher] = useState<Teacher | null>(
    location.state?.teacher || null
  );
  const [loading, setLoading] = useState(!teacher);

  useEffect(() => {
    if (id && !teacher) {
      loadTeacherData();
    } else if (id) {
      // Refresh data even if we have state
      refreshTeacherData();
    }
  }, [id]);

  const loadTeacherData = async () => {
    if (!id) return;

    setLoading(true);
    try {
      const teacherDoc = await getDoc(doc(db, "users", id));
      if (teacherDoc.exists()) {
        const data = teacherDoc.data();
        setTeacher({
          id: teacherDoc.id,
          name: data.name || "Unknown",
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email || "N/A",
          department: data.department || "N/A",
          employeeId: data.employeeId,
          phone: data.phone,
          dob: data.dob,
          joinDate: data.joinDate,
          qualification: data.qualification,
          profilePictureUrl: data.profilePictureUrl,
          assignedSubjectNames: data.assignedSubjectNames || [],
        });
      } else {
        toast({
          title: "Error",
          description: "Teacher not found",
          variant: "destructive",
        });
        navigate("/view-teachers");
      }
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to load teacher details",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const refreshTeacherData = async () => {
    if (!id) return;

    try {
      const teacherDoc = await getDoc(doc(db, "users", id));
      if (teacherDoc.exists()) {
        const data = teacherDoc.data();
        setTeacher({
          id: teacherDoc.id,
          name: data.name || "Unknown",
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email || "N/A",
          department: data.department || "N/A",
          employeeId: data.employeeId,
          phone: data.phone,
          dob: data.dob,
          joinDate: data.joinDate,
          qualification: data.qualification,
          profilePictureUrl: data.profilePictureUrl,
          assignedSubjectNames: data.assignedSubjectNames || [],
        });
      }
    } catch (error) {
      console.error("Error refreshing teacher data:", error);
    }
  };

  const handleEdit = () => {
    if (teacher) {
      navigate(`/edit-teacher/${teacher.id}`, { state: { teacher } });
    }
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

  if (!teacher) {
    return (
      <Layout>
        <div className="text-center py-12">
          <p className="text-muted-foreground">Teacher not found</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-4xl mx-auto animate-fade-up">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={() => navigate("/teachers")}
            className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span>Back</span>
          </button>
          <button
            onClick={handleEdit}
            className="flex items-center gap-2 bg-primary/10 hover:bg-primary/20 text-primary px-4 py-2 rounded-lg transition-colors"
          >
            <Edit className="w-4 h-4" />
            <span>Edit</span>
          </button>
        </div>

        {/* Profile Card */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-blue-500/20 to-purple-500/20 border border-border rounded-3xl shadow-2xl p-8 mb-6">
          <div className="flex flex-col items-center">
            <div className="w-24 h-24 rounded-full bg-purple-500/30 flex items-center justify-center text-white text-3xl font-bold overflow-hidden mb-4">
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
            <h1 className="text-2xl font-bold text-foreground mb-2">
              {teacher.name}
            </h1>
            <p className="text-white/80 text-sm">
              {teacher.employeeId ? `ID: ${teacher.employeeId}` : "No ID"}
            </p>
          </div>
        </div>

        {/* Personal Information Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6 mb-6">
          <h2 className="text-lg font-bold text-foreground mb-4">
            Personal Information
          </h2>
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <Mail className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Email</p>
                <p className="text-sm text-foreground">{teacher.email}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Phone className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Phone</p>
                <p className="text-sm text-foreground">
                  {teacher.phone || "N/A"}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Calendar className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">
                  Date of Birth
                </p>
                <p className="text-sm text-foreground">
                  {teacher.dob || "N/A"}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Calendar className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Join Date</p>
                <p className="text-sm text-foreground">
                  {teacher.joinDate || "N/A"}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Professional Information Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6 mb-6">
          <h2 className="text-lg font-bold text-foreground mb-4">
            Professional Information
          </h2>
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <Building2 className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Department</p>
                <p className="text-sm text-foreground">{teacher.department}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Award className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">
                  Qualification
                </p>
                <p className="text-sm text-foreground">
                  {teacher.qualification || "N/A"}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Assigned Subjects Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6">
          <h2 className="text-lg font-bold text-foreground mb-4">
            Assigned Subjects
          </h2>
          {teacher.assignedSubjectNames &&
          teacher.assignedSubjectNames.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {teacher.assignedSubjectNames.map((subject, index) => (
                <div
                  key={index}
                  className="bg-primary/10 text-primary px-3 py-1.5 rounded-full text-sm font-medium"
                >
                  {subject}
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              No subjects assigned
            </p>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default TeacherDetail;
