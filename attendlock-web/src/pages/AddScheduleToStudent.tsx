import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  collection,
  getDocs,
  doc,
  updateDoc,
  query,
  where,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { GradientButton } from "@/components/ui/button2";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Search, Loader2, User } from "lucide-react";

interface StudentItem {
  id: string;
  name: string;
  studentId: string;
  batch?: string;
  profileUrl?: string;
  isSelected: boolean;
}

const AddScheduleToStudent = () => {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [students, setStudents] = useState<StudentItem[]>([]);
  const [filteredStudents, setFilteredStudents] = useState<StudentItem[]>([]);
  const [batches, setBatches] = useState<string[]>([]);
  const [selectedBatch, setSelectedBatch] = useState("");
  const [selectedScheduleId, setSelectedScheduleId] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectAll, setSelectAll] = useState(false);
  const [loading, setLoading] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [batchToScheduleMap, setBatchToScheduleMap] = useState<
    Map<string, string>
  >(new Map());

  useEffect(() => {
    loadBatches();
  }, []);

  useEffect(() => {
    if (selectedBatch) {
      loadStudents();
    }
  }, [selectedBatch]);

  useEffect(() => {
    filterStudents();
  }, [searchQuery, students]);

  const loadBatches = async () => {
    setLoading(true);
    try {
      const querySnapshot = await getDocs(collection(db, "schedules"));
      const uniqueBatches = new Set<string>();
      const batchMap = new Map<string, string>();

      querySnapshot.forEach((doc) => {
        const batch = doc.data().batch;
        if (batch) {
          uniqueBatches.add(batch);
          batchMap.set(batch, doc.id);
        }
      });

      setBatches(Array.from(uniqueBatches));
      setBatchToScheduleMap(batchMap);
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to load batches",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const loadStudents = async () => {
    setLoading(true);
    try {
      const q = query(collection(db, "users"), where("role", "==", "student"));
      const querySnapshot = await getDocs(q);

      const studentsList: StudentItem[] = [];
      querySnapshot.forEach((doc) => {
        const data = doc.data();
        studentsList.push({
          id: doc.id,
          name: data.name || "Unknown",
          studentId: data.studentId || "N/A",
          batch: data.batch,
          profileUrl: data.profilePictureUrl,
          isSelected: false,
        });
      });

      setStudents(studentsList);
      setSelectedScheduleId(batchToScheduleMap.get(selectedBatch) || "");
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
    if (!searchQuery.trim()) {
      setFilteredStudents(students);
      return;
    }

    const query = searchQuery.toLowerCase();
    const filtered = students.filter(
      (student) =>
        student.name.toLowerCase().includes(query) ||
        student.studentId.toLowerCase().includes(query)
    );
    setFilteredStudents(filtered);
  };

  const handleSelectAll = (checked: boolean) => {
    setSelectAll(checked);
    setStudents(students.map((s) => ({ ...s, isSelected: checked })));
  };

  const handleStudentToggle = (studentId: string, checked: boolean) => {
    setStudents(
      students.map((s) =>
        s.id === studentId ? { ...s, isSelected: checked } : s
      )
    );
  };

  const getSelectedCount = (): number => {
    return students.filter((s) => s.isSelected).length;
  };

  const assignScheduleToStudents = async () => {
    if (!selectedBatch) {
      toast({
        title: "Error",
        description: "Please select a batch",
        variant: "destructive",
      });
      return;
    }

    const selectedStudents = students.filter((s) => s.isSelected);
    if (selectedStudents.length === 0) {
      toast({
        title: "Error",
        description: "Please select at least one student",
        variant: "destructive",
      });
      return;
    }

    setAssigning(true);

    try {
      const updatePromises = selectedStudents.map((student) =>
        updateDoc(doc(db, "users", student.id), {
          assignedScheduleId: selectedScheduleId,
          assignedBatch: selectedBatch,
        })
      );

      await Promise.all(updatePromises);

      toast({
        title: "Success",
        description: `Schedule assigned to ${selectedStudents.length} student${
          selectedStudents.length !== 1 ? "s" : ""
        }`,
      });

      navigate("/dashboard");
    } catch (error: any) {
      toast({
        title: "Error",
        description: "Failed to assign schedule",
        variant: "destructive",
      });
    } finally {
      setAssigning(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-4xl mx-auto animate-fade-up">
        {/* Header */}
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
              Assign Schedule to Students
            </h1>

            {/* Batch Selection Card */}
            <div className="bg-card border border-border rounded-2xl p-6 mb-6">
              <h2 className="text-lg font-semibold text-foreground mb-4">
                Select Batch
              </h2>
              <div className="space-y-2">
                <label className="text-xs text-muted-foreground">Batch</label>
                <Select value={selectedBatch} onValueChange={setSelectedBatch}>
                  <SelectTrigger className="bg-secondary border-border">
                    <SelectValue placeholder="Select Batch" />
                  </SelectTrigger>
                  <SelectContent>
                    {batches.map((batch) => (
                      <SelectItem key={batch} value={batch}>
                        {batch}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Students Selection Card */}
            {selectedBatch && (
              <div className="bg-card border border-border rounded-2xl p-6 mb-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-semibold text-foreground">
                    Select Students
                  </h2>
                  <p className="text-sm text-muted-foreground">
                    {getSelectedCount()} selected
                  </p>
                </div>

                {/* Search Bar */}
                <div className="relative mb-4">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    type="text"
                    placeholder="Search students..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-10 bg-secondary border-border"
                  />
                </div>

                {/* Select All Checkbox */}
                <div className="flex items-center space-x-2 mb-4">
                  <Checkbox
                    id="select-all"
                    checked={selectAll}
                    onCheckedChange={handleSelectAll}
                  />
                  <label
                    htmlFor="select-all"
                    className="text-sm text-foreground cursor-pointer"
                  >
                    Select All Students
                  </label>
                </div>

                {/* Students List */}
                {loading ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="w-8 h-8 text-primary animate-spin" />
                  </div>
                ) : filteredStudents.length === 0 ? (
                  <div className="text-center py-8">
                    <p className="text-muted-foreground">No students found</p>
                  </div>
                ) : (
                  <div className="max-h-96 overflow-y-auto space-y-2">
                    {filteredStudents.map((student) => (
                      <div
                        key={student.id}
                        className="bg-background border border-border rounded-xl p-3 flex items-center gap-3 cursor-pointer hover:border-primary transition-colors"
                        onClick={() =>
                          handleStudentToggle(student.id, !student.isSelected)
                        }
                      >
                        <Checkbox
                          checked={student.isSelected}
                          onCheckedChange={(checked) =>
                            handleStudentToggle(student.id, checked as boolean)
                          }
                          onClick={(e) => e.stopPropagation()}
                        />

                        <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400 font-semibold overflow-hidden">
                          {student.profileUrl ? (
                            <img
                              src={student.profileUrl}
                              alt={student.name}
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            student.name.charAt(0).toUpperCase()
                          )}
                        </div>

                        <div className="flex-1">
                          <p className="text-sm font-semibold text-foreground">
                            {student.name}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {student.studentId}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Assign Button */}
            {selectedBatch && (
              <GradientButton
                onClick={assignScheduleToStudents}
                disabled={assigning || getSelectedCount() === 0}
                className="w-[70.5%]"
                size="sm"
              >
                {assigning ? "Assigning..." : "Assign Schedule"}
              </GradientButton>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default AddScheduleToStudent;
