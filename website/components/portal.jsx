"use client";
import { createContext, useContext, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  AnimatePresence,
  motion,
  MotionConfig,
  useReducedMotion,
} from "motion/react";
import { zones, news, products, guides, serviceChecks } from "../lib/content";

const Portal = createContext(null);
const nav = [
  ["/", "หน้าหลัก"],
  ["/news/", "ข่าวสาร"],
  ["/map/", "แผนที่"],
  ["/wiki/", "คู่มือเกม"],
  ["/events/", "กิจกรรม"],
  ["/shop/", "ร้านค้า"],
];
const asset = (name) => `/assets/${name}`;
const iconPaths = {
  gem: "M12 2 3 9l9 13 9-13z M3 9h18 M8 9l4 13 4-13 M8 9l4-7 4 7",
  chest: "M3 7h18v14H3z M3 12h18 M10 10h4v5h-4z M5 3h14v4",
  sword: "M16 2h6v6L10 20l-6-6z M7 17l-5 5 M2 12l10 10",
  book: "M12 6c-3-3-6-3-10-2v16c4-1 7-1 10 2 3-3 6-3 10-2V4c-4-1-7-1-10 2z M12 6v16 M5 9h4 M15 9h4 M5 13h4 M15 13h4",
  pick: "M3 3h14l4 4-3 3-4-4H3z M13 6 3 22",
  banner: "M5 22V2h14v13l-7-3-7 3",
  portal: "M5 22V4l7-2 7 2v18 M9 22V8h6v14",
};
function Icon({ kind = "gem" }) {
  return (
    <svg
      className="pixel-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinejoin="miter"
      aria-hidden="true"
    >
      <path d={iconPaths[kind] || iconPaths.gem} />
    </svg>
  );
}
function Logo({ className = "brand-logo" }) {
  return (
    <img
      className={className}
      src={asset("luma-logo.webp")}
      alt="Luma Fantasy Town"
      width="400"
      height="160"
    />
  );
}
function Head({ label, title, children }) {
  return (
    <div className="page-head">
      <p className="meta">{label}</p>
      <h1>{title}</h1>
      <p>{children}</p>
    </div>
  );
}
function Reveal({ children, className = "", delay = 0 }) {
  const { reduced } = useContext(Portal);
  return (
    <motion.section
      className={className}
      initial={reduced ? false : { opacity: 0, y: 18 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.08 }}
      transition={{
        duration: reduced ? 0 : 0.45,
        delay,
        ease: [0.22, 1, 0.36, 1],
      }}
    >
      {children}
    </motion.section>
  );
}
function useFeed(path, fallback) {
  const [feed, setFeed] = useState({
    items: fallback,
    live: false,
    loading: true,
  });
  useEffect(() => {
    const controller = new AbortController();
    fetch(`${process.env.NEXT_PUBLIC_API_BASE || ""}/api/${path}`, {
      signal: controller.signal,
    })
      .then(async (res) => {
        if (!res.ok) throw new Error("unavailable");
        return res.json();
      })
      .then((data) =>
        setFeed({
          items: data.items.length ? data.items : fallback,
          live: data.items.length > 0,
          loading: false,
        }),
      )
      .catch((e) => {
        if (e.name !== "AbortError")
          setFeed({ items: fallback, live: false, loading: false });
      });
    return () => controller.abort();
  }, [path, fallback]);
  return feed;
}
function useHealth() {
  const [health, setHealth] = useState(null);
  useEffect(() => {
    const c = new AbortController();
    fetch(`${process.env.NEXT_PUBLIC_API_BASE || ""}/api/health`, {
      signal: c.signal,
    })
      .then((r) => r.json())
      .then(setHealth)
      .catch(() =>
        setHealth({
          database: "unavailable",
          game: "unconfigured",
          payment: "unconfigured",
        }),
      );
    return () => c.abort();
  }, []);
  return health;
}

export function AppFrame({ children }) {
  const pathname = usePathname(),
    router = useRouter();
  const osReduce = useReducedMotion();
  const [theme, setTheme] = useState("dark"),
    [reduce, setReduce] = useState(false),
    [menu, setMenu] = useState(false),
    [popup, setPopup] = useState(null),
    [query, setQuery] = useState(""),
    [activeZone, setActiveZone] = useState("spawn"),
    [toast, setToast] = useState("");
  const dialogRef = useRef(null),
    toastTimer = useRef(null);
  const reduced = reduce || Boolean(osReduce);
  useEffect(() => {
    try {
      setTheme(localStorage.getItem("luma-theme") || "dark");
      setReduce(localStorage.getItem("luma-motion") === "reduce");
    } catch {}
  }, []);
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try {
      localStorage.setItem("luma-theme", theme);
    } catch {}
  }, [theme]);
  useEffect(() => {
    document.documentElement.dataset.motion = reduced ? "reduce" : "normal";
    try {
      localStorage.setItem("luma-motion", reduce ? "reduce" : "normal");
    } catch {}
  }, [reduce, reduced]);
  useEffect(() => {
    setMenu(false);
  }, [pathname]);
  useEffect(() => {
    const d = dialogRef.current;
    if (popup && !d.open) d.showModal();
    else if (!popup && d.open) d.close();
    return undefined;
  }, [popup]);
  useEffect(() => {
    const onKey = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setQuery("");
        setPopup({ type: "search" });
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, []);
  useEffect(() => () => clearTimeout(toastTimer.current), []);
  function showToast(text) {
    setToast(text);
    clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(""), 3500);
  }
  function goZone(id) {
    setActiveZone(id);
    setPopup(null);
    router.push("/map/");
  }
  const context = {
    theme,
    setTheme,
    reduce,
    setReduce,
    reduced,
    open: setPopup,
    goZone,
    activeZone,
    setActiveZone,
    showToast,
  };
  const result = query.trim().toLowerCase();
  const hits = [
    ...zones.map((z) => ({
      id: z.id,
      type: "zone",
      name: z.name,
      description: z.services.join(" · "),
      label: "สถานที่",
    })),
    ...guides.map((g) => ({
      id: g.id,
      type: "guide",
      name: g.name,
      description: g.summary,
      label: "คู่มือ",
    })),
    ...products.map((p) => ({
      id: p.id,
      type: "product",
      name: p.name,
      description: p.type,
      label: "สินค้า",
    })),
  ]
    .filter((i) => `${i.name} ${i.description}`.toLowerCase().includes(result))
    .slice(0, 10);
  return (
    <Portal.Provider value={context}>
      <MotionConfig
        reducedMotion={reduced ? "always" : "user"}
        transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
      >
        <a className="skip" href="#main">
          ข้ามไปเนื้อหา
        </a>
        <header className="nav">
          <Link className="wordmark" href="/" aria-label="Luma หน้าหลัก">
            <Logo />
          </Link>
          <button
            className="icon-button menu-toggle"
            aria-label={menu ? "ปิดเมนู" : "เปิดเมนู"}
            aria-expanded={menu}
            aria-controls="primary-nav"
            onClick={() => setMenu(!menu)}
          >
            <motion.span animate={{ rotate: menu ? 90 : 0 }}>
              {menu ? "×" : "☰"}
            </motion.span>
          </button>
          <nav
            id="primary-nav"
            className={menu ? "open" : ""}
            aria-label="เมนูหลัก"
          >
            {nav.map(([href, name]) => (
              <Link
                key={href}
                href={href}
                aria-current={pathname === href ? "page" : undefined}
              >
                {name}
              </Link>
            ))}
          </nav>
          <div className="nav-actions">
            <button
              className="icon-button"
              aria-label="ค้นหาทั้งเว็บ"
              onClick={() => {
                setQuery("");
                setPopup({ type: "search" });
              }}
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                aria-hidden="true"
              >
                <circle cx="10" cy="10" r="6" />
                <path d="m15 15 6 6" />
              </svg>
            </button>
            <button
              className="text-button"
              aria-label="สลับโหมดสี"
              onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
            >
              {theme === "dark" ? "โหมดสว่าง" : "โหมดมืด"}
            </button>
            <Link className="account-link" href="/account/">
              บัญชีของฉัน
            </Link>
          </div>
        </header>
        <main id="main" tabIndex={-1}>
          {children}
        </main>
        <footer>
          <Link className="wordmark" href="/">
            <Logo />
          </Link>
          <div>
            <p>สร้างเรื่องราวของคุณในเมือง Luma</p>
            <div className="footer-links">
              <Link href="/status/">สถานะบริการ</Link>
              <Link href="/rankings/">อันดับผู้เล่น</Link>
              <Link href="/wiki/">คู่มือและกติกา</Link>
              <Link href="/privacy/">ความเป็นส่วนตัว</Link>
              <Link href="/terms/">เงื่อนไขบริการ</Link>
            </div>
          </div>
          <p className="fine">
            เว็บฉบับพัฒนา • ข้อมูลเกมและการชำระเงินยังไม่เปิดบริการ
          </p>
          <p className="fine">
            บริการอิสระ ไม่ใช่ผลิตภัณฑ์ทางการของ Minecraft หรือ Mojang
          </p>
        </footer>
        <dialog
          ref={dialogRef}
          aria-labelledby="dialog-title"
          onCancel={() => setPopup(null)}
          onClick={(e) => {
            if (e.target !== e.currentTarget) return;
            const r = e.currentTarget.getBoundingClientRect();
            if (
              e.clientX < r.left ||
              e.clientX > r.right ||
              e.clientY < r.top ||
              e.clientY > r.bottom
            )
              setPopup(null);
          }}
        >
          <div className="dialog-top">
            <span>LUMA</span>
            <button aria-label="ปิดหน้าต่าง" onClick={() => setPopup(null)}>
              ปิด
            </button>
          </div>
          <AnimatePresence mode="wait">
            {popup && (
              <motion.div
                key={`${popup.type}-${popup.id || ""}`}
                id="dialog-content"
                initial={{ opacity: 0, y: reduced ? 0 : 9 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                transition={{ duration: reduced ? 0 : 0.18 }}
              >
                {popup.type === "search" ? (
                  <>
                    <h2 id="dialog-title">ค้นหาใน Luma</h2>
                    <div className="field">
                      <label htmlFor="global-query">
                        สถานที่ ระบบ หรือไอเทม
                      </label>
                      <input
                        autoFocus
                        id="global-query"
                        type="search"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder="ธนาคาร ซ่อม เควส Halo"
                      />
                    </div>
                    <div className="global-results">
                      {hits.length ? (
                        hits.map((i) => (
                          <button
                            key={`${i.type}-${i.id}`}
                            onClick={() =>
                              i.type === "zone"
                                ? goZone(i.id)
                                : setPopup({ type: i.type, id: i.id })
                            }
                          >
                            <span>
                              {i.name}
                              <small>{i.description}</small>
                            </span>
                            <span className="meta">{i.label}</span>
                          </button>
                        ))
                      ) : (
                        <div className="empty">
                          ไม่พบรายการ ลองใช้ชื่อบริการอื่น
                        </div>
                      )}
                    </div>
                  </>
                ) : (
                  <PopupContent popup={popup} />
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </dialog>
        <AnimatePresence>
          {toast && (
            <motion.div
              id="react-toast"
              role="status"
              aria-live="polite"
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 12 }}
            >
              {toast}
            </motion.div>
          )}
        </AnimatePresence>
      </MotionConfig>
    </Portal.Provider>
  );
}

function PopupContent({ popup }) {
  const { goZone } = useContext(Portal);
  if (popup.type === "guide") {
    const g = guides.find((g) => g.id === popup.id);
    return (
      <article className="article">
        <p className="meta">{g.category}</p>
        <h2 id="dialog-title">{g.name}</h2>
        {g.body.map((p) => (
          <p key={p}>{p}</p>
        ))}
        {g.commands.length > 0 && (
          <>
            <div className="command-list">
              {g.commands.map((c) => (
                <code key={c}>{c}</code>
              ))}
            </div>
            <p className="fine">คำสั่งที่เสนอสำหรับระบบจริง</p>
          </>
        )}
        {g.zone && (
          <button className="button secondary" onClick={() => goZone(g.zone)}>
            ดูโซนบริการ
          </button>
        )}
      </article>
    );
  }
  if (popup.type === "news") {
    const n = popup.item || news.find((n) => n.id === popup.id);
    return (
      <article className="article">
        <p className="meta">{n.date}</p>
        <h2 id="dialog-title">{n.title}</h2>
        {n.body.map((p) => (
          <p key={p}>{p}</p>
        ))}
      </article>
    );
  }
  if (popup.type === "product") {
    const p = popup.item || products.find((p) => p.id === popup.id);
    return (
      <>
        <h2 id="dialog-title">{p.name}</h2>
        <img className="modal-product" src={asset(p.image)} alt={p.name} />
        <p>{p.description}</p>
        <p>
          {popup.live ? "ราคา" : "ราคาเสนอ"} ฿{p.price} / ชำระครั้งเดียว
        </p>
        <p>
          เมื่อเปิดบริการ: ยืนยันบัญชีเกม → ชำระผ่านผู้ให้บริการ → ตรวจรับเงิน →
          ส่งของอัตโนมัติ
        </p>
        <button className="button" disabled>
          ยังไม่เปิดรับชำระ
        </button>
      </>
    );
  }
  if (popup.type === "image") {
    const z = zones.find((z) => z.id === popup.id);
    return (
      <>
        <h2 id="dialog-title">{z.name}</h2>
        <img src={asset(z.image)} alt={z.name} />
        <p>ภาพคอนเซปต์ ใช้แปลนพิกัดและขนาดจริงในการสร้าง</p>
      </>
    );
  }
  return (
    <>
      <h2 id="dialog-title">เตรียมพบกันที่ Luma</h2>
      <p>เซิร์ฟอยู่ระหว่างพัฒนา ยังไม่มี IP ที่เปิดให้เข้าเล่น</p>
      <p>
        เป้าหมายคือ Minecraft Java 1.16.5 ขึ้นไป
        รุ่นที่รองรับจริงจะประกาศหลังทดสอบ
      </p>
    </>
  );
}
export function PageContent({ section }) {
  const { reduced } = useContext(Portal);
  const pages = {
    home: Home,
    map: MapPage,
    news: NewsPage,
    wiki: WikiPage,
    events: EventsPage,
    shop: ShopPage,
    account: AccountPage,
    rankings: RankingsPage,
    status: StatusPage,
    privacy: () => <Legal kind="privacy" />,
    terms: () => <Legal kind="terms" />,
  };
  const View = pages[section] || Home;
  return (
    <motion.div
      key={section}
      initial={reduced ? false : { opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: reduced ? 0 : 0.38, ease: [0.22, 1, 0.36, 1] }}
    >
      <View />
    </motion.div>
  );
}

function Home() {
  const { open, goZone, reduced } = useContext(Portal);
  return (
    <div className="portal-shell">
      <section className="town-hero">
        <motion.img
          className="hero-world"
          src={asset("city.webp")}
          alt="ภาพคอนเซปต์เมือง Minecraft Luma พร้อมปราสาทและคริสตัล"
          fetchPriority="high"
          initial={false}
          animate={{ scale: reduced ? 1 : [1, 1.035, 1] }}
          transition={{ duration: 24, repeat: Infinity, ease: "easeInOut" }}
        />
        <div className="hero-shade" />
        <div className="hero-content">
          <p className="eyebrow">
            <span className="square-mark" /> MINECRAFT · FANTASY · CASUAL
            ROLEPLAY
          </p>
          <motion.div
            initial={false}
            animate={{ y: reduced ? 0 : [0, -4, 0] }}
            transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
          >
            <Logo className="hero-brand" />
          </motion.div>
          <h1>
            เรื่องราวใหม่
            <br />
            เริ่มที่เมืองของเรา
          </h1>
          <p>
            ออกผจญภัย เปิดร้าน สร้างกิลด์ หรือพักตกปลากับเพื่อน
            <br />
            เลือกเป็นตัวเองได้ในโลกแฟนตาซีของ Luma
          </p>
          <div className="actions">
            <motion.button
              className="button"
              whileTap={{ scale: 0.98 }}
              onClick={() => open({ type: "join" })}
            >
              <Icon kind="portal" /> วิธีเข้าเล่น
            </motion.button>
            <Link className="hero-map-link" href="/map/">
              เปิดแผนที่เมือง
            </Link>
          </div>
        </div>
        <div className="hero-side">
          <span className="map-label">THE CRYSTAL DISTRICT</span>
          <span>ภาพแนวทางการสร้างเมือง</span>
        </div>
        <div className="hero-foot">
          <div>
            <span className="meta">EDITION</span>
            <strong>Java 1.16.5+</strong>
            <small>รุ่นเป้าหมาย</small>
          </div>
          <div>
            <span className="meta">PLAY YOUR WAY</span>
            <strong>Roleplay แบบสบาย ๆ</strong>
            <small>ไม่บังคับใช้เสียง</small>
          </div>
          <Link href="/status/">
            <span className="meta">SERVER</span>
            <strong>อยู่ระหว่างพัฒนา</strong>
            <small>ดูสถานะบริการ</small>
          </Link>
        </div>
      </section>
      <nav className="quick-dock" aria-label="ทางลัดระบบ">
        {[
          ["map", "portal", "แผนที่เมือง", "ค้นหาทั้ง 12 โซน"],
          ["wiki", "book", "คู่มือผู้เล่น", "เริ่มเล่นและระบบเกม"],
          ["events", "banner", "กิจกรรม", "ติดตามแผนกิจกรรม"],
          ["shop", "gem", "ร้านค้าของตกแต่ง", "ดูชุดไอเทมของ Luma"],
        ].map(([id, icon, name, hint]) => (
          <Link href={`/${id}/`} key={id}>
            <Icon kind={icon} />
            <span>
              {name}
              <small>{hint}</small>
            </span>
          </Link>
        ))}
      </nav>
      <div className="hub-layout">
        <div>
          <Reveal>
            <div className="section-heading">
              <div>
                <p className="meta">TOWN JOURNAL</p>
                <h2>ข่าวจากเมือง</h2>
              </div>
              <Link className="text-link" href="/news/">
                ข่าวทั้งหมด
              </Link>
            </div>
            <div className="journal-feature">
              <img
                src={asset("03-quest-lodge.webp")}
                alt="กระท่อมนักผจญภัย Minecraft"
                loading="lazy"
              />
              <div>
                <span className="tag">ชุมชน</span>
                <h3>
                  มีบทบาทให้เล่น
                  <br />
                  มีพื้นที่ให้เป็นตัวเอง
                </h3>
                <p>
                  เป็นนักผจญภัย พ่อค้า
                  หรือนักตกปลาได้โดยไม่ต้องเล่นตามบทตลอดเวลา
                </p>
                <button
                  className="text-button"
                  onClick={() => open({ type: "news", id: "rp" })}
                >
                  อ่านแนวทาง Roleplay
                </button>
              </div>
            </div>
            <div className="brief-list">
              {news
                .filter((n) => n.id !== "rp")
                .map((n) => (
                  <article className="brief-row" key={n.id}>
                    <span>{n.category}</span>
                    <h3>{n.title}</h3>
                    <button
                      onClick={() => open({ type: "news", id: n.id })}
                      aria-label={`อ่าน ${n.title}`}
                    >
                      อ่าน
                    </button>
                  </article>
                ))}
            </div>
          </Reveal>
          <Reveal className="systems-section">
            <div className="section-heading">
              <div>
                <p className="meta">CITY SERVICES</p>
                <h2>ทุกเส้นทางมีเรื่องให้ทำ</h2>
              </div>
              <Link className="text-link" href="/wiki/">
                ดูระบบทั้งหมด
              </Link>
            </div>
            <div className="service-grid">
              {["bank", "forge", "quest", "market"].map((id) => {
                const z = zones.find((z) => z.id === id);
                return (
                  <motion.button
                    className="service-tile"
                    key={id}
                    onClick={() => goZone(id)}
                    whileHover={reduced ? {} : { y: -3 }}
                  >
                    <img src={asset(z.image)} alt={z.name} loading="lazy" />
                    <div>
                      <Icon
                        kind={
                          id === "forge"
                            ? "sword"
                            : id === "quest"
                              ? "book"
                              : "chest"
                        }
                      />
                      <h3>{z.name}</h3>
                      <p>{z.services.slice(0, 2).join(" · ")}</p>
                    </div>
                  </motion.button>
                );
              })}
            </div>
          </Reveal>
        </div>
        <aside className="hub-aside">
          <Reveal className="panel get-started" delay={0.06}>
            <p className="meta">YOUR FIRST ADVENTURE</p>
            <h2>
              มาเป็นส่วนหนึ่ง
              <br />
              ของเมือง
            </h2>
            <ol className="step-list">
              {[
                ["เตรียม Minecraft Java", "รุ่นเป้าหมาย 1.16.5 ขึ้นไป"],
                ["อ่านคู่มือเริ่มเล่น", "รู้จักเมืองและกติกา"],
                ["ติดตามวันเปิดเซิร์ฟ", "IP จะประกาศเมื่อพร้อม"],
              ].map(([name, hint], i) => (
                <li key={name}>
                  <span>0{i + 1}</span>
                  <div>
                    {name}
                    <small>{hint}</small>
                  </div>
                </li>
              ))}
            </ol>
            <Link className="button secondary" href="/wiki/">
              เปิดคู่มือผู้เล่น
            </Link>
          </Reveal>
          <Reveal className="panel">
            <div className="panel-heading">
              <h2>สถานะบริการ</h2>
              <Link href="/status/">รายละเอียด</Link>
            </div>
            {[
              ["เซิร์ฟเกม", "รอเปิดบริการ"],
              ["แผนที่สด", "รอเชื่อมต่อ"],
              ["ร้านค้า", "ยังไม่รับชำระ"],
            ].map(([name, state]) => (
              <div className="status-line" key={name}>
                <span>{name}</span>
                <span className="state pending">{state}</span>
              </div>
            ))}
          </Reveal>
          <Reveal className="guild-banner">
            <span className="meta">FIND YOUR PARTY</span>
            <h2>
              การผจญภัย
              <br />
              ดีกว่าเมื่อมีเพื่อน
            </h2>
            <p>รู้จักปาร์ตี้ กิลด์ และเควสของเมือง</p>
            <button
              className="text-button"
              onClick={() => open({ type: "guide", id: "quest" })}
            >
              อ่านเรื่องเควสและปาร์ตี้
            </button>
          </Reveal>
        </aside>
      </div>
    </div>
  );
}

function MapPage() {
  const { activeZone, setActiveZone, open, showToast } = useContext(Portal);
  const [query, setQuery] = useState(""),
    [type, setType] = useState("ทั้งหมด"),
    [scale, setScale] = useState(1),
    [pan, setPan] = useState([0, 0]);
  const drag = useRef(null);
  const list = zones.filter(
    (z) =>
      (type === "ทั้งหมด" || z.type === type) &&
      [z.name, ...z.services].join(" ").includes(query.trim()),
  );
  const selected = list.find((z) => z.id === activeZone) || list[0];
  async function copy() {
    try {
      await navigator.clipboard.writeText(`${selected.x}, ${selected.z}`);
      showToast("คัดลอกพิกัดแล้ว");
    } catch {
      showToast(`พิกัด X ${selected.x}, Z ${selected.z}`);
    }
  }
  return (
    <div className="shell">
      <Head label="TOWN DIRECTORY" title="หาเส้นทางของคุณ">
        ค้นหาธนาคาร จุดซ่อม ตลาด และบริการต่าง ๆ ในเมือง
      </Head>
      <div className="toolbar">
        <div className="field grow">
          <label htmlFor="zone-search">ค้นหาสถานที่หรือบริการ</label>
          <input
            id="zone-search"
            type="search"
            placeholder="เช่น ธนาคาร ซ่อม ตกปลา"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="zone-type">ประเภท</label>
          <select
            id="zone-type"
            value={type}
            onChange={(e) => setType(e.target.value)}
          >
            {["ทั้งหมด", "บริการ", "กิจกรรม", "พักผ่อน", "เมือง"].map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </div>
      </div>
      <div className="map-layout">
        <aside>
          <p className="search-count" role="status">
            {list.length} สถานที่
          </p>
          <div className="zone-list">
            {list.length ? (
              list.map((z) => (
                <button
                  className={`zone-button ${z.id === selected.id ? "selected" : ""}`}
                  key={z.id}
                  onClick={() => setActiveZone(z.id)}
                >
                  {z.name}
                  <small>{z.services.join(" / ")}</small>
                </button>
              ))
            ) : (
              <div className="empty">ไม่พบสถานที่ ลองค้นหาบริการอื่น</div>
            )}
          </div>
        </aside>
        <div>
          <div
            className="map-viewport"
            aria-label="แผนผังตัวอย่างของเมือง"
            onPointerDown={(e) => {
              if (e.target.closest("button")) return;
              drag.current = [e.clientX, e.clientY, ...pan];
              e.currentTarget.setPointerCapture(e.pointerId);
            }}
            onPointerMove={(e) => {
              if (!drag.current) return;
              setPan([
                Math.max(
                  -300,
                  Math.min(300, drag.current[2] + e.clientX - drag.current[0]),
                ),
                Math.max(
                  -220,
                  Math.min(220, drag.current[3] + e.clientY - drag.current[1]),
                ),
              ]);
            }}
            onPointerUp={() => (drag.current = null)}
            onPointerCancel={() => (drag.current = null)}
          >
            <div
              className="map-stage"
              style={{
                transform: `translate(${pan[0]}px,${pan[1]}px) scale(${scale})`,
              }}
            >
              <img src={asset("city.webp")} alt="ภาพเมืองสำหรับเลือกโซน" />
              {list.map((z) => (
                <button
                  className={`map-pin ${z.id === selected?.id ? "active" : ""}`}
                  key={z.id}
                  style={{ left: `${z.pin[0]}%`, top: `${z.pin[1]}%` }}
                  aria-label={z.name}
                  onClick={() => setActiveZone(z.id)}
                >
                  {zones.indexOf(z) + 1}
                </button>
              ))}
            </div>
            <div className="map-controls">
              <button
                aria-label="ขยายแผนที่"
                onClick={() => setScale(Math.min(2.5, scale + 0.25))}
              >
                +
              </button>
              <button
                aria-label="ย่อแผนที่"
                onClick={() => setScale(Math.max(1, scale - 0.25))}
              >
                −
              </button>
              <button
                aria-label="คืนมุมมอง"
                onClick={() => {
                  setScale(1);
                  setPan([0, 0]);
                }}
              >
                ↺
              </button>
            </div>
            <p className="map-note">
              แผนผังตัวอย่าง • ยังไม่ใช่พิกัดจากโลกจริง
            </p>
          </div>
          {selected ? (
            <motion.div
              className="zone-detail"
              key={selected.id}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
            >
              <div>
                <h2>{selected.name}</h2>
                <p>{selected.detail}</p>
                <p className="fine">
                  พิกัดแผน: X {selected.x}, Z {selected.z} / {selected.type}
                </p>
                <div className="actions">
                  <button className="button secondary" onClick={copy}>
                    คัดลอกพิกัด
                  </button>
                  <button
                    className="text-button"
                    onClick={() => open({ type: "image", id: selected.id })}
                  >
                    ดูภาพโซน
                  </button>
                </div>
              </div>
              <img
                src={asset(selected.image)}
                alt={`ภาพคอนเซปต์ ${selected.name}`}
              />
            </motion.div>
          ) : null}
        </div>
      </div>
      <p className="status-box fine">
        แผนที่ 3D สดจะพร้อมเมื่อเชื่อมโลก Minecraft กับ BlueMap
        ภาพนี้ยังไม่ใช่ข้อมูลสด
      </p>
    </div>
  );
}

function NewsPage() {
  const { open } = useContext(Portal);
  const [category, setCategory] = useState("ทั้งหมด");
  const feed = useFeed("news", news);
  return (
    <div className="shell">
      <Head label="TOWN JOURNAL" title="ข่าวจาก Luma">
        แนวทางเมือง ระบบ และกิจกรรมที่กำลังวางแผน
      </Head>
      {!feed.live && (
        <p className="notice">
          ประกาศตัวอย่างและแผนเมือง • ยังไม่มีข่าวจากฐานข้อมูล
        </p>
      )}
      <div className="pill-row">
        {["ทั้งหมด", "เมือง", "ชุมชน", "ระบบ"].map((c) => (
          <button
            className="pill"
            key={c}
            aria-pressed={category === c}
            onClick={() => setCategory(c)}
          >
            {c}
          </button>
        ))}
      </div>
      <motion.div layout className="news-grid">
        <AnimatePresence mode="popLayout">
          {feed.items
            .filter((n) => category === "ทั้งหมด" || n.category === category)
            .map((n) => (
              <motion.article
                layout
                className="news-card"
                key={n.id}
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.96 }}
              >
                <img src={asset(n.image)} alt={n.title} loading="lazy" />
                <span className="meta">{n.date}</span>
                <h2>{n.title}</h2>
                <p>{n.intro}</p>
                <button
                  onClick={() => open({ type: "news", item: n, id: n.id })}
                >
                  อ่านต่อ
                </button>
              </motion.article>
            ))}
        </AnimatePresence>
      </motion.div>
      {feed.live && !feed.items.length && (
        <div className="empty">ยังไม่มีประกาศที่เผยแพร่</div>
      )}
    </div>
  );
}
function WikiPage() {
  const { open } = useContext(Portal);
  const [query, setQuery] = useState(""),
    [category, setCategory] = useState("ทั้งหมด");
  const list = guides.filter(
    (g) =>
      (category === "ทั้งหมด" || g.category === category) &&
      `${g.name} ${g.summary} ${g.body.join(" ")}`
        .toLowerCase()
        .includes(query.trim().toLowerCase()),
  );
  return (
    <div className="shell">
      <Head label="PLAYER HANDBOOK" title="คู่มือเมือง Luma">
        ค้นหาระบบที่สนใจ แล้วดูวิธีเล่นและโซนบริการได้ในที่เดียว
      </Head>
      <p className="notice">
        คู่มือฉบับวางแผน • คำสั่งและสูตรจะยืนยันก่อนเปิดเซิร์ฟ
      </p>
      <div className="toolbar">
        <div className="field grow">
          <label htmlFor="guide-search">ค้นหาคู่มือ</label>
          <input
            type="search"
            id="guide-search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="เช่น ซ่อม เงิน กิลด์"
          />
        </div>
      </div>
      <div className="pill-row">
        {["ทั้งหมด", "เริ่มเล่น", "เศรษฐกิจ", "อุปกรณ์", "ผจญภัย", "ชุมชน"].map(
          (c) => (
            <button
              className="pill"
              aria-pressed={category === c}
              onClick={() => setCategory(c)}
              key={c}
            >
              {c}
            </button>
          ),
        )}
      </div>
      <motion.div layout className="guide-grid">
        <AnimatePresence mode="popLayout">
          {list.map((g) => (
            <motion.button
              layout
              className="guide-card"
              key={g.id}
              onClick={() => open({ type: "guide", id: g.id })}
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.96 }}
            >
              <Icon kind={g.icon} />
              <span className="meta">{g.category}</span>
              <h2>{g.name}</h2>
              <p>{g.summary}</p>
              <span className="text-link">อ่านคู่มือ</span>
            </motion.button>
          ))}
        </AnimatePresence>
        {!list.length && (
          <div className="empty">ไม่พบคู่มือนี้ ลองค้นหาระบบอื่น</div>
        )}
      </motion.div>
    </div>
  );
}
function ShopPage() {
  const { open, reduced } = useContext(Portal);
  const feed = useFeed("catalog", products);
  return (
    <div className="shell">
      <Head label="LUMA COSMETICS" title="ของตกแต่งจากเมือง Luma">
        รูปลักษณ์สำหรับตัวละคร ไม่มีโบนัสโจมตีจากสินค้าตกแต่ง
      </Head>
      <p className="shop-note">
        {feed.live ? "แคตตาล็อกจากฐานข้อมูล" : "สินค้าและราคาเสนอเป็นตัวอย่าง"}{" "}
        • ยังไม่เปิดชำระเงินหรือส่งของเข้าเกม
      </p>
      <div className="shop-grid">
        {feed.items.map((p) => (
          <Reveal key={p.id}>
            <motion.button
              className="product-image"
              onClick={() =>
                open({ type: "product", id: p.id, item: p, live: feed.live })
              }
              aria-label={`ดูรายละเอียด ${p.name}`}
              whileHover={reduced ? {} : { y: -4 }}
            >
              <img
                src={asset(p.image)}
                alt={`ภาพอ้างอิง ${p.name}`}
                loading="lazy"
              />
            </motion.button>
            <div className="product-info">
              <div>
                <h2 className="product-name">{p.name}</h2>
                <p>
                  {p.type} / ฿{p.price}
                  {feed.live ? "" : " ราคาเสนอ"}
                </p>
              </div>
              <button
                onClick={() =>
                  open({ type: "product", id: p.id, item: p, live: feed.live })
                }
              >
                รายละเอียด
              </button>
            </div>
          </Reveal>
        ))}
      </div>
      {feed.live && !feed.items.length && (
        <div className="empty">ยังไม่มีสินค้าที่เปิดแสดง</div>
      )}
    </div>
  );
}
function EventsPage() {
  const { open } = useContext(Portal);
  return (
    <div className="shell">
      <Head label="TOWN CALENDAR" title="กิจกรรมของเมือง">
        เควสชุมชน วันรวมกิลด์ และกิจกรรมที่เล่นได้ตามจังหวะของคุณ
      </Head>
      <p className="notice">
        แผนกิจกรรม • ยังไม่ประกาศวันเริ่มหรือเปิดลงทะเบียน
      </p>
      <div className="event-layout">
        <Reveal className="event-feature">
          <img
            src={asset("09-portals-adventure.webp")}
            alt="ประตูการผจญภัย Luma"
          />
          <div>
            <span className="tag">กำลังวางแผน</span>
            <h2>
              เปิดประตูสู่
              <br />
              การผจญภัยครั้งแรก
            </h2>
            <p>
              เตรียมเส้นทางสำรวจ เควสเริ่มต้น
              และกลุ่มนักผจญภัยสำหรับวันเปิดเมือง
            </p>
            <button
              className="button secondary"
              onClick={() => open({ type: "guide", id: "quest" })}
            >
              อ่านคู่มือเควส
            </button>
          </div>
        </Reveal>
        <div className="event-list">
          {[
            [
              "ตลาดกลางคืน",
              "วันรวมตัวของพ่อค้าและผู้เล่น",
              "06-fantasy-market.webp",
              "market",
            ],
            [
              "เทศกาลนักตกปลา",
              "กิจกรรมพักผ่อนริมบ่อในเมือง",
              "10-pets-fishing.webp",
              "pets",
            ],
            [
              "คืนรวมกิลด์",
              "นัดพบเพื่อนและเตรียมปาร์ตี้",
              "04-guild-arcane-hall.webp",
              "guild",
            ],
          ].map(([name, description, image, id], i) => (
            <Reveal key={id} delay={i * 0.06}>
              <article>
                <img src={asset(image)} alt={name} loading="lazy" />
                <div>
                  <span className="meta">ยังไม่กำหนดวัน</span>
                  <h2>{name}</h2>
                  <p>{description}</p>
                  <button
                    className="text-button"
                    onClick={() => open({ type: "guide", id })}
                  >
                    ดูระบบที่เกี่ยวข้อง
                  </button>
                </div>
              </article>
            </Reveal>
          ))}
        </div>
      </div>
    </div>
  );
}
function RankingsPage() {
  const [category, setCategory] = useState("การผจญภัย");
  return (
    <div className="shell">
      <Head label="HALL OF ADVENTURERS" title="บันทึกนักผจญภัย">
        อันดับตามฤดูกาล แยกตามรูปแบบการเล่น
      </Head>
      <div className="pill-row">
        {["การผจญภัย", "อาชีพ", "ตกปลา", "กิลด์"].map((c) => (
          <button
            key={c}
            className="pill"
            aria-pressed={category === c}
            onClick={() => setCategory(c)}
          >
            {c}
          </button>
        ))}
      </div>
      <section className="ranking-panel">
        <div className="ranking-head">
          <h2>{category}</h2>
          <span className="state pending">รอข้อมูลเกม</span>
        </div>
        <div className="empty large-empty">
          <Icon kind="banner" />
          <h3>เรื่องราวของนักผจญภัยยังไม่เริ่ม</h3>
          <p>อันดับจริงจะปรากฏเมื่อเซิร์ฟเปิดบริการและเชื่อมข้อมูลฤดูกาล</p>
          <Link className="text-link" href="/wiki/">
            เตรียมตัวก่อนออกผจญภัย
          </Link>
        </div>
      </section>
    </div>
  );
}
function StatusPage() {
  const health = useHealth();
  return (
    <div className="shell">
      <Head label="SERVICE STATUS" title="สถานะระบบ Luma">
        ตรวจความพร้อมของบริการก่อนเข้าเล่นหรือซื้อสินค้า
      </Head>
      <div className="status-summary">
        <Icon kind="portal" />
        <div>
          <h2>เมืองอยู่ระหว่างพัฒนา</h2>
          <p>ยังไม่มีข้อมูลออนไลน์จากเซิร์ฟ Minecraft</p>
        </div>
      </div>
      <div className="service-status-list">
        <article>
          <h2>ฐานข้อมูลเว็บ</h2>
          <p>PostgreSQL ผ่าน Node API</p>
          <span className="state pending">
            {health?.database === "ready"
              ? "เชื่อมฐานข้อมูลแล้ว"
              : health
                ? "ยังไม่เชื่อมฐานข้อมูล"
                : "กำลังตรวจการเชื่อมต่อ"}
          </span>
        </article>
        {serviceChecks.map((s) => (
          <article key={s[0]}>
            <h2>{s[0]}</h2>
            <p>{s[2]}</p>
            <span className="state pending">{s[1]}</span>
          </article>
        ))}
      </div>
    </div>
  );
}
function AccountPage() {
  const { theme, setTheme, reduce, setReduce } = useContext(Portal);
  const [tab, setTab] = useState("overview"),
    [code, setCode] = useState(""),
    [message, setMessage] = useState("");
  return (
    <div className="shell">
      <Head label="PLAYER CENTER" title="ศูนย์บัญชีผู้เล่น">
        เชื่อมตัวละคร ติดตามรายการซื้อ และรับของที่ส่งเข้าเกม
      </Head>
      <div className="account-top">
        <div className="avatar-placeholder">
          <Icon />
        </div>
        <div>
          <h2>ยังไม่ได้เชื่อมตัวละคร</h2>
          <p className="fine">ยืนยันบัญชี Minecraft ก่อนดูข้อมูลส่วนตัว</p>
        </div>
        <span className="state pending">ยังไม่เชื่อมเซิร์ฟ</span>
      </div>
      <div className="pill-row account-tabs">
        {[
          ["overview", "เชื่อมบัญชี"],
          ["orders", "รายการซื้อ"],
          ["mail", "กล่องจดหมาย"],
          ["settings", "ตั้งค่า"],
        ].map(([id, name]) => (
          <button
            key={id}
            className="pill"
            aria-pressed={tab === id}
            onClick={() => setTab(id)}
          >
            {name}
          </button>
        ))}
      </div>
      <AnimatePresence mode="wait">
        <motion.div
          key={tab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -5 }}
          transition={{ duration: 0.15 }}
        >
          {tab === "overview" ? (
            <div className="account-layout">
              <section className="account-panel">
                <h2>เชื่อมบัญชี Minecraft</h2>
                <ol className="step-list">
                  {[
                    "เข้าเกมด้วยบัญชีของคุณ",
                    "ใช้ /link เพื่อรับรหัสครั้งเดียว",
                    "นำรหัสมากรอกด้านล่าง",
                  ].map((text, i) => (
                    <li key={text}>
                      <span>0{i + 1}</span>
                      <div>{text}</div>
                    </li>
                  ))}
                </ol>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    setMessage(
                      "ยังไม่เชื่อมเซิร์ฟเกม รหัสนี้ไม่ได้ถูกส่งหรือตรวจสอบ",
                    );
                  }}
                >
                  <div className="field">
                    <label htmlFor="link-code">รหัสจากเกม</label>
                    <input
                      id="link-code"
                      value={code}
                      onChange={(e) => setCode(e.target.value)}
                      maxLength={12}
                      required
                      autoComplete="off"
                      aria-describedby="link-help"
                    />
                  </div>
                  <p id="link-help" className="fine">
                    ยังไม่เชื่อมเซิร์ฟ จึงไม่สามารถตรวจรหัสได้
                  </p>
                  <div className="actions">
                    <button className="button" type="submit">
                      ตรวจสอบรหัส
                    </button>
                  </div>
                  <p className="error" role="status">
                    {message}
                  </p>
                </form>
              </section>
              <section className="panel">
                <h2>บัญชีเดียว ดูแลของครบ</h2>
                {[
                  [
                    "chest",
                    "รายการซื้อและใบเสร็จ",
                    "ติดตามสถานะชำระเงินและการส่งมอบ",
                  ],
                  ["book", "ของรอรับในเกม", "รับผ่านกล่องจดหมายเมื่อพร้อม"],
                  ["banner", "ตัวละครและกิลด์", "ดูข้อมูลที่คุณอนุญาตให้แสดง"],
                ].map(([icon, title, hint]) => (
                  <div className="benefit-row" key={title}>
                    <Icon kind={icon} />
                    <div>
                      <h3>{title}</h3>
                      <p>{hint}</p>
                    </div>
                  </div>
                ))}
                <p className="fine">บริการเหล่านี้อยู่ในแผนเชื่อมระบบจริง</p>
              </section>
            </div>
          ) : tab === "settings" ? (
            <section className="panel">
              <h2>ตั้งค่าหน้าจอ</h2>
              <p className="fine">
                บันทึกในเบราว์เซอร์นี้ โดยไม่ต้องเชื่อมบัญชีเกม
              </p>
              <div className="setting-row">
                <label htmlFor="color-choice">รูปแบบสี</label>
                <select
                  id="color-choice"
                  value={theme}
                  onChange={(e) => setTheme(e.target.value)}
                >
                  <option value="dark">เมืองยามค่ำ</option>
                  <option value="light">เมืองยามเช้า</option>
                </select>
              </div>
              <div className="setting-row">
                <label htmlFor="reduce-motion">ลดการเคลื่อนไหว</label>
                <input
                  type="checkbox"
                  id="reduce-motion"
                  checked={reduce}
                  onChange={(e) => setReduce(e.target.checked)}
                />
              </div>
              <p className="fine">
                ตั้งค่าบัญชีและการแจ้งเตือนจะพร้อมหลังเชื่อมระบบจริง
              </p>
            </section>
          ) : (
            <section className="panel">
              <h2>{tab === "orders" ? "รายการซื้อของฉัน" : "กล่องจดหมาย"}</h2>
              <div className="empty large-empty">
                <Icon kind={tab === "orders" ? "chest" : "book"} />
                <h3>
                  {tab === "orders"
                    ? "ยังไม่มีรายการซื้อให้แสดง"
                    : "เชื่อมตัวละครเพื่อดูของรอรับ"}
                </h3>
                <p>
                  {tab === "orders"
                    ? "ร้านค้ายังไม่เปิดรับชำระ ระบบจริงจะแสดงเลขรายการ ยอดเงิน และสถานะส่งมอบที่นี่"
                    : "ของที่ส่งตอนออฟไลน์หรือกระเป๋าเต็มจะรอรับในกล่องจดหมายเมื่อเปิดบริการ"}
                </p>
                {tab === "orders" ? (
                  <Link href="/shop/" className="text-link">
                    ดูของตกแต่ง
                  </Link>
                ) : (
                  <button
                    className="text-button"
                    onClick={() => setTab("overview")}
                  >
                    ไปเชื่อมบัญชี
                  </button>
                )}
              </div>
            </section>
          )}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}
function Legal({ kind }) {
  return (
    <div className="shell legal-page">
      <Head
        label="LUMA SERVICE"
        title={kind === "privacy" ? "ความเป็นส่วนตัว" : "เงื่อนไขบริการ"}
      >
        ร่างสำหรับเตรียมเปิดบริการ
        ต้องระบุผู้ดำเนินการและช่องทางติดต่อก่อนใช้งานจริง
      </Head>
      <article className="article">
        {kind === "privacy" ? (
          <>
            <h2>ข้อมูลในเว็บฉบับพัฒนา</h2>
            <p>
              หน้าเว็บยังไม่ขอรหัสผ่าน Minecraft ไม่ตรวจรหัส /link
              และไม่รับข้อมูลการชำระเงินจริง
              การเลือกธีมและลดการเคลื่อนไหวบันทึกในเบราว์เซอร์ของคุณ
            </p>
            <h2>เมื่อเชื่อมบริการจริง</h2>
            <p>
              แสดงวัตถุประสงค์ ระยะเวลาเก็บข้อมูล ผู้ให้บริการ
              และช่องทางขอเข้าถึงหรือลบข้อมูลก่อนเก็บ UUID ประวัติคำสั่งซื้อ
              หรือข้อมูลบัญชี
            </p>
          </>
        ) : (
          <>
            <h2>สถานะบริการ</h2>
            <p>
              ระบบเกม สินค้า ราคา และกิจกรรมยังอยู่ระหว่างวางแผน
              ไม่มีการรับชำระหรือรับประกันวันเปิดเซิร์ฟ
            </p>
            <h2>ก่อนเปิดร้านค้า</h2>
            <p>
              ต้องประกาศรายละเอียดสินค้า รุ่นเกมที่รองรับ วิธีส่งของ การคืนเงิน
              และช่องทางติดต่อให้ครบก่อนเปิดรับชำระ
            </p>
          </>
        )}
        <Link className="text-link" href="/">
          กลับหน้าหลัก
        </Link>
      </article>
    </div>
  );
}
