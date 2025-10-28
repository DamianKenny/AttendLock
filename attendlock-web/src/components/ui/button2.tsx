import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const gradientButtonVariants = cva(
  "relative inline-block font-semibold rounded-xl cursor-pointer transition-transform duration-300 ease-in-out select-none disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default:
          "text-white bg-gray-950 shadow-2xl shadow-zinc-900 hover:scale-105 active:scale-95",
        destructive:
          "text-white bg-red-600 shadow-lg hover:scale-105 active:scale-95",
        secondary:
          "text-gray-200 bg-gray-700 shadow hover:scale-105 active:scale-95",
      },
      size: {
        default: "px-6 py-3 text-base",
        sm: "px3 py-2 text-default",
        lg: "px-8 py-4 text-lg",
        xs: "px-1 py-0.5 text-[10px]",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
);

export interface GradientButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof gradientButtonVariants> {
  asChild?: boolean;
  children: React.ReactNode;
}

const GradientButton = React.forwardRef<HTMLButtonElement, GradientButtonProps>(
  ({ className, variant, size, asChild = false, children, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";

    return (
      <Comp
        className={cn(
          "relative group",
          gradientButtonVariants({ variant, size, className })
        )}
        ref={ref}
        {...props}
      >
        <div className="flex items-center justify-center h-4 w-[141%]">
          <div className="relative group w-full">
            <button className="relative inline-block w-full p-px font-semibold leading-6 text-white bg-gray-800 shadow-2xl cursor-pointer rounded-xl shadow-zinc-900 transition-transform duration-300 ease-in-out hover:scale-100 active:scale-95">
              {/* Gradient overlay */}
              <span className="absolute top-0 left-0 w-full h-full rounded-xl bg-gradient-to-r from-teal-400 via-blue-500 to-purple-500 p-[2px] opacity-0 transition-opacity duration-500 group-hover:opacity-100"></span>

              {/* Button content */}
              <span className="relative z-10 block w-full px-6 py-3 rounded-xl bg-gray-950">
                <div className="relative z-10 flex items-center justify-center space-x-2">
                  <span className="transition-all duration-500 group-hover:translate-x-1">
                    {children}
                  </span>
                  <svg
                    className="w-6 h-6 transition-transform duration-500 group-hover:translate-x-1"
                    data-slot="icon"
                    aria-hidden="true"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <path
                      clipRule="evenodd"
                      d="M8.22 5.22a.75.75 0 0 1 1.06 0l4.25 4.25a.75.75 0 0 1 0 1.06l-4.25 4.25a.75.75 0 0 1-1.06-1.06L11.94 10 8.22 6.28a.75.75 0 0 1 0-1.06Z"
                      fillRule="evenodd"
                    ></path>
                  </svg>
                </div>
              </span>
            </button>
          </div>
        </div>
      </Comp>
    );
  }
);

GradientButton.displayName = "GradientButton";

export { GradientButton, gradientButtonVariants };
