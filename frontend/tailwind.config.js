import withMT from "@material-tailwind/react/utils/withMT";

export default withMT({
  darkMode: "class",
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "./node_modules/@material-tailwind/react/components/**/*.{js,jsx,ts,tsx}",
    "./node_modules/@material-tailwind/react/theme/components/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Slate colors palette (optimized for dark mode)
        slate: {
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
          950: '#020617',
        },
        // Accent colors
        accent: {
          blue: '#3b82f6',         // blue-500
          'blue-light': '#60a5fa', // blue-400
          'blue-dark': '#2563eb',  // blue-600
        }
        
      },
      backgroundColor: {
        // Custom background utilities
        'primary-light': '#ffffff',
        'primary-dark': '#0f172a',
        'secondary-light': '#f9fafb',
        'secondary-dark': '#1e293b',
        'tertiary-light': '#f3f4f6',
        'tertiary-dark': '#334155',
      },
      textColor: {
        // Custom text utilities
        'primary-light': '#111827',
        'primary-dark': '#f1f5f9',
        'secondary-light': '#4b5563',
        'secondary-dark': '#cbd5e1',
        'tertiary-light': '#9ca3af',
        'tertiary-dark': '#94a3b8',
      },
      borderColor: {
        // Custom border utilities
        'primary-light': '#e5e7eb',
        'primary-dark': '#334155',
      },
    },
  },
  plugins: [],
});