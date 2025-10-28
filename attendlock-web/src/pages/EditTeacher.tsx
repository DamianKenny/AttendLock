import React, { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { doc, getDoc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { GradientButton } from "@/components/ui/button2";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Calendar } from "lucide-react";

interface Teacher {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  dob: string;
  joinDate: string;
  department: string;
  qualification: string;
}

const EditTeacher = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const { toast } = useToast();

  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<Teacher>({
    id: id || "",
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    dob: "",
    joinDate: "",
    department: "",
    qualification: "",
  });

  useEffect(() => {
    if (location.state?.teacher) {
      const teacher = location.state.teacher;
      setFormData({
        id: teacher.id,
        firstName: teacher.firstName || "",
        lastName: teacher.lastName || "",
        email: teacher.email || "",
        phone: teacher.phone || "",
        dob: teacher.dob || "",
        joinDate: teacher.joinDate || "",
        department: teacher.department || "",
        qualification: teacher.qualification || "",
      });
    } else if (id) {
      loadTeacherData();
    }
  }, [id, location.state]);

  const loadTeacherData = async () => {
    if (!id) return;

    try {
      const teacherDoc = await getDoc(doc(db, "users", id));
      if (teacherDoc.exists()) {
        const data = teacherDoc.data();
        setFormData({
          id: teacherDoc.id,
          firstName: data.firstName || "",
          lastName: data.lastName || "",
          email: data.email || "",
          phone: data.phone || "",
          dob: data.dob || "",
          joinDate: data.joinDate || "",
          department: data.department || "",
          qualification: data.qualification || "",
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
        description: "Failed to load teacher data",
        variant: "destructive",
      });
    }
  };

  const handleInputChange = (field: keyof Teacher, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const isValidEmail = (email: string) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validation
    if (!formData.firstName.trim()) {
      toast({
        title: "Validation Error",
        description: "First name is required",
        variant: "destructive",
      });
      return;
    }

    if (!formData.lastName.trim()) {
      toast({
        title: "Validation Error",
        description: "Last name is required",
        variant: "destructive",
      });
      return;
    }

    if (!formData.email.trim()) {
      toast({
        title: "Validation Error",
        description: "Email is required",
        variant: "destructive",
      });
      return;
    }

    if (!isValidEmail(formData.email)) {
      toast({
        title: "Validation Error",
        description: "Please enter a valid email address",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);

    try {
      const fullName = `${formData.firstName} ${formData.lastName}`;

      const updates = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        name: fullName,
        email: formData.email,
        phone: formData.phone,
        dob: formData.dob,
        joinDate: formData.joinDate,
        department: formData.department,
        qualification: formData.qualification,
      };

      await updateDoc(doc(db, "users", formData.id), updates);

      toast({
        title: "Success",
        description: "Teacher updated successfully",
      });

      navigate(`/teacher-detail/${formData.id}`);
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.message || "Failed to update teacher",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-3xl mx-auto animate-fade-up">
        <button
          onClick={() => navigate(`/teacher-detail/${id}`)}
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back</span>
        </button>

        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl">
          <div className="p-6">
            <h1 className="text-2xl font-bold text-foreground mb-6">
              Edit Teacher
            </h1>

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* First Name */}
              <div className="space-y-2">
                <Label htmlFor="firstName">First Name *</Label>
                <Input
                  id="firstName"
                  value={formData.firstName}
                  onChange={(e) =>
                    handleInputChange("firstName", e.target.value)
                  }
                  className="bg-secondary border-border"
                  placeholder="Enter first name"
                  required
                />
              </div>

              {/* Last Name */}
              <div className="space-y-2">
                <Label htmlFor="lastName">Last Name *</Label>
                <Input
                  id="lastName"
                  value={formData.lastName}
                  onChange={(e) =>
                    handleInputChange("lastName", e.target.value)
                  }
                  className="bg-secondary border-border"
                  placeholder="Enter last name"
                  required
                />
              </div>

              {/* Email */}
              <div className="space-y-2">
                <Label htmlFor="email">Email *</Label>
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => handleInputChange("email", e.target.value)}
                  className="bg-secondary border-border"
                  placeholder="Enter email"
                  required
                />
              </div>

              {/* Phone */}
              <div className="space-y-2">
                <Label htmlFor="phone">Phone</Label>
                <Input
                  id="phone"
                  type="tel"
                  value={formData.phone}
                  onChange={(e) => handleInputChange("phone", e.target.value)}
                  className="bg-secondary border-border"
                  placeholder="Enter phone number"
                />
              </div>

              {/* Date of Birth */}
              <div className="space-y-2">
                <Label htmlFor="dob">Date of Birth</Label>
                <div className="relative">
                  <Input
                    id="dob"
                    type="date"
                    value={formData.dob}
                    onChange={(e) => handleInputChange("dob", e.target.value)}
                    className="bg-secondary border-border"
                  />
                </div>
              </div>

              {/* Join Date */}
              <div className="space-y-2">
                <Label htmlFor="joinDate">Join Date</Label>
                <div className="relative">
                  <Input
                    id="joinDate"
                    type="date"
                    value={formData.joinDate}
                    onChange={(e) =>
                      handleInputChange("joinDate", e.target.value)
                    }
                    className="bg-secondary border-border"
                  />
                </div>
              </div>

              {/* Department */}
              <div className="space-y-2">
                <Label htmlFor="department">Department</Label>
                <Input
                  id="department"
                  value={formData.department}
                  onChange={(e) =>
                    handleInputChange("department", e.target.value)
                  }
                  className="bg-secondary border-border"
                  placeholder="Enter department"
                />
              </div>

              {/* Qualification */}
              <div className="space-y-2">
                <Label htmlFor="qualification">Qualification</Label>
                <Input
                  id="qualification"
                  value={formData.qualification}
                  onChange={(e) =>
                    handleInputChange("qualification", e.target.value)
                  }
                  className="bg-secondary border-border"
                  placeholder="Enter qualification"
                />
              </div>

              {/* Submit Button */}
              <div className="pt-4">
                <GradientButton
                  type="submit"
                  size="sm"
                  className="w-[71%]"
                  disabled={loading}
                >
                  {loading ? "Saving..." : "Save Details"}
                </GradientButton>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default EditTeacher;
