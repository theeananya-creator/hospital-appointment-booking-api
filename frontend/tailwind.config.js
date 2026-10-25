/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#14213d",
        brand: "#0f766e",
        "brand-dark": "#115e59",
        canvas: "#f4f7f8",
      },
      boxShadow: {
        card: "0 12px 34px rgba(20, 33, 61, 0.08)",
      },
    },
  },
  plugins: [],
};
