import { PageContent } from "../../components/portal";
const names = {
  news: "ข่าวสาร",
  map: "แผนที่",
  wiki: "คู่มือเกม",
  events: "กิจกรรม",
  shop: "ร้านค้า",
  account: "บัญชีผู้เล่น",
  rankings: "อันดับผู้เล่น",
  status: "สถานะบริการ",
  privacy: "ความเป็นส่วนตัว",
  terms: "เงื่อนไขบริการ",
};
export function generateStaticParams() {
  return Object.keys(names).map((section) => ({ section }));
}
export const dynamicParams = false;
export async function generateMetadata({ params }) {
  const { section } = await params;
  return { title: names[section] || "Luma" };
}
export default async function Page({ params }) {
  const { section } = await params;
  return <PageContent section={section} />;
}
