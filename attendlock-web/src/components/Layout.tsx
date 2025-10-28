import React from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { Home, Users, BookOpen, Calendar, Settings } from "lucide-react";
import GlassSurface from "./GlassSurface";
import logo from "../assets/image.svg?url";
import logo2 from "../assets/attendlock.svg?url";
import DarkVeil from "@/components/DarkVeil";

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { icon: Home, label: "Home", path: "/dashboard" },
    { icon: Users, label: "Teachers", path: "/teachers" },
    { icon: BookOpen, label: "Students", path: "/students" },
    { icon: Calendar, label: "Schedule", path: "/schedule" },
    { icon: Settings, label: "More", path: "/settings" },
  ];

  const isActive = (path: string) => location.pathname === path;

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-b from-[#090909] to-[#111] text-gray-100 relative overflow-hidden w-screen h-screen">
      {/* Full-screen background */}
      <div className="absolute inset-0 z-0">
        {/* Assuming DarkVeil is passed or imported here */}
        <DarkVeil />
      </div>

      <main className="flex-1 w-full px-8 pt-[100px] pb-8 z-10 flex justify-center items-start overflow-y-auto">
        <div className="w-full max-w-4xl">{children}</div>
      </main>

      <div className="fixed top-6 left-[6%] w-auto z-50">
        <GlassSurface
          width={1300}
          height={60}
          borderRadius={30}
          brightness={70}
          opacity={0.8}
          displace={15}
          distortionScale={-150}
          redOffset={5}
          greenOffset={15}
          blueOffset={25}
          mixBlendMode="screen"
          className="border border-white/10 shadow-[0_0_30px_rgba(255,255,255,0.1)] backdrop-blur-lg"
        >
          <nav className="flex items-center px-8 py-4 gap-14 justify-end w-full">
            <img src={logo2} alt="Logo" className="h-30 w-60 mr-auto" />
            {navItems.map((item) => {
              const Icon = item.icon;
              const active = isActive(item.path);

              return (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className={`flex flex-col items-center gap-1 transition-all duration-300 ease-out ${
                    active
                      ? "text-blue-400 scale-105 drop-shadow-[0_0_8px_rgba(255,0,255,0.4)]"
                      : "text-gray-400 hover:text-gray-200 hover:scale-105"
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  <span className="text-[11px] font-medium tracking-wide whitespace-nowrap">
                    {item.label}
                  </span>
                </button>
              );
            })}
          </nav>
        </GlassSurface>
      </div>

      {/* Optional subtle background effects - keep or remove based on design */}
      <div className="absolute inset-0 bg-gradient-to-t from-pink-500/5 via-transparent to-transparent pointer-events-none" />
    </div>
  );
};

export default Layout;
