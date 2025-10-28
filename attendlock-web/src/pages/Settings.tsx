import React from "react";
import { useNavigate } from "react-router-dom";
import Layout from "@/components/Layout";
import { ArrowLeft } from "lucide-react";

const Settings = () => {
  const navigate = useNavigate();

  return (
    <Layout>
      <div className="max-w-4xl mx-auto animate-fade-up">
        <button
          onClick={() => navigate("/dashboard")}
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back</span>
        </button>

        <div className="backdrop-blur-xl bg-card/50 border border-border rounded-3xl shadow-2xl">
          <div className="p-8 text-center">
            <h1 className="text-2xl font-bold text-foreground mb-4">
              Settings
            </h1>
            <p className="text-muted-foreground">
              This page is under construction
            </p>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Settings;
