import "./globals.css";
import { AppFrame } from "../components/portal";
export const metadata = {
  title: { default: "Luma • Fantasy Town", template: "%s | Luma" },
  description:
    "เมือง Minecraft แฟนตาซี คู่มือ แผนที่ กิจกรรม และศูนย์บัญชีผู้เล่น",
  icons: { icon: "/assets/favicon.svg" },
};
export default function Layout({ children }) {
  return (
    <html lang="th" data-theme="dark" suppressHydrationWarning>
      <body>
        <AppFrame>{children}</AppFrame>
      </body>
    </html>
  );
}
