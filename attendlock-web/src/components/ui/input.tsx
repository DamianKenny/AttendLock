import * as React from "react";

import { cn } from "@/lib/utils";

const Input = React.forwardRef<HTMLInputElement, React.ComponentProps<"input">>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        ref={ref}
        {...props}
        className={cn(
          "relative w-full h-12 px-4 py-2 text-base font-semibold text-gray-50 bg-neutral-800 border border-gray-600 rounded-lg transition-all duration-500 ease-in-out overflow-hidden",
          // glowing violet orb
          "before:absolute before:content-[''] before:w-10 before:h-10 before:bg-violet-500 before:rounded-full before:blur-lg before:top-1 before:right-2 before:z-10 before:transition-all before:duration-500",
          // glowing rose orb
          "after:absolute after:content-[''] after:w-16 after:h-16 after:bg-rose-300 after:rounded-full after:blur-lg after:top-2 after:right-8 after:z-10 after:transition-all after:duration-500",
          // hover animations
          "hover:border-rose-300 hover:text-rose-300 hover:before:right-10 hover:before:-bottom-6 hover:after:-right-6 hover:after:top-6 hover:before:blur-xl hover:after:blur-xl",
          // focus effects
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-400 focus-visible:ring-offset-2",
          className
        )}
      />
    );
  }
);
Input.displayName = "Input";

export { Input };
