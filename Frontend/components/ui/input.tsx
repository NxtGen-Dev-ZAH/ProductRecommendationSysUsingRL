"use client";

import { forwardRef, InputHTMLAttributes, useState } from "react";
import { cn } from "../../utils/cn";
import { cva, type VariantProps } from "class-variance-authority";

const inputVariants = cva(
  "flex w-full rounded-2xl border bg-background px-4 py-3 text-sm transition-all duration-300 file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50",
  {
    variants: {
      variant: {
        default:
          "border-input focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus:border-primary",
        filled:
          "bg-muted border-transparent focus:bg-background focus:border-primary",
        underlined:
          "rounded-none border-t-0 border-l-0 border-r-0 border-b-2 px-0 focus:ring-0 focus:ring-offset-0",
        floating: "pt-6 pb-2 focus:pt-6 focus:pb-2",
      },
      inputSize: {
        sm: "h-9 px-3 py-2 text-sm",
        default: "h-12 px-4 py-3",
        lg: "h-14 px-6 py-4 text-base",
      },
      state: {
        default: "",
        error: "border-destructive focus-visible:ring-destructive",
        success: "border-green-500 focus-visible:ring-green-500",
      },
    },
    defaultVariants: {
      variant: "default",
      inputSize: "default",
      state: "default",
    },
  }
);

export interface InputProps
  extends InputHTMLAttributes<HTMLInputElement>,
    VariantProps<typeof inputVariants> {
  label?: string;
  error?: string;
  success?: string;
  hint?: string;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      className,
      variant,
      inputSize,
      state,
      label,
      error,
      success,
      hint,
      startIcon,
      endIcon,
      id,
      ...props
    },
    ref
  ) => {
    const inputId = id || `input-${Math.random().toString(36).substr(2, 9)}`;
    const hasError = error || state === "error";
    const hasSuccess = success || state === "success";
    const currentState = hasError
      ? "error"
      : hasSuccess
      ? "success"
      : "default";

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm font-medium text-foreground mb-2"
          >
            {label}
          </label>
        )}

        <div className="relative">
          {startIcon && (
            <div className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground">
              {startIcon}
            </div>
          )}

          <input
            id={inputId}
            className={cn(
              inputVariants({ variant, inputSize, state: currentState }),
              startIcon && "pl-10",
              endIcon && "pr-10",
              className
            )}
            ref={ref}
            {...props}
          />

          {endIcon && (
            <div className="absolute right-3 top-1/2 transform -translate-y-1/2 text-muted-foreground">
              {endIcon}
            </div>
          )}
        </div>

        {(error || success || hint) && (
          <div className="mt-2 text-sm">
            {error && (
              <p className="text-destructive flex items-center gap-1">
                <svg
                  className="w-4 h-4"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                    clipRule="evenodd"
                  />
                </svg>
                {error}
              </p>
            )}
            {success && !error && (
              <p className="text-green-600 flex items-center gap-1">
                <svg
                  className="w-4 h-4"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                    clipRule="evenodd"
                  />
                </svg>
                {success}
              </p>
            )}
            {hint && !error && !success && (
              <p className="text-muted-foreground">{hint}</p>
            )}
          </div>
        )}
      </div>
    );
  }
);

Input.displayName = "Input";

// Password Input Component with Eye Toggle
export type PasswordInputProps = Omit<InputProps, "type" | "endIcon">;

const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  ({ className, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false);

    const togglePasswordVisibility = () => {
      setShowPassword(!showPassword);
    };

    const eyeIcon = showPassword ? (
      <svg
        className="w-5 h-5 cursor-pointer hover:text-primary transition-colors"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
        xmlns="http://www.w3.org/2000/svg"
        onClick={togglePasswordVisibility}
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21"
        />
      </svg>
    ) : (
      <svg
        className="w-5 h-5 cursor-pointer hover:text-primary transition-colors"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
        xmlns="http://www.w3.org/2000/svg"
        onClick={togglePasswordVisibility}
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
        />
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
        />
      </svg>
    );

    return (
      <Input
        {...props}
        ref={ref}
        type={showPassword ? "text" : "password"}
        endIcon={eyeIcon}
        className={className}
      />
    );
  }
);

PasswordInput.displayName = "PasswordInput";

export { Input, inputVariants, PasswordInput };
