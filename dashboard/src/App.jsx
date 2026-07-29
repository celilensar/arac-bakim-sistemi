import { useEffect, useState, useCallback } from "react";
import "./App.css";

const API = "http://localhost:8080";

// Uyari metninden ("[UYARI] VH-003 - ...") seviyeyi cikar
function severityOf(text) {
  const m = text.match(/^\[(\w+)\]/);
  return m ? m[1] : "BILGI";
}

// Sadece gorsel renklendirme icin basit esikler (asil kurallar backend'de)
function metricClass(kind, v) {
  switch (kind) {
    case "engineTemp":     return v > 105 ? "bad" : v > 95 ? "warn" : "ok";
    case "oilLife":        return v < 15  ? "bad" : v < 30 ? "warn" : "ok";
    case "batteryVoltage": return v < 12  ? "bad" : v < 12.4 ? "warn" : "ok";
    case "tirePressure":   return v < 30  ? "warn" : "ok";
    default: return "ok";
  }
}

export default function App() {
  const [fleet, setFleet] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [connected, setConnected] = useState(false);

  const fetchFleet = useCallback(() => {
    fetch(`${API}/api/fleet`)
      .then((r) => r.json())
      .then((data) =>
        setFleet([...data].sort((a, b) => a.vehicleId.localeCompare(b.vehicleId)))
      )
      .catch(() => {});
  }, []);

  // Ilk yukleme + guvenlik icin arada bir tazele
  useEffect(() => {
    fetchFleet();
    const id = setInterval(fetchFleet, 8000);
    return () => clearInterval(id);
  }, [fetchFleet]);

  // SSE: canli uyari akisi (push). Tarayici bir kez baglanir, sunucu iter.
  useEffect(() => {
    const es = new EventSource(`${API}/api/stream/alerts`);
    es.onopen = () => setConnected(true);
    es.onerror = () => setConnected(false);
    es.addEventListener("alert", (e) => {
      const text = e.data;
      const item = {
        id: crypto.randomUUID(),
        text,
        severity: severityOf(text),
        time: new Date(),
      };
      setAlerts((prev) => [item, ...prev].slice(0, 60));
      fetchFleet(); // uyari gelince filoyu da tazele (olay-gudumlu)
    });
    return () => es.close();
  }, [fetchFleet]);

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>Araç Bakım Uyarı Paneli</h1>
          <p className="sub">Filo durumu ve canlı bakım uyarıları</p>
        </div>
        <div className={`status ${connected ? "live" : "off"}`}>
          <span className="dot" />
          {connected ? "CANLI" : "BAĞLANTI YOK"}
        </div>
      </header>

      <main className="layout">
        <section>
          <h2>
            Filo <span className="count">{fleet.length} araç</span>
          </h2>
          <div className="grid">
            {fleet.map((v) => (
              <article key={v.vehicleId} className="card">
                <div className="card-head">
                  <span className="vid">{v.vehicleId}</span>
                  <span className="km">
                    {Math.round(v.mileage).toLocaleString("tr-TR")} km
                  </span>
                </div>
                <div className="metrics">
                  <Metric label="Motor" val={v.engineTemp} unit="°C" cls={metricClass("engineTemp", v.engineTemp)} />
                  <Metric label="Yağ" val={v.oilLife} unit="%" cls={metricClass("oilLife", v.oilLife)} />
                  <Metric label="Akü" val={v.batteryVoltage} unit="V" cls={metricClass("batteryVoltage", v.batteryVoltage)} />
                  <Metric label="Lastik" val={v.tirePressure} unit="psi" cls={metricClass("tirePressure", v.tirePressure)} />
                </div>
              </article>
            ))}
            {fleet.length === 0 && (
              <p className="empty">Veri bekleniyor… (servisleri çalıştır)</p>
            )}
          </div>
        </section>

        <section>
          <h2>
            Canlı Uyarılar <span className="count">{alerts.length}</span>
          </h2>
          <ul className="alerts">
            {alerts.map((a) => (
              <li key={a.id} className={`alert ${a.severity}`}>
                <span className="badge">{a.severity}</span>
                <span className="text">{a.text.replace(/^\[\w+\]\s*/, "")}</span>
                <time>{a.time.toLocaleTimeString("tr-TR")}</time>
              </li>
            ))}
            {alerts.length === 0 && <li className="empty">Henüz uyarı yok…</li>}
          </ul>
        </section>
      </main>
    </div>
  );
}

function Metric({ label, val, unit, cls }) {
  const shown = typeof val === "number" ? val.toFixed(1) : val;
  return (
    <div className={`metric ${cls}`}>
      <span className="m-label">{label}</span>
      <span className="m-val">
        {shown}
        <em>{unit}</em>
      </span>
    </div>
  );
}
