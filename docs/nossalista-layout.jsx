import { useState } from "react";

const SCREENS = {
  LOGIN: "login",
  HOME: "home",
  LIST: "list",
};

// --- Mock Data ---
const mockLists = [
  { id: 1, name: "Mercado da Semana", type: "compras", emoji: "🛒", members: 3, items: 8, checked: 5, color: "#0ea5e9" },
  { id: 2, name: "Viagem Natal", type: "tarefas", emoji: "✅", members: 2, items: 6, checked: 2, color: "#38bdf8" },
  { id: 3, name: "Aniversário Ana", type: "wishlist", emoji: "🎁", members: 4, items: 5, checked: 1, color: "#6366f1" },
  { id: 4, name: "Anotações", type: "generica", emoji: "📝", members: 1, items: 3, checked: 0, color: "#94a3b8" },
];

const mockItems = [
  { id: 1, name: "Arroz integral", quantity: 2, checked: true, createdBy: "Leo" },
  { id: 2, name: "Feijão preto", quantity: 1, checked: true, createdBy: "Ana" },
  { id: 3, name: "Leite desnatado", quantity: 3, checked: false, createdBy: "Leo" },
  { id: 4, name: "Banana prata", quantity: 6, checked: false, createdBy: "Ana" },
  { id: 5, name: "Pão de forma", quantity: 1, checked: true, createdBy: "Leo" },
  { id: 6, name: "Café torrado", quantity: 1, checked: false, createdBy: "Carlos" },
  { id: 7, name: "Azeite extra virgem", quantity: 1, checked: false, createdBy: "Leo" },
  { id: 8, name: "Tomate italiano", quantity: 4, checked: true, createdBy: "Ana" },
];

const mockMembers = [
  { id: 1, name: "Leo", avatar: "L", online: true },
  { id: 2, name: "Ana", avatar: "A", online: true },
  { id: 3, name: "Carlos", avatar: "C", online: false },
];

const mockActivity = [
  { user: "Ana", action: "marcou", target: "Feijão preto", time: "agora" },
  { user: "Leo", action: "adicionou", target: "Azeite extra virgem", time: "2 min" },
  { user: "Carlos", action: "entrou na lista", target: "", time: "10 min" },
];

// --- Blue gradient palette ---
const palette = {
  bg: "#f0f9ff",
  card: "#ffffff",
  surface: "#f0f7ff",
  border: "#dbeafe",
  borderHover: "#bfdbfe",
  text: "#0f172a",
  textSecondary: "#64748b",
  accent: "#0284c7",
  accentLight: "#e0f2fe",
  accentMid: "#38bdf8",
  accentDark: "#075985",
  success: "#10b981",
  danger: "#ef4444",
};

const font = `'DM Sans', 'Nunito', system-ui, sans-serif`;
const fontDisplay = `'Playfair Display', Georgia, serif`;

function LoginScreen({ onLogin }) {
  return (
    <div style={{
      minHeight: "100vh",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      background: `linear-gradient(155deg, #f0f9ff 0%, #e0f2fe 25%, #bae6fd 55%, #7dd3fc 100%)`,
      fontFamily: font,
      position: "relative",
      overflow: "hidden",
    }}>
      <div style={{ position: "absolute", top: -100, right: -100, width: 360, height: 360, borderRadius: "50%", background: "rgba(56,189,248,0.12)", filter: "blur(2px)" }} />
      <div style={{ position: "absolute", bottom: -140, left: -80, width: 440, height: 440, borderRadius: "50%", background: "rgba(14,165,233,0.08)", filter: "blur(2px)" }} />
      <div style={{ position: "absolute", top: "40%", left: "10%", width: 180, height: 180, borderRadius: "50%", background: "rgba(125,211,252,0.15)" }} />

      <div style={{
        background: "rgba(255,255,255,0.85)",
        backdropFilter: "blur(20px)",
        WebkitBackdropFilter: "blur(20px)",
        borderRadius: 28,
        padding: "48px 40px",
        width: "100%",
        maxWidth: 400,
        boxShadow: "0 24px 64px rgba(7,89,133,0.1), 0 1px 4px rgba(7,89,133,0.04), inset 0 1px 0 rgba(255,255,255,0.8)",
        border: "1px solid rgba(186,230,253,0.5)",
        position: "relative",
        zIndex: 1,
        textAlign: "center",
      }}>
        <div style={{
          width: 56, height: 56, borderRadius: 16,
          background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`,
          display: "flex", alignItems: "center", justifyContent: "center",
          margin: "0 auto 14px",
          boxShadow: "0 4px 16px rgba(2,132,199,0.25)",
        }}>
          <span style={{ fontSize: 28 }}>📋</span>
        </div>
        <h1 style={{ fontFamily: fontDisplay, fontSize: 32, fontWeight: 700, color: palette.text, margin: "0 0 4px", letterSpacing: "-0.5px" }}>NossaLista</h1>
        <p style={{ color: palette.textSecondary, fontSize: 14, margin: "0 0 36px", letterSpacing: "0.3px" }}>Listas compartilhadas em tempo real</p>

        <button onClick={onLogin} style={{
          width: "100%", padding: "14px 20px", borderRadius: 14,
          border: `1px solid ${palette.border}`, background: palette.card,
          cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 12,
          fontSize: 15, fontWeight: 500, color: palette.text, fontFamily: font,
          transition: "all 0.2s", boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
        }}
          onMouseOver={e => { e.currentTarget.style.background = palette.surface; e.currentTarget.style.borderColor = palette.borderHover; e.currentTarget.style.boxShadow = "0 4px 12px rgba(2,132,199,0.08)"; }}
          onMouseOut={e => { e.currentTarget.style.background = palette.card; e.currentTarget.style.borderColor = palette.border; e.currentTarget.style.boxShadow = "0 1px 3px rgba(0,0,0,0.04)"; }}
        >
          <svg width="20" height="20" viewBox="0 0 24 24">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
          </svg>
          Entrar com Google
        </button>

        <div style={{ display: "flex", alignItems: "center", gap: 12, margin: "24px 0", color: palette.textSecondary, fontSize: 12 }}>
          <div style={{ flex: 1, height: 1, background: palette.border }} />
          <span>ou</span>
          <div style={{ flex: 1, height: 1, background: palette.border }} />
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <input type="email" placeholder="Email" style={{
            padding: "13px 16px", borderRadius: 14, border: `1px solid ${palette.border}`,
            fontSize: 14, fontFamily: font, outline: "none", background: palette.surface,
            color: palette.text, transition: "border 0.2s, box-shadow 0.2s",
          }}
            onFocus={e => { e.target.style.borderColor = palette.accentMid; e.target.style.boxShadow = "0 0 0 3px rgba(56,189,248,0.15)"; }}
            onBlur={e => { e.target.style.borderColor = palette.border; e.target.style.boxShadow = "none"; }}
          />
          <input type="password" placeholder="Senha" style={{
            padding: "13px 16px", borderRadius: 14, border: `1px solid ${palette.border}`,
            fontSize: 14, fontFamily: font, outline: "none", background: palette.surface,
            color: palette.text, transition: "border 0.2s, box-shadow 0.2s",
          }}
            onFocus={e => { e.target.style.borderColor = palette.accentMid; e.target.style.boxShadow = "0 0 0 3px rgba(56,189,248,0.15)"; }}
            onBlur={e => { e.target.style.borderColor = palette.border; e.target.style.boxShadow = "none"; }}
          />
          <button onClick={onLogin} style={{
            padding: "14px 20px", borderRadius: 14, border: "none",
            background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`,
            color: "#fff", fontSize: 15, fontWeight: 600, fontFamily: font,
            cursor: "pointer", transition: "all 0.2s",
            boxShadow: "0 4px 14px rgba(2,132,199,0.3)",
          }}
            onMouseOver={e => { e.currentTarget.style.boxShadow = "0 6px 20px rgba(2,132,199,0.4)"; e.currentTarget.style.transform = "translateY(-1px)"; }}
            onMouseOut={e => { e.currentTarget.style.boxShadow = "0 4px 14px rgba(2,132,199,0.3)"; e.currentTarget.style.transform = "translateY(0)"; }}
          >Entrar</button>
        </div>

        <p style={{ marginTop: 24, fontSize: 13, color: palette.textSecondary }}>
          Não tem conta?{" "}
          <span style={{ color: palette.accent, cursor: "pointer", fontWeight: 600 }}>Criar conta</span>
        </p>
      </div>
    </div>
  );
}

function Header() {
  return (
    <header style={{
      display: "flex", alignItems: "center", justifyContent: "space-between",
      padding: "14px 24px",
      background: "rgba(255,255,255,0.8)",
      backdropFilter: "blur(16px)", WebkitBackdropFilter: "blur(16px)",
      borderBottom: `1px solid ${palette.border}`,
      position: "sticky", top: 0, zIndex: 50,
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <div style={{
          width: 32, height: 32, borderRadius: 9,
          background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`,
          display: "flex", alignItems: "center", justifyContent: "center",
          boxShadow: "0 2px 8px rgba(2,132,199,0.2)",
        }}><span style={{ fontSize: 16 }}>📋</span></div>
        <span style={{ fontFamily: fontDisplay, fontSize: 20, fontWeight: 700, color: palette.text, letterSpacing: "-0.3px" }}>NossaLista</span>
      </div>
      <div style={{
        width: 36, height: 36, borderRadius: "50%",
        background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`,
        display: "flex", alignItems: "center", justifyContent: "center",
        color: "#fff", fontWeight: 700, fontSize: 14, fontFamily: font, cursor: "pointer",
        boxShadow: "0 2px 8px rgba(2,132,199,0.2)",
      }}>L</div>
    </header>
  );
}

function HomeScreen({ onSelectList }) {
  const [showModal, setShowModal] = useState(false);
  const [selectedType, setSelectedType] = useState("Compras");

  return (
    <div style={{ minHeight: "100vh", background: `linear-gradient(180deg, ${palette.bg} 0%, #e0f2fe 100%)`, fontFamily: font }}>
      <Header />
      <main style={{ maxWidth: 640, margin: "0 auto", padding: "32px 20px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 28 }}>
          <div>
            <h2 style={{ fontSize: 26, fontWeight: 700, color: palette.text, margin: 0, fontFamily: fontDisplay }}>Minhas Listas</h2>
            <p style={{ fontSize: 13, color: palette.textSecondary, margin: "4px 0 0" }}>4 listas · 2 compartilhadas</p>
          </div>
          <button onClick={() => setShowModal(true)} style={{
            padding: "10px 20px", borderRadius: 14, border: "none",
            background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`,
            color: "#fff", fontSize: 14, fontWeight: 600, fontFamily: font, cursor: "pointer",
            display: "flex", alignItems: "center", gap: 6, transition: "all 0.2s",
            boxShadow: "0 4px 14px rgba(2,132,199,0.3)",
          }}
            onMouseOver={e => { e.currentTarget.style.transform = "translateY(-1px)"; e.currentTarget.style.boxShadow = "0 6px 20px rgba(2,132,199,0.35)"; }}
            onMouseOut={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 4px 14px rgba(2,132,199,0.3)"; }}
          ><span style={{ fontSize: 18, lineHeight: 1 }}>+</span>Nova Lista</button>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {mockLists.map((list) => (
            <div key={list.id} onClick={() => onSelectList(list)} style={{
              background: "rgba(255,255,255,0.75)", backdropFilter: "blur(8px)",
              borderRadius: 18, padding: "18px 20px", cursor: "pointer",
              border: `1px solid ${palette.border}`, transition: "all 0.25s ease",
              display: "flex", alignItems: "center", gap: 16,
            }}
              onMouseOver={e => { e.currentTarget.style.boxShadow = "0 8px 24px rgba(2,132,199,0.1)"; e.currentTarget.style.transform = "translateY(-2px)"; e.currentTarget.style.borderColor = palette.borderHover; e.currentTarget.style.background = "rgba(255,255,255,0.95)"; }}
              onMouseOut={e => { e.currentTarget.style.boxShadow = "none"; e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.borderColor = palette.border; e.currentTarget.style.background = "rgba(255,255,255,0.75)"; }}
            >
              <div style={{ width: 50, height: 50, borderRadius: 15, background: `${list.color}14`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 24, flexShrink: 0 }}>{list.emoji}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <h3 style={{ fontSize: 16, fontWeight: 600, color: palette.text, margin: 0, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{list.name}</h3>
                <p style={{ fontSize: 13, color: palette.textSecondary, margin: "3px 0 0" }}>
                  {list.checked}/{list.items} itens{list.members > 1 && ` · ${list.members} pessoas`}
                </p>
              </div>
              <div style={{ position: "relative", width: 42, height: 42, flexShrink: 0 }}>
                <svg width="42" height="42" viewBox="0 0 42 42">
                  <circle cx="21" cy="21" r="17" fill="none" stroke={palette.border} strokeWidth="3" />
                  <circle cx="21" cy="21" r="17" fill="none" stroke={list.color} strokeWidth="3"
                    strokeDasharray={`${(list.checked / list.items) * 106.8} 106.8`}
                    strokeLinecap="round" transform="rotate(-90 21 21)"
                    style={{ transition: "stroke-dasharray 0.4s ease" }} />
                </svg>
                <span style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, fontWeight: 700, color: palette.textSecondary }}>{Math.round((list.checked / list.items) * 100)}%</span>
              </div>
            </div>
          ))}
        </div>
      </main>

      {showModal && (
        <div onClick={() => setShowModal(false)} style={{ position: "fixed", inset: 0, background: "rgba(15,23,42,0.35)", backdropFilter: "blur(6px)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 100, padding: 20 }}>
          <div onClick={e => e.stopPropagation()} style={{ background: palette.card, borderRadius: 24, padding: "32px 28px", width: "100%", maxWidth: 420, boxShadow: "0 24px 64px rgba(7,89,133,0.18)", border: `1px solid ${palette.border}` }}>
            <h3 style={{ fontSize: 22, fontWeight: 700, color: palette.text, margin: "0 0 24px", fontFamily: fontDisplay }}>Nova Lista</h3>
            <input type="text" placeholder="Nome da lista" style={{
              width: "100%", padding: "13px 16px", borderRadius: 14, border: `1px solid ${palette.border}`,
              fontSize: 15, fontFamily: font, outline: "none", background: palette.surface,
              marginBottom: 18, boxSizing: "border-box", transition: "border 0.2s, box-shadow 0.2s",
            }}
              onFocus={e => { e.target.style.borderColor = palette.accentMid; e.target.style.boxShadow = "0 0 0 3px rgba(56,189,248,0.12)"; }}
              onBlur={e => { e.target.style.borderColor = palette.border; e.target.style.boxShadow = "none"; }}
            />
            <p style={{ fontSize: 12, fontWeight: 600, color: palette.textSecondary, margin: "0 0 12px", textTransform: "uppercase", letterSpacing: "0.6px" }}>Tipo</p>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 24 }}>
              {[{ emoji: "🛒", label: "Compras" }, { emoji: "✅", label: "Tarefas" }, { emoji: "🎁", label: "Wishlist" }, { emoji: "📝", label: "Genérica" }].map(t => {
                const active = t.label === selectedType;
                return (
                  <div key={t.label} onClick={() => setSelectedType(t.label)} style={{
                    padding: "14px 12px", borderRadius: 14,
                    border: `2px solid ${active ? palette.accent : palette.border}`,
                    background: active ? palette.accentLight : "transparent",
                    cursor: "pointer", display: "flex", alignItems: "center", gap: 10, transition: "all 0.15s",
                  }}>
                    <span style={{ fontSize: 20 }}>{t.emoji}</span>
                    <span style={{ fontSize: 14, fontWeight: active ? 600 : 500, color: active ? palette.accentDark : palette.text }}>{t.label}</span>
                  </div>
                );
              })}
            </div>
            <div style={{ display: "flex", gap: 10 }}>
              <button onClick={() => setShowModal(false)} style={{ flex: 1, padding: "13px", borderRadius: 14, border: `1px solid ${palette.border}`, background: "transparent", fontSize: 14, fontWeight: 600, fontFamily: font, color: palette.textSecondary, cursor: "pointer", transition: "all 0.15s" }}
                onMouseOver={e => e.currentTarget.style.background = palette.surface}
                onMouseOut={e => e.currentTarget.style.background = "transparent"}
              >Cancelar</button>
              <button onClick={() => setShowModal(false)} style={{ flex: 1, padding: "13px", borderRadius: 14, border: "none", background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`, color: "#fff", fontSize: 14, fontWeight: 600, fontFamily: font, cursor: "pointer", boxShadow: "0 4px 12px rgba(2,132,199,0.25)", transition: "all 0.15s" }}
                onMouseOver={e => e.currentTarget.style.boxShadow = "0 6px 18px rgba(2,132,199,0.35)"}
                onMouseOut={e => e.currentTarget.style.boxShadow = "0 4px 12px rgba(2,132,199,0.25)"}
              >Criar Lista</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function ListViewScreen({ list, onBack }) {
  const [items, setItems] = useState(mockItems);
  const [showInvite, setShowInvite] = useState(false);
  const [showActivity, setShowActivity] = useState(false);
  const [newItem, setNewItem] = useState("");

  const toggleCheck = (id) => setItems(items.map(i => i.id === id ? { ...i, checked: !i.checked } : i));
  const unchecked = items.filter(i => !i.checked);
  const checked = items.filter(i => i.checked);

  return (
    <div style={{ minHeight: "100vh", background: `linear-gradient(180deg, ${palette.bg} 0%, #e0f2fe 100%)`, fontFamily: font }}>
      <header style={{ padding: "14px 20px", background: "rgba(255,255,255,0.8)", backdropFilter: "blur(16px)", WebkitBackdropFilter: "blur(16px)", borderBottom: `1px solid ${palette.border}`, position: "sticky", top: 0, zIndex: 50 }}>
        <div style={{ maxWidth: 640, margin: "0 auto", display: "flex", alignItems: "center", gap: 12 }}>
          <button onClick={onBack} style={{ background: "none", border: "none", cursor: "pointer", padding: "4px 8px", fontSize: 20, color: palette.accent, display: "flex", borderRadius: 8, transition: "background 0.15s" }}
            onMouseOver={e => e.currentTarget.style.background = palette.accentLight}
            onMouseOut={e => e.currentTarget.style.background = "none"}
          >←</button>
          <div style={{ flex: 1 }}>
            <h2 style={{ fontSize: 18, fontWeight: 700, color: palette.text, margin: 0 }}>{list.emoji} {list.name}</h2>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <button onClick={() => setShowActivity(!showActivity)} style={{
              padding: "8px 12px", borderRadius: 10,
              border: `1px solid ${showActivity ? palette.accentMid : palette.border}`,
              background: showActivity ? palette.accentLight : "transparent",
              fontSize: 13, fontFamily: font, cursor: "pointer",
              color: showActivity ? palette.accent : palette.textSecondary, fontWeight: 500, transition: "all 0.15s",
            }}>📜</button>
            <button onClick={() => setShowInvite(true)} style={{
              padding: "8px 14px", borderRadius: 10, border: "none",
              background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`,
              color: "#fff", fontSize: 13, fontWeight: 600, fontFamily: font, cursor: "pointer",
              display: "flex", alignItems: "center", gap: 4, boxShadow: "0 2px 8px rgba(2,132,199,0.25)",
            }}><span>+</span> Convidar</button>
          </div>
        </div>
      </header>

      <main style={{ maxWidth: 640, margin: "0 auto", padding: "20px 20px 100px" }}>
        {/* Members bar */}
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 20, padding: "12px 16px", background: "rgba(255,255,255,0.7)", backdropFilter: "blur(8px)", borderRadius: 14, border: `1px solid ${palette.border}` }}>
          <div style={{ display: "flex", marginRight: 4 }}>
            {mockMembers.map((m, i) => (
              <div key={m.id} style={{
                width: 30, height: 30, borderRadius: "50%",
                background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`,
                display: "flex", alignItems: "center", justifyContent: "center",
                color: "#fff", fontSize: 12, fontWeight: 700,
                border: `2px solid ${palette.card}`,
                marginLeft: i > 0 ? -8 : 0, position: "relative", zIndex: mockMembers.length - i,
              }}>
                {m.avatar}
                {m.online && <div style={{ position: "absolute", bottom: -1, right: -1, width: 10, height: 10, borderRadius: "50%", background: palette.success, border: `2px solid ${palette.card}` }} />}
              </div>
            ))}
          </div>
          <span style={{ fontSize: 13, color: palette.textSecondary }}>{mockMembers.filter(m => m.online).length} online</span>
          <div style={{ flex: 1 }} />
          <span style={{ fontSize: 13, color: palette.textSecondary }}>{checked.length}/{items.length} feitos</span>
        </div>

        {/* Activity Panel */}
        {showActivity && (
          <div style={{ background: "rgba(255,255,255,0.8)", backdropFilter: "blur(8px)", borderRadius: 16, border: `1px solid ${palette.border}`, padding: "16px 18px", marginBottom: 16 }}>
            <h4 style={{ fontSize: 12, fontWeight: 600, color: palette.textSecondary, margin: "0 0 12px", textTransform: "uppercase", letterSpacing: "0.6px" }}>Atividade Recente</h4>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {mockActivity.map((a, i) => (
                <div key={i} style={{ display: "flex", alignItems: "center", gap: 10, fontSize: 13 }}>
                  <div style={{ width: 7, height: 7, borderRadius: "50%", background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`, flexShrink: 0 }} />
                  <span style={{ color: palette.text }}><strong>{a.user}</strong> <span style={{ color: palette.textSecondary }}>{a.action} {a.target && `'${a.target}'`}</span></span>
                  <span style={{ marginLeft: "auto", color: palette.textSecondary, fontSize: 12, flexShrink: 0 }}>{a.time}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Unchecked items */}
        <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          {unchecked.map(item => (
            <div key={item.id} onClick={() => toggleCheck(item.id)} style={{
              display: "flex", alignItems: "center", gap: 14, padding: "14px 16px",
              background: "rgba(255,255,255,0.7)", backdropFilter: "blur(4px)",
              borderRadius: 14, border: `1px solid ${palette.border}`,
              transition: "all 0.2s ease", cursor: "pointer",
            }}
              onMouseOver={e => { e.currentTarget.style.background = "rgba(255,255,255,0.95)"; e.currentTarget.style.borderColor = palette.borderHover; }}
              onMouseOut={e => { e.currentTarget.style.background = "rgba(255,255,255,0.7)"; e.currentTarget.style.borderColor = palette.border; }}
            >
              <div style={{ width: 22, height: 22, borderRadius: 7, border: `2px solid ${palette.borderHover}`, flexShrink: 0, transition: "all 0.15s" }} />
              <span style={{ flex: 1, fontSize: 15, fontWeight: 500, color: palette.text }}>{item.name}</span>
              {item.quantity && <span style={{ fontSize: 13, fontWeight: 600, color: palette.accent, background: palette.accentLight, padding: "3px 10px", borderRadius: 8 }}>×{item.quantity}</span>}
              <span style={{ fontSize: 11, color: palette.textSecondary }}>{item.createdBy}</span>
            </div>
          ))}
        </div>

        {/* Checked items */}
        {checked.length > 0 && (
          <div style={{ marginTop: 20 }}>
            <p style={{ fontSize: 12, fontWeight: 600, color: palette.textSecondary, textTransform: "uppercase", letterSpacing: "0.6px", margin: "0 0 8px 4px" }}>Concluídos ({checked.length})</p>
            <div style={{ display: "flex", flexDirection: "column", gap: 3 }}>
              {checked.map(item => (
                <div key={item.id} onClick={() => toggleCheck(item.id)} style={{
                  display: "flex", alignItems: "center", gap: 14, padding: "12px 16px",
                  background: "rgba(255,255,255,0.45)", borderRadius: 14,
                  border: `1px solid ${palette.border}`, opacity: 0.6,
                  cursor: "pointer", transition: "all 0.15s",
                }}>
                  <div style={{ width: 22, height: 22, borderRadius: 7, background: palette.success, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2.5 6L5 8.5L9.5 4" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
                  </div>
                  <span style={{ flex: 1, fontSize: 15, color: palette.textSecondary, textDecoration: "line-through" }}>{item.name}</span>
                  {item.quantity && <span style={{ fontSize: 13, color: palette.textSecondary, padding: "3px 10px", borderRadius: 8 }}>×{item.quantity}</span>}
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* Add item bar */}
      <div style={{ position: "fixed", bottom: 0, left: 0, right: 0, padding: "12px 20px", background: "rgba(255,255,255,0.85)", backdropFilter: "blur(16px)", WebkitBackdropFilter: "blur(16px)", borderTop: `1px solid ${palette.border}`, zIndex: 50 }}>
        <div style={{ maxWidth: 640, margin: "0 auto", display: "flex", gap: 10 }}>
          <input type="text" placeholder="Adicionar item..." value={newItem} onChange={e => setNewItem(e.target.value)} style={{
            flex: 1, padding: "12px 16px", borderRadius: 14, border: `1px solid ${palette.border}`,
            fontSize: 15, fontFamily: font, outline: "none", background: palette.surface, transition: "border 0.2s, box-shadow 0.2s",
          }}
            onFocus={e => { e.target.style.borderColor = palette.accentMid; e.target.style.boxShadow = "0 0 0 3px rgba(56,189,248,0.12)"; }}
            onBlur={e => { e.target.style.borderColor = palette.border; e.target.style.boxShadow = "none"; }}
          />
          <input type="number" placeholder="Qtd" style={{ width: 60, padding: "12px 10px", borderRadius: 14, border: `1px solid ${palette.border}`, fontSize: 15, fontFamily: font, outline: "none", background: palette.surface, textAlign: "center" }} />
          <button style={{
            padding: "12px 18px", borderRadius: 14, border: "none",
            background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`,
            color: "#fff", fontSize: 20, fontWeight: 700, cursor: "pointer",
            display: "flex", alignItems: "center", justifyContent: "center", lineHeight: 1,
            boxShadow: "0 3px 10px rgba(2,132,199,0.25)", transition: "all 0.15s",
          }}
            onMouseOver={e => e.currentTarget.style.boxShadow = "0 5px 16px rgba(2,132,199,0.35)"}
            onMouseOut={e => e.currentTarget.style.boxShadow = "0 3px 10px rgba(2,132,199,0.25)"}
          >+</button>
        </div>
      </div>

      {/* Invite Modal */}
      {showInvite && (
        <div onClick={() => setShowInvite(false)} style={{ position: "fixed", inset: 0, background: "rgba(15,23,42,0.35)", backdropFilter: "blur(6px)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 100, padding: 20 }}>
          <div onClick={e => e.stopPropagation()} style={{ background: palette.card, borderRadius: 24, padding: "32px 28px", width: "100%", maxWidth: 400, boxShadow: "0 24px 64px rgba(7,89,133,0.18)", border: `1px solid ${palette.border}` }}>
            <h3 style={{ fontSize: 22, fontWeight: 700, color: palette.text, margin: "0 0 8px", fontFamily: fontDisplay }}>Convidar Pessoas</h3>
            <p style={{ fontSize: 13, color: palette.textSecondary, margin: "0 0 20px" }}>Busque por username ou compartilhe o link</p>
            <input type="text" placeholder="Buscar por username..." style={{
              width: "100%", padding: "13px 16px", borderRadius: 14, border: `1px solid ${palette.border}`,
              fontSize: 14, fontFamily: font, outline: "none", background: palette.surface, marginBottom: 16, boxSizing: "border-box",
            }}
              onFocus={e => { e.target.style.borderColor = palette.accentMid; e.target.style.boxShadow = "0 0 0 3px rgba(56,189,248,0.12)"; }}
              onBlur={e => { e.target.style.borderColor = palette.border; e.target.style.boxShadow = "none"; }}
            />
            <div style={{ padding: "14px 16px", borderRadius: 14, background: palette.accentLight, border: `1px solid ${palette.border}`, display: "flex", alignItems: "center", gap: 10, marginBottom: 20 }}>
              <span style={{ flex: 1, fontSize: 13, color: palette.textSecondary, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>nossalista.leoferolive.com.br/join/a8f3k...</span>
              <button style={{ padding: "6px 14px", borderRadius: 8, border: "none", background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`, color: "#fff", fontSize: 12, fontWeight: 600, fontFamily: font, cursor: "pointer", whiteSpace: "nowrap", boxShadow: "0 2px 6px rgba(2,132,199,0.2)" }}>Copiar</button>
            </div>
            <h4 style={{ fontSize: 12, fontWeight: 600, color: palette.textSecondary, margin: "0 0 12px", textTransform: "uppercase", letterSpacing: "0.6px" }}>Membros ({mockMembers.length})</h4>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {mockMembers.map(m => (
                <div key={m.id} style={{ display: "flex", alignItems: "center", gap: 12, padding: "8px 0" }}>
                  <div style={{ width: 34, height: 34, borderRadius: "50%", background: `linear-gradient(135deg, ${palette.accentMid}, ${palette.accent})`, display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 13, fontWeight: 700 }}>{m.avatar}</div>
                  <span style={{ flex: 1, fontSize: 14, fontWeight: 500, color: palette.text }}>{m.name}</span>
                  {m.online && <span style={{ fontSize: 11, color: palette.success, fontWeight: 600 }}>Online</span>}
                </div>
              ))}
            </div>
            <button onClick={() => setShowInvite(false)} style={{ marginTop: 20, width: "100%", padding: "13px", borderRadius: 14, border: `1px solid ${palette.border}`, background: "transparent", fontSize: 14, fontWeight: 600, fontFamily: font, color: palette.textSecondary, cursor: "pointer", transition: "all 0.15s" }}
              onMouseOver={e => e.currentTarget.style.background = palette.surface}
              onMouseOut={e => e.currentTarget.style.background = "transparent"}
            >Fechar</button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function NossaListaPrototype() {
  const [screen, setScreen] = useState(SCREENS.LOGIN);
  const [selectedList, setSelectedList] = useState(null);

  if (screen === SCREENS.LOGIN) return <LoginScreen onLogin={() => setScreen(SCREENS.HOME)} />;
  if (screen === SCREENS.LIST && selectedList) return <ListViewScreen list={selectedList} onBack={() => { setScreen(SCREENS.HOME); setSelectedList(null); }} />;
  return <HomeScreen onSelectList={(list) => { setSelectedList(list); setScreen(SCREENS.LIST); }} />;
}
