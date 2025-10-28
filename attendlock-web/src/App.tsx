import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import CreateTeacher from "./pages/CreateTeacher";
import CreateStudent from "./pages/CreateStudent";
import CreateSubject from "./pages/CreateSubject";
import ViewTeachers from "./pages/ViewTeachers";
import ViewStudents from "./pages/ViewStudents";
import ViewSchedule from "./pages/ViewSchedule";
import AssignSubjects from "./pages/AssignSubjects";
import CreateSchedule from "./pages/CreateSchedule";
import Settings from "./pages/Settings";
import NotFound from "./pages/NotFound";
import TeacherDetail from "./pages/TeacherDetail";
import EditTeacher from "./pages/EditTeacher";
import EditStudent from "./pages/EditStudent";
import ScheduleDetail from "./pages/ScheduleDetail";
import StudentDetail from "./pages/StudentDetail";
import AddScheduleToStudent from "./pages/AddScheduleToStudent";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/create-teacher" element={<CreateTeacher />} />
          <Route path="/create-student" element={<CreateStudent />} />
          <Route path="/create-subject" element={<CreateSubject />} />
          <Route path="/teachers" element={<ViewTeachers />} />
          <Route path="/students" element={<ViewStudents />} />
          <Route path="/schedule" element={<ViewSchedule />} />
          <Route path="/assign-subjects" element={<AssignSubjects />} />
          <Route path="/create-schedule" element={<CreateSchedule />} />
          <Route path="/teacher-detail/:id" element={<TeacherDetail />} />
          <Route path="/edit-teacher/:id" element={<EditTeacher />} />
          <Route path="/edit-student/:id" element={<EditStudent />} />
          <Route path="/schedule-detail/:id" element={<ScheduleDetail />} />
          <Route path="/student-detail/:id" element={<StudentDetail />} />
          <Route path="/add-schedule" element={<AddScheduleToStudent />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
