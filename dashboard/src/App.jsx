import { useEffect, useState, useCallback } from "react";
import CarViewer from "./CarViewer";
import {
  METRICS,
  vehicleStatuses,
  sensorStatus,
  healthScore,
  issueCount,
  statusColor,
} from "./lib/vehicle";

const API = "http://localhost:8080";
const severityOf = (t) => t.match(/^\[(\w+)\]/)?.[1] || "BILGI";

function HealthRing({ value, size = 46, stroke = 5 }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const off = c * (1 - value / 100);
  const col = value >= 80 ? "#37d67a" : value >= 55 ? "#ffb020" : "#ff5252";
  return (
    <svg width={size} height={size} className="shrink-0">
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth={stroke} />
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={col} strokeWidth={stroke}
        strokeLinecap="round" strokeDasharray={c} strokeDashoffset={off}
        transform={`rotate(-90 ${size / 2} ${size / 2})`} style={{ transition: "stroke-dashoffset .5s ease" }} />
      <text x="50%" y="53%" textAnchor="middle" dominantBaseline="middle"
        className="font-num" fontSize={size * 0.3} fontWeight="700" fill="#e6e9f2">{value}</text>
    </svg>
  );
}

export default function App() {
  const [fleet, setFleet] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [connected, setConnected] = useState(false);
  const [selectedId, setSelectedId] = useState(null);

  const fetchFleet = useCallback(() => {
    fetch(`${API}/api/fleet`)
      .then((r) => r.json())
      .then((d) => setFleet([...d].sort((a, b) => a.vehicleId.localeCompare(b.vehicleId))))
      .catch(() => {});
  }, []);

  useEffect(() => {
    fetchFleet();
    const id = setInterval(fetchFleet, 8000);
    return () => clearInterval(id);
  }, [fetchFleet]);

  useEffect(() => {
    const es = new EventSource(`${API}/api/stream/alerts`);
    es.onopen = () => setConnected(true);
    es.onerror = () => setConnected(false);
    es.addEventListener("alert", (e) => {
      const text = e.data;
      setAlerts((p) => [{ id: crypto.randomUUID(), text, severity: severityOf(text), time: new Date() }, ...p].slice(0, 60));
      fetchFleet();
    });
    return () => es.close();
  }, [fetchFleet]);

  const selected = fleet.find((v) => v.vehicleId === selectedId) || fleet[0] || null;
  const statuses = vehicleStatuses(selected);
  const totalIssues = fleet.reduce((n, v) => n + issueCount(v), 0);
  const criticalCount = fleet.filter((v) => Object.keys(METRICS).some((k) => sensorStatus(k, v[k]) === "bad")).length;
  const avgHealth = fleet.length ? Math.round(fleet.reduce((s, v) => s + healthScore(v), 0) / fleet.length) : 0;

  const KPIS = [
    { label: "Filo", value: `${fleet.length}`, sub: "araç", color: "#e6e9f2" },
    { label: "Aktif uyarı", value: totalIssues, color: totalIssues ? "#ffb020" : "#e6e9f2" },
    { label: "Kritik", value: criticalCount, color: criticalCount ? "#ff5252" : "#e6e9f2" },
    { label: "Ort. sağlık", value: `%${avgHealth}`, color: avgHealth >= 80 ? "#37d67a" : "#ffb020" },
  ];

  return (
    <div className="relative h-screen w-screen overflow-hidden bg-[#0b1020] text-white">
      {/* Tam ekran 3D arka plan */}
      <div className="absolute inset-0">
        <CarViewer statuses={statuses} vehicle={selected} />
      </div>

      {/* UI overlay (glass) */}
      <div className="pointer-events-none absolute inset-0 flex flex-col gap-4 p-4 md:p-6">
        {/* Ust satir */}
        <div className="flex items-start justify-between gap-3">
          <div className="glass pointer-events-auto rounded-2xl px-5 py-3">
            <h1 className="font-display text-base font-bold md:text-lg">Araç Bakım Uyarı Paneli</h1>
            <p className="text-[11px] text-white/45">Filo · 3B araç görünümü · canlı uyarılar</p>
          </div>

          <div className="glass pointer-events-auto hidden divide-x divide-white/10 rounded-2xl md:flex">
            {KPIS.map((k) => (
              <div key={k.label} className="px-5 py-2.5 text-center">
                <div className="text-[10px] uppercase tracking-wide text-white/45">{k.label}</div>
                <div className="font-num text-xl font-bold" style={{ color: k.color }}>
                  {k.value}{k.sub && <span className="ml-1 text-[11px] font-medium text-white/40">{k.sub}</span>}
                </div>
              </div>
            ))}
          </div>

          <div className="glass pointer-events-auto flex items-center gap-2 rounded-full px-4 py-2 text-xs font-bold"
            style={{ color: connected ? "#37d67a" : "#ff6b6b" }}>
            <span className="h-2 w-2 rounded-full bg-current" style={connected ? { boxShadow: "0 0 8px currentColor" } : {}} />
            {connected ? "CANLI" : "BAĞLANTI YOK"}
          </div>
        </div>

        {/* Orta satir: sol arac paneli | (arac ortada) | sag uyarilar */}
        <div className="flex min-h-0 flex-1 items-start justify-between gap-4">
          {/* Sol: secili arac */}
          <div className="glass pointer-events-auto hidden w-[250px] rounded-2xl p-4 lg:block">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-[11px] uppercase tracking-wide text-white/45">Seçili araç</div>
                <div className="font-display text-2xl font-bold">{selected?.vehicleId ?? "—"}</div>
              </div>
              {selected && <HealthRing value={healthScore(selected)} />}
            </div>
            <div className="mt-4 flex flex-col gap-1.5">
              {Object.entries(METRICS).map(([k, meta]) => {
                const s = sensorStatus(k, selected?.[k]);
                const val = typeof selected?.[k] === "number" ? selected[k].toFixed(1) : "—";
                return (
                  <div key={k} className="glass-soft flex items-center justify-between rounded-xl px-3 py-2">
                    <span className="flex items-center gap-2 text-[13px] text-white/70">
                      <span className="h-2 w-2 rounded-full" style={{ background: statusColor(s) }} />
                      {meta.label}
                    </span>
                    <span className="font-num text-sm font-bold" style={{ color: statusColor(s) }}>
                      {val}<span className="ml-0.5 text-[10px] text-white/40">{meta.unit}</span>
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="flex-1" />

          {/* Sag: canli uyarilar */}
          <div className="glass pointer-events-auto flex max-h-full w-[330px] flex-col rounded-2xl p-4">
            <div className="mb-3 flex items-center gap-2">
              <span className="h-2.5 w-2.5 rounded-full" style={{ background: connected ? "#37d67a" : "#ff6b6b" }} />
              <span className="font-display text-sm font-bold">Canlı Uyarılar</span>
              <span className="ml-auto text-xs text-white/40">{alerts.length}</span>
            </div>
            <div className="thin-scroll flex flex-col gap-2 overflow-y-auto pr-1">
              {alerts.map((a) => {
                const col = a.severity === "KRITIK" ? "#ff5252" : a.severity === "UYARI" ? "#ffb020" : "#5b8cff";
                return (
                  <div key={a.id} className="glass-soft rounded-xl border-l-4 px-3 py-2" style={{ borderLeftColor: col }}>
                    <div className="font-num text-[10px] font-bold" style={{ color: col }}>
                      {a.severity} · {a.time.toLocaleTimeString("tr-TR")}
                    </div>
                    <div className="text-[13px] text-white/85">{a.text.replace(/^\[\w+\]\s*/, "")}</div>
                  </div>
                );
              })}
              {alerts.length === 0 && <div className="text-sm text-white/40">Henüz uyarı yok…</div>}
            </div>
          </div>
        </div>

        {/* Alt: filo seridi */}
        <div className="glass pointer-events-auto thin-scroll flex items-center gap-3 overflow-x-auto rounded-2xl p-3">
          <span className="shrink-0 pl-1 pr-2 text-[11px] font-bold uppercase tracking-wide text-white/45">Filo</span>
          {fleet.map((v) => {
            const h = healthScore(v);
            const n = issueCount(v);
            const sel = v.vehicleId === selected?.vehicleId;
            return (
              <button key={v.vehicleId} onClick={() => setSelectedId(v.vehicleId)}
                className={`flex shrink-0 items-center gap-2.5 rounded-xl border px-3 py-2 transition ${sel ? "border-white/40 bg-white/10" : "border-white/10 hover:border-white/25"}`}>
                <HealthRing value={h} size={38} stroke={4} />
                <div className="text-left">
                  <div className="font-display text-sm font-bold">{v.vehicleId}</div>
                  <div className="text-[11px]" style={{ color: n ? (h < 55 ? "#ff6b6b" : "#ffb020") : "#37d67a" }}>
                    {n ? `${n} uyarı` : "iyi"}
                  </div>
                </div>
              </button>
            );
          })}
          {fleet.length === 0 && <span className="text-sm text-white/40">Veri bekleniyor… (servisleri çalıştır)</span>}
        </div>
      </div>
    </div>
  );
}
