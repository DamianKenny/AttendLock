import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { collection, doc, setDoc } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { db, storage } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { Button } from "@/components/ui/button";
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
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Upload, Calendar as CalendarIcon } from "lucide-react";

const CreateStudent = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string>("");

  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    gender: "",
    studentId: "",
    faculty: "",
    programme: "",
    courses: "",
    batch: "",
    rollNumber: "",
    admissionDate: "",
    parentName: "",
    parentPhone: "",
    parentEmail: "",
    address: "",
    studentEmail: "",
    password: "",
    confirmPassword: "",
  });

  const faculties = [
    "School of Computing",
    "School of Engineering",
    "School of Business",
    "School of Language",
    "School of Design",
    "School of Humanities",
  ];

  const genders = ["Male", "Female", "Other"];

  const handleInputChange = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      toast({
        title: "Error",
        description: "Passwords do not match",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);

    try {
      let profilePictureUrl = "";

      if (imageFile) {
        const storageRef = ref(
          storage,
          `profile_pictures/${formData.studentId}.jpg`
        );
        await uploadBytes(storageRef, imageFile);
        profilePictureUrl = await getDownloadURL(storageRef);
      }

      const studentData = {
        role: "student",
        name: `${formData.firstName} ${formData.lastName}`,
        email: formData.studentEmail,
        phone: formData.parentPhone,
        firstName: formData.firstName,
        lastName: formData.lastName,
        dob: formData.dateOfBirth,
        gender: formData.gender,
        studentId: formData.studentId,
        faculty: formData.faculty,
        programme: formData.programme,
        courses: formData.courses,
        batch: formData.batch,
        roll: formData.rollNumber,
        admissionDate: formData.admissionDate,
        parentName: formData.parentName,
        parentEmail: formData.parentEmail,
        address: formData.address,
        password: formData.password,
        profilePictureUrl,
        createdAt: new Date().toISOString(),
      };

      await setDoc(doc(db, "users", formData.studentId), studentData);

      toast({
        title: "Success",
        description: "Student created successfully!",
      });

      navigate("/dashboard");
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.message || "Failed to create student",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-2xl mx-auto animate-fade-up">
        <button
          onClick={() => navigate("/dashboard")}
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back</span>
        </button>

        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl">
          <div className="p-6">
            <h1 className="text-2xl font-bold text-foreground mb-6">
              Create Student
            </h1>

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* ... keep existing code */}
              <div className="flex flex-col items-center gap-4">
                <div className="w-24 h-24 rounded-full bg-secondary flex items-center justify-center overflow-hidden">
                  {imagePreview ? (
                    <img
                      src={imagePreview}
                      alt="Preview"
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <Upload className="w-8 h-8 text-muted-foreground" />
                  )}
                </div>
                <Label htmlFor="photo" className="cursor-pointer">
                  <div className="px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium">
                    Select Photo
                  </div>
                  <Input
                    id="photo"
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleImageChange}
                  />
                </Label>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="firstName">First Name *</Label>
                  <Input
                    id="firstName"
                    value={formData.firstName}
                    onChange={(e) =>
                      handleInputChange("firstName", e.target.value)
                    }
                    className="bg-secondary border-border"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="lastName">Last Name *</Label>
                  <Input
                    id="lastName"
                    value={formData.lastName}
                    onChange={(e) =>
                      handleInputChange("lastName", e.target.value)
                    }
                    className="bg-secondary border-border"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="dateOfBirth">Date of Birth *</Label>
                  <Input
                    id="dateOfBirth"
                    type="date"
                    value={formData.dateOfBirth}
                    onChange={(e) =>
                      handleInputChange("dateOfBirth", e.target.value)
                    }
                    className="bg-secondary border-border"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="gender">Gender *</Label>
                  <Select
                    onValueChange={(value) =>
                      handleInputChange("gender", value)
                    }
                    required
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
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="studentId">Student ID *</Label>
                  <Input
                    id="studentId"
                    value={formData.studentId}
                    onChange={(e) =>
                      handleInputChange("studentId", e.target.value)
                    }
                    className="bg-secondary border-border"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="rollNumber">Roll Number *</Label>
                  <Input
                    id="rollNumber"
                    value={formData.rollNumber}
                    onChange={(e) =>
                      handleInputChange("rollNumber", e.target.value)
                    }
                    className="bg-secondary border-border"
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="faculty">Faculty *</Label>
                <Select
                  onValueChange={(value) => handleInputChange("faculty", value)}
                  required
                >
                  <SelectTrigger className="bg-secondary border-border">
                    <SelectValue placeholder="Select faculty" />
                  </SelectTrigger>
                  <SelectContent>
                    {faculties.map((faculty) => (
                      <SelectItem key={faculty} value={faculty}>
                        {faculty}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="programme">Programme *</Label>
                  <Input
                    id="programme"
                    value={formData.programme}
                    onChange={(e) =>
                      handleInputChange("programme", e.target.value)
                    }
                    className="bg-secondary border-border"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="batch">Batch *</Label>
                  <Input
                    id="batch"
                    value={formData.batch}
                    onChange={(e) => handleInputChange("batch", e.target.value)}
                    className="bg-secondary border-border"
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="courses">Courses *</Label>
                <Input
                  id="courses"
                  value={formData.courses}
                  onChange={(e) => handleInputChange("courses", e.target.value)}
                  className="bg-secondary border-border"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="admissionDate">Admission Date *</Label>
                <Input
                  id="admissionDate"
                  type="date"
                  value={formData.admissionDate}
                  onChange={(e) =>
                    handleInputChange("admissionDate", e.target.value)
                  }
                  className="bg-secondary border-border"
                  required
                />
              </div>

              <div className="border-t border-border pt-6">
                <h3 className="text-lg font-semibold text-foreground mb-4">
                  Parent/Guardian Information
                </h3>

                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="parentName">Parent Name *</Label>
                    <Input
                      id="parentName"
                      value={formData.parentName}
                      onChange={(e) =>
                        handleInputChange("parentName", e.target.value)
                      }
                      className="bg-secondary border-border"
                      required
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="parentPhone">Parent Phone *</Label>
                      <Input
                        id="parentPhone"
                        type="tel"
                        value={formData.parentPhone}
                        onChange={(e) =>
                          handleInputChange("parentPhone", e.target.value)
                        }
                        className="bg-secondary border-border"
                        required
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="parentEmail">Parent Email</Label>
                      <Input
                        id="parentEmail"
                        type="email"
                        value={formData.parentEmail}
                        onChange={(e) =>
                          handleInputChange("parentEmail", e.target.value)
                        }
                        className="bg-secondary border-border"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="address">Address</Label>
                <Input
                  id="address"
                  value={formData.address}
                  onChange={(e) => handleInputChange("address", e.target.value)}
                  className="bg-secondary border-border"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="studentEmail">Student Email</Label>
                <Input
                  id="studentEmail"
                  type="email"
                  value={formData.studentEmail}
                  onChange={(e) =>
                    handleInputChange("studentEmail", e.target.value)
                  }
                  className="bg-secondary border-border"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="password">Password *</Label>
                  <Input
                    id="password"
                    type="password"
                    value={formData.password}
                    onChange={(e) =>
                      handleInputChange("password", e.target.value)
                    }
                    className="bg-secondary border-border"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">Confirm Password *</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={formData.confirmPassword}
                    onChange={(e) =>
                      handleInputChange("confirmPassword", e.target.value)
                    }
                    className="bg-secondary border-border"
                    required
                  />
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
                <GradientButton type="submit" size="sm">
                  Create Student
                </GradientButton>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default CreateStudent;
