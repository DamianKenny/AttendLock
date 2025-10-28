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
  User,
  MapPin,
  GraduationCap,
  Building2,
  Award,
  Loader2,
} from "lucide-react";

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
}

const StudentDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const { toast } = useToast();

  const [student, setStudent] = useState<Student | null>(
    location.state?.student || null
  );
  const [loading, setLoading] = useState(!student);

  useEffect(() => {
    if (id && !student) {
      loadStudentData();
    } else if (id) {
      refreshStudentData();
    }
  }, [id]);

  const loadStudentData = async () => {
    if (!id) return;

    setLoading(true);
    try {
      const studentDoc = await getDoc(doc(db, "users", id));
      if (studentDoc.exists()) {
        const data = studentDoc.data();
        setStudent({
          id: studentDoc.id,
          name: data.name || "Unknown",
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email || "N/A",
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
        });
      } else {
        toast({
          title: "Error",
          description: "Student not found",
          variant: "destructive",
        });
        navigate("/view-students");
      }
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to load student details",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const refreshStudentData = async () => {
    if (!id) return;

    try {
      const studentDoc = await getDoc(doc(db, "users", id));
      if (studentDoc.exists()) {
        const data = studentDoc.data();
        setStudent({
          id: studentDoc.id,
          name: data.name || "Unknown",
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email || "N/A",
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
        });
      }
    } catch (error) {
      console.error("Error refreshing student data:", error);
    }
  };

  const handleEdit = () => {
    if (student) {
      navigate(`/edit-student/${student.id}`, { state: { student } });
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

  if (!student) {
    return (
      <Layout>
        <div className="text-center py-12">
          <p className="text-muted-foreground">Student not found</p>
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
            onClick={() => navigate("/students")}
            className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span>Back</span>
          </button>
          <button
            onClick={() => navigate(`/edit-student/${id}`)}
            className="flex items-center gap-2 bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition-colors"
          >
            <Edit className="w-4 h-4" />
            <span>Edit</span>
          </button>
        </div>

        {/* Profile Card */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-teal-500/20 to-blue-500/20 border border-border rounded-3xl shadow-2xl p-8 mb-6">
          <div className="flex flex-col items-center">
            <div className="w-24 h-24 rounded-full bg-blue-500/30 flex items-center justify-center text-white text-3xl font-bold overflow-hidden mb-4">
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
            <h1 className="text-2xl font-bold text-foreground mb-2">
              {student.name}
            </h1>
            <p className="text-white/80 text-sm">{student.studentId}</p>
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
                <p className="text-sm text-foreground">{student.email}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Phone className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Phone</p>
                <p className="text-sm text-foreground">
                  {student.phone || "N/A"}
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
                  {student.dob || "N/A"}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <User className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Gender</p>
                <p className="text-sm text-foreground">
                  {student.gender || "N/A"}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <MapPin className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Address</p>
                <p className="text-sm text-foreground">
                  {student.address || "N/A"}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Academic Information Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6 mb-6">
          <h2 className="text-lg font-bold text-foreground mb-4">
            Academic Information
          </h2>
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <GraduationCap className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Programme</p>
                <p className="text-sm text-foreground">
                  {student.programme || "N/A"}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Building2 className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Faculty</p>
                <p className="text-sm text-foreground">
                  {student.faculty || "N/A"}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Award className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">Batch</p>
                <p className="text-sm text-foreground">
                  {student.batch || "N/A"}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Calendar className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">
                  Admission Date
                </p>
                <p className="text-sm text-foreground">
                  {student.admissionDate || "N/A"}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Parent/Guardian Information Card */}
        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl p-6">
          <h2 className="text-lg font-bold text-foreground mb-4">
            Parent/Guardian Information
          </h2>
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <User className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">
                  Parent/Guardian Name
                </p>
                <p className="text-sm text-foreground">
                  {student.parentName || "N/A"}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Mail className="w-5 h-5 text-muted-foreground mt-1" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground mb-1">
                  Parent/Guardian Email
                </p>
                <p className="text-sm text-foreground">
                  {student.parentEmail || "N/A"}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default StudentDetail;
