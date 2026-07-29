// Sensor meta + durum/health hesaplari (gorsel; asil kurallar backend'de).

export const METRICS = {
  engineTemp:     { label: "Motor", unit: "°C" },
  oilLife:        { label: "Yağ", unit: "%" },
  batteryVoltage: { label: "Akü", unit: "V" },
  tirePressure:   { label: "Lastik", unit: "psi" },
};

export function sensorStatus(key, v) {
  if (v == null) return "ok";
  switch (key) {
    case "engineTemp":     return v > 105 ? "bad" : v > 95 ? "warn" : "ok";
    case "oilLife":        return v < 15 ? "bad" : v < 30 ? "warn" : "ok";
    case "batteryVoltage": return v < 12 ? "bad" : v < 12.4 ? "warn" : "ok";
    case "tirePressure":   return v < 30 ? "warn" : "ok";
    default: return "ok";
  }
}

// Bir aracin tum sensor durumlari: { engineTemp: "bad", ... }
export function vehicleStatuses(v) {
  const out = {};
  for (const k of Object.keys(METRICS)) out[k] = sensorStatus(k, v?.[k]);
  return out;
}

// 0-100 saglik skoru
export function healthScore(v) {
  let score = 100;
  for (const k of Object.keys(METRICS)) {
    const s = sensorStatus(k, v?.[k]);
    if (s === "bad") score -= 28;
    else if (s === "warn") score -= 12;
  }
  return Math.max(0, Math.round(score));
}

// Bir aracin aktif (warn/bad) uyari sayisi
export function issueCount(v) {
  let n = 0;
  for (const k of Object.keys(METRICS)) if (sensorStatus(k, v?.[k]) !== "ok") n++;
  return n;
}

export const statusColor = (s) =>
  s === "bad" ? "#ff5252" : s === "warn" ? "#ffb020" : "#37d67a";
