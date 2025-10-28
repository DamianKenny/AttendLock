import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { collection, getDocs, doc, getDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import Layout from "@/components/Layout";
import { PiChalkboardTeacher, PiStudent } from "react-icons/pi";
import {
  MdFormatListBulletedAdd,
  MdOutlineSubject,
  MdOutlineAddAlarm,
} from "react-icons/md";
import { GrSchedules } from "react-icons/gr";
import {
  UserPlus,
  Users,
  Calendar,
  BookOpen,
  Settings,
  Bell,
  QrCode,
  UserCog,
  CalendarCheck,
  SquareUserRound,
} from "lucide-react";
import SpotlightCard from "@/components/SpotlightCard";

const Dashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalStudents: 0,
    totalTeachers: 0,
    avgAttendance: "—",
  });

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const usersSnap = await getDocs(collection(db, "users"));
      let studentCount = 0;
      let teacherCount = 0;

      usersSnap.forEach((doc) => {
        const data = doc.data();
        if (data.role === "student") studentCount++;
        if (data.role === "teacher") teacherCount++;
      });

      const attendanceDoc = await getDoc(
        doc(db, "attendance_stats", "overall")
      );
      const avgAttendance = attendanceDoc.exists()
        ? attendanceDoc.data().average + "%"
        : "—";

      setStats({
        totalStudents: studentCount,
        totalTeachers: teacherCount,
        avgAttendance,
      });
    } catch (error) {
      console.error("Error loading stats:", error);
    }
  };

  const adminCards = [
    {
      title: "Add Teacher",
      icon: UserPlus,
      color: "rgba(255, 165, 0, 0.25)",
      path: "/create-teacher",
    },
    {
      title: "Add Student",
      icon: Users,
      color: "rgba(0, 123, 255, 0.25)",
      path: "/create-student",
    },
    {
      title: "View Teachers",
      icon: PiChalkboardTeacher,
      color: "rgba(76, 175, 80, 0.25)",
      path: "/teachers",
    },
    {
      title: "View Students",
      icon: PiStudent,
      color: "rgba(255, 99, 132, 0.25)",
      path: "/students",
    },
    {
      title: "View Schedule",
      icon: CalendarCheck,
      color: "rgba(153, 102, 255, 0.25)",
      path: "/schedule",
    },
    {
      title: "Add Subjects to Teachers",
      icon: MdFormatListBulletedAdd,
      color: "rgba(255, 206, 86, 0.25)",
      path: "/assign-subjects",
    },
    {
      title: "Create Subject",
      icon: MdOutlineSubject,
      color: "rgba(54, 162, 235, 0.25)",
      path: "/create-subject",
    },
    {
      title: "Create Schedule",
      icon: GrSchedules,
      color: "rgba(255, 159, 64, 0.25)",
      path: "/create-schedule",
    },
  ];

  const quickActions = [
    {
      title: "System Settings",
      subtitle: "Manage settings, holidays, timings",
      icon: Settings,
      path: "/settings",
    },
  ];

  const scheduleCard = [
    {
      title: "Add schedule to Student",
      subtitle: "Add batch schedules to students",
      icon: MdOutlineAddAlarm,
      color: "rgba(255, 165, 0, 0.25)",
      path: "/add-schedule",
    },
  ];

  return (
    <Layout>
      <div className="relative z-10 space-y-4 animate-fade-up w-full h-full">
        <div className="flex items-center gap-4 p-4 justify-start w-full ml-[-13rem]">
          <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center">
            <UserCog className="w-6 h-6 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-foreground">Hello, Admin</h1>
            <p className="text-sm text-muted-foreground">
              {new Date().toLocaleString("en-US", {
                weekday: "long",
                day: "numeric",
                month: "long",
                hour: "2-digit",
                minute: "2-digit",
                hour12: true,
                timeZoneName: "short",
              })}
            </p>
          </div>
        </div>

        <div className="bg-card/50 border border-border rounded-3xl shadow-xl p-4">
          <h2 className="text-lg font-semibold text-foreground mb-4">
            Overall Statistics
          </h2>
          <div className="grid grid-cols-3 gap-4">
            <div className="text-center">
              <div className="text-3xl font-bold text-primary">
                {stats.totalStudents}
              </div>
              <div className="text-xs text-muted-foreground mt-1">Students</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-primary">
                {stats.totalTeachers}
              </div>
              <div className="text-xs text-muted-foreground mt-1">Teachers</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-primary">
                {stats.avgAttendance}
              </div>
              <div className="text-xs text-muted-foreground mt-1">
                Attendance
              </div>
            </div>
          </div>
        </div>
        <div>
          <h2 className="text-xl font-semibold text-foreground mb-4 p-4">
            Admin Panel
          </h2>
          <div className="grid grid-cols-2 gap-6 p-2">
            {adminCards.map((card) => {
              const Icon = card.icon;
              return (
                <button
                  key={card.title}
                  onClick={() => navigate(card.path)}
                  className="transform transition-transform duration-300 hover:scale-105"
                >
                  <SpotlightCard
                    spotlightColor={card.color}
                    className="p-9 flex flex-col items-center justify-center rounded-2xl w-full"
                  >
                    <Icon className="w-8 h-9 text-white" />
                    <span className="text-white font-semibold text-center text-sm mt-3">
                      {card.title}
                    </span>
                  </SpotlightCard>
                </button>
              );
            })}
          </div>
        </div>
        <div className="grid gap-3 p-2 mt-[-8px]">
          {scheduleCard.map((schedulecard) => {
            const Icon = schedulecard.icon;
            return (
              <button
                key={schedulecard.title}
                onClick={() => navigate(schedulecard.path)}
                className="transform transition-transform duration-300 hover:scale-105"
              >
                <SpotlightCard
                  spotlightColor={schedulecard.color}
                  className="p-9 flex flex-col items-center justify-center rounded-2xl w-100"
                >
                  <Icon className="w-8 h-9 text-white" />
                  <span className="text-white font-semibold text-center text-sm mt-3">
                    {schedulecard.title}
                  </span>
                </SpotlightCard>
              </button>
            );
          })}
        </div>
        <div>
          <h2 className="text-xl font-semibold text-foreground mb-4 p-4">
            Quick Actions
          </h2>
          <div className="space-y-3 p-4">
            {quickActions.map((action) => {
              const Icon = action.icon;
              return (
                <button
                  key={action.title}
                  onClick={() => navigate(action.path)}
                  className="w-full group"
                >
                  <div className="backdrop-blur-xl bg-card/50 border border-border rounded-2xl shadow-xl">
                    <div className="p-4 flex items-center gap-4 transition-transform group-hover:translate-x-2">
                      <div className="w-12 h-12 rounded-full bg-secondary flex items-center justify-center">
                        <Icon className="w-6 h-6 text-primary" />
                      </div>
                      <div className="flex-1 text-left">
                        <div className="font-semibold text-foreground">
                          {action.title}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {action.subtitle}
                        </div>
                      </div>
                      <div className="text-muted-foreground">›</div>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Dashboard;
