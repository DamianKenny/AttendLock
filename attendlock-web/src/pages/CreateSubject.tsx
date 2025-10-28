import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { collection, addDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { GradientButton } from "@/components/ui/button2";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft } from "lucide-react";

const CreateSubject = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);

  const [formData, setFormData] = useState({
    subjectName: "",
    department: "",
    course: "",
    university: "",
    category: "",
  });

  const handleInputChange = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const subjectData = {
        subjectName: formData.subjectName,
        department: formData.department,
        course: formData.course,
        awardingUniversity: formData.university,
        category: formData.category,
        createdAt: Date.now(),
      };

      await addDoc(collection(db, "subjects"), subjectData);

      toast({
        title: "Success",
        description: "Subject created successfully!",
      });

      // Clear form
      setFormData({
        subjectName: "",
        department: "",
        course: "",
        university: "",
        category: "",
      });

      navigate("/dashboard");
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.message || "Failed to create subject",
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
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back</span>
        </button>

        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl">
          <div className="p-6">
            <h1 className="text-2xl font-bold text-foreground mb-6">
              Create Subject
            </h1>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="subjectName">Subject Name *</Label>
                <Input
                  id="subjectName"
                  value={formData.subjectName}
                  onChange={(e) =>
                    handleInputChange("subjectName", e.target.value)
                  }
                  className="bg-secondary border-border"
                  placeholder="Enter subject name"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="department">Department *</Label>
                <Input
                  id="department"
                  value={formData.department}
                  onChange={(e) =>
                    handleInputChange("department", e.target.value)
                  }
                  className="bg-secondary border-border"
                  placeholder="Enter department"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="course">Course *</Label>
                <Input
                  id="course"
                  value={formData.course}
                  onChange={(e) => handleInputChange("course", e.target.value)}
                  className="bg-secondary border-border"
                  placeholder="Enter course"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="university">Awarding University *</Label>
                <Input
                  id="university"
                  value={formData.university}
                  onChange={(e) =>
                    handleInputChange("university", e.target.value)
                  }
                  className="bg-secondary border-border"
                  placeholder="Enter awarding university"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="category">Category *</Label>
                <Input
                  id="category"
                  value={formData.category}
                  onChange={(e) =>
                    handleInputChange("category", e.target.value)
                  }
                  className="bg-secondary border-border"
                  placeholder="Enter category"
                  required
                />
              </div>

              <div className="flex gap-[17%] pt-7">
                <GradientButton
                  className="w-[34.5%]"
                  type="button"
                  onClick={() => navigate("/dashboard")}
                  size="sm"
                  disabled={loading}
                >
                  Cancel
                </GradientButton>
                <GradientButton
                  type="submit"
                  size="sm"
                  disabled={loading}
                  onClick={CreateSubject}
                >
                  {loading ? "Creating..." : "Create Subject"}
                </GradientButton>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default CreateSubject;
