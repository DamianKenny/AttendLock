import React, { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { doc, getDoc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { GradientButton } from "@/components/ui/button2";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Loader2 } from "lucide-react";

interface StudentData {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  dob: string;
  gender: string;
  address: string;
  programme: string;
  faculty: string;
  batch: string;
  admissionDate: string;
  parentName: string;
  parentEmail: string;
}

const EditStudent = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const { toast } = useToast();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState<StudentData>({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    dob: "",
    gender: "Male",
    address: "",
    programme: "",
    faculty: "",
    batch: "",
    admissionDate: "",
    parentName: "",
    parentEmail: "",
  });

  const genders = ["Male", "Female", "Other"];

  useEffect(() => {
    loadStudentData();
  }, [id]);

  const loadStudentData = async () => {
    if (!id) {
      navigate("/view-students");
      return;
    }

    setLoading(true);
    try {
      const studentDoc = await getDoc(doc(db, "users", id));
      if (studentDoc.exists()) {
        const data = studentDoc.data();

        // Format dates for input fields (YYYY-MM-DD format)
        const formatDateForInput = (dateStr: string) => {
          if (!dateStr) return "";
          // If date is in DD/MM/YYYY format, convert to YYYY-MM-DD
          const parts = dateStr.split("/");
          if (parts.length === 3) {
            return `${parts[2]}-${parts[1].padStart(
              2,
              "0"
            )}-${parts[0].padStart(2, "0")}`;
          }
          return dateStr;
        };

        setFormData({
          firstName: data.firstName || "",
          lastName: data.lastName || "",
          email: data.email || "",
          phone: data.phone || "",
          dob: formatDateForInput(data.dob || ""),
          gender: data.gender || "Male",
          address: data.address || "",
          programme: data.programme || "",
          faculty: data.faculty || "",
          batch: data.batch || "",
          admissionDate: formatDateForInput(data.admissionDate || ""),
          parentName: data.parentName || "",
          parentEmail: data.parentEmail || "",
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
      console.error("Error loading student:", error);
      toast({
        title: "Error",
        description: "Failed to load student data",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field: keyof StudentData, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const formatDateForFirestore = (dateStr: string) => {
    if (!dateStr) return "";
    // Convert YYYY-MM-DD to DD/MM/YYYY for Firestore
    const parts = dateStr.split("-");
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
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

    if (!id) return;

    setSaving(true);

    try {
      const fullName = `${formData.firstName} ${formData.lastName}`;

      const updates = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        name: fullName,
        email: formData.email,
        phone: formData.phone,
        dob: formatDateForFirestore(formData.dob),
        gender: formData.gender,
        address: formData.address,
        programme: formData.programme,
        faculty: formData.faculty,
        batch: formData.batch,
        admissionDate: formatDateForFirestore(formData.admissionDate),
        parentName: formData.parentName,
        parentEmail: formData.parentEmail,
      };

      await updateDoc(doc(db, "users", id), updates);

      toast({
        title: "Success",
        description: "Student updated successfully",
      });

      navigate(`/student-detail/${id}`);
    } catch (error: any) {
      console.error("Error updating student:", error);
      toast({
        title: "Error",
        description: error.message || "Failed to update student",
        variant: "destructive",
      });
    } finally {
      setSaving(false);
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

  return (
    <Layout>
      <div className="max-w-3xl mx-auto animate-fade-up">
        <button
          onClick={() => navigate(`/student-detail/${id}`)}
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back</span>
        </button>

        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl">
          <div className="p-6">
            <h1 className="text-2xl font-bold text-foreground mb-6">
              Edit Student
            </h1>

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Personal Information Card */}
              <div className="bg-card border border-border rounded-2xl p-6">
                <h2 className="text-lg font-semibold text-foreground mb-4">
                  Personal Information
                </h2>

                <div className="space-y-4">
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
                      onChange={(e) =>
                        handleInputChange("email", e.target.value)
                      }
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
                      onChange={(e) =>
                        handleInputChange("phone", e.target.value)
                      }
                      className="bg-secondary border-border"
                      placeholder="Enter phone number"
                    />
                  </div>

                  {/* Date of Birth */}
                  <div className="space-y-2">
                    <Label htmlFor="dob">Date of Birth</Label>
                    <Input
                      id="dob"
                      type="date"
                      value={formData.dob}
                      onChange={(e) => handleInputChange("dob", e.target.value)}
                      className="bg-secondary border-border"
                    />
                  </div>

                  {/* Gender */}
                  <div className="space-y-2">
                    <Label htmlFor="gender">Gender</Label>
                    <Select
                      value={formData.gender}
                      onValueChange={(value) =>
                        handleInputChange("gender", value)
                      }
                    >
                      <SelectTrigger className="bg-secondary border-border">
                        <SelectValue placeholder="Select gender" />
                      </SelectTrigger>
                      <SelectContent>
                        {genders.map((gender) => (
                          <SelectItem key={gender} value={gender}>
                            {gender}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Address */}
                  <div className="space-y-2">
                    <Label htmlFor="address">Address</Label>
                    <Textarea
                      id="address"
                      value={formData.address}
                      onChange={(e) =>
                        handleInputChange("address", e.target.value)
                      }
                      className="bg-secondary border-border min-h-[80px]"
                      placeholder="Enter address"
                    />
                  </div>
                </div>
              </div>

              {/* Academic Information Card */}
              <div className="bg-card border border-border rounded-2xl p-6">
                <h2 className="text-lg font-semibold text-foreground mb-4">
                  Academic Information
                </h2>

                <div className="space-y-4">
                  {/* Programme */}
                  <div className="space-y-2">
                    <Label htmlFor="programme">Programme</Label>
                    <Input
                      id="programme"
                      value={formData.programme}
                      onChange={(e) =>
                        handleInputChange("programme", e.target.value)
                      }
                      className="bg-secondary border-border"
                      placeholder="Enter programme"
                    />
                  </div>

                  {/* Faculty */}
                  <div className="space-y-2">
                    <Label htmlFor="faculty">Faculty</Label>
                    <Input
                      id="faculty"
                      value={formData.faculty}
                      onChange={(e) =>
                        handleInputChange("faculty", e.target.value)
                      }
                      className="bg-secondary border-border"
                      placeholder="Enter faculty"
                    />
                  </div>

                  {/* Batch */}
                  <div className="space-y-2">
                    <Label htmlFor="batch">Batch</Label>
                    <Input
                      id="batch"
                      value={formData.batch}
                      onChange={(e) =>
                        handleInputChange("batch", e.target.value)
                      }
                      className="bg-secondary border-border"
                      placeholder="Enter batch"
                    />
                  </div>

                  {/* Admission Date */}
                  <div className="space-y-2">
                    <Label htmlFor="admissionDate">Admission Date</Label>
                    <Input
                      id="admissionDate"
                      type="date"
                      value={formData.admissionDate}
                      onChange={(e) =>
                        handleInputChange("admissionDate", e.target.value)
                      }
                      className="bg-secondary border-border"
                    />
                  </div>
                </div>
              </div>

              {/* Parent/Guardian Information Card */}
              <div className="bg-card border border-border rounded-2xl p-6">
                <h2 className="text-lg font-semibold text-foreground mb-4">
                  Parent/Guardian Information
                </h2>

                <div className="space-y-4">
                  {/* Parent Name */}
                  <div className="space-y-2">
                    <Label htmlFor="parentName">Parent/Guardian Name</Label>
                    <Input
                      id="parentName"
                      value={formData.parentName}
                      onChange={(e) =>
                        handleInputChange("parentName", e.target.value)
                      }
                      className="bg-secondary border-border"
                      placeholder="Enter parent/guardian name"
                    />
                  </div>

                  {/* Parent Email */}
                  <div className="space-y-2">
                    <Label htmlFor="parentEmail">Parent/Guardian Email</Label>
                    <Input
                      id="parentEmail"
                      type="email"
                      value={formData.parentEmail}
                      onChange={(e) =>
                        handleInputChange("parentEmail", e.target.value)
                      }
                      className="bg-secondary border-border"
                      placeholder="Enter parent/guardian email"
                    />
                  </div>
                </div>
              </div>

              {/* Submit button */}
              <div className="pt-4">
                <GradientButton
                  type="submit"
                  size="sm"
                  className="w-[70.5%]"
                  disabled={saving}
                >
                  {saving ? "Saving..." : "Save Student Details"}
                </GradientButton>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default EditStudent;
