import { Suspense, useEffect, useRef, useState } from "react";
import { Canvas } from "@react-three/fiber";
import {
  CameraControls,
  Environment,
  ContactShadows,
  AdaptiveDpr,
  Html,
  useGLTF,
} from "@react-three/drei";
import * as THREE from "three";
import cityHdri from "@pmndrs/assets/hdri/city.exr";
import { METRICS, statusColor } from "./lib/vehicle";

const PAINT_COLOR = "#34373d";

const HOTSPOTS = [
  { id: "engine",  key: "engineTemp",     label: "Motor",  pos: [0, 0.82, 1.3] },
  { id: "battery", key: "batteryVoltage", label: "Akü",    pos: [-0.5, 0.74, 1.08] },
  { id: "oil",     key: "oilLife",        label: "Yağ",    pos: [0.5, 0.74, 1.08] },
  { id: "tireFR",  key: "tirePressure",   label: "Lastik", pos: [0.82, 0.26, 1.5] },
  { id: "tireFL",  key: "tirePressure",   label: "Lastik", pos: [-0.82, 0.26, 1.5] },
  { id: "tireRR",  key: "tirePressure",   label: "Lastik", pos: [0.82, 0.26, -1.0] },
  { id: "tireRL",  key: "tirePressure",   label: "Lastik", pos: [-0.82, 0.26, -1.0] },
];

const DEMO_STATUS = { engineTemp: "bad", oilLife: "warn", batteryVoltage: "ok", tirePressure: "warn" };
const colorOf = (s) => statusColor(s);

function CarModel() {
  const { scene } = useGLTF("/models/car.glb");
  useEffect(() => {
    scene.traverse((o) => {
      if (!o.isMesh || !o.material) return;
      const m = o.material;
      const c = m.color;
      if (!c) return;
      const lum = 0.299 * c.r + 0.587 * c.g + 0.114 * c.b;
      const isGlass = m.transparent && m.opacity < 0.9;
      const emis = m.emissive;
      const isEmissive = (m.emissiveIntensity ?? 0) > 0 && emis && emis.r + emis.g + emis.b > 0.05;
      if (!isGlass && !isEmissive && lum > 0.35) c.set(PAINT_COLOR);
    });
  }, [scene]);
  return <primitive object={scene} />;
}

function Hotspot({ data, status, active, onSelect }) {
  return (
    <Html position={data.pos} center zIndexRange={[20, 0]}>
      <div
        className="hotspot"
        style={{ "--c": colorOf(status) }}
        data-status={status}
        data-active={active}
        onPointerDown={(e) => e.stopPropagation()}
        onClick={(e) => { e.stopPropagation(); onSelect(data); }}
        title={data.label}
      />
    </Html>
  );
}

function SensorPopover({ data, status, value }) {
  return (
    <Html position={data.pos} zIndexRange={[40, 0]} style={{ pointerEvents: "none" }}>
      <div className="hs-pop">
        <div
          className="glass font-display"
          style={{ pointerEvents: "auto", minWidth: 148, borderRadius: 16, padding: "11px 14px", color: "#e6e9f2" }}
          onPointerDown={(e) => e.stopPropagation()}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span style={{ width: 9, height: 9, borderRadius: 999, background: statusColor(status) }} />
            <span style={{ fontWeight: 700, fontSize: 13 }}>{data.label}</span>
          </div>
          <div className="font-num" style={{ fontSize: 26, fontWeight: 700, marginTop: 3, color: statusColor(status) }}>
            {typeof value === "number" ? value.toFixed(1) : "—"}
            <span style={{ fontSize: 12, color: "#8890ac", marginLeft: 4 }}>{METRICS[data.key]?.unit}</span>
          </div>
        </div>
      </div>
    </Html>
  );
}

function Loader() {
  return <Html center><div style={{ color: "#8890ac", fontSize: 13 }}>Model yükleniyor…</div></Html>;
}

export default function CarViewer({ statuses = DEMO_STATUS, vehicle = null }) {
  const controls = useRef();
  const [selected, setSelected] = useState(null);
  const center = new THREE.Vector3(0, 0.5, 0);

  useEffect(() => {
    controls.current?.setLookAt(3.8, 2.2, 4.6, 0, 0.5, 0, false);
  }, []);

  function focus(data) {
    setSelected(data);
    const p = new THREE.Vector3(...data.pos);
    const dir = p.clone().sub(center).normalize();
    const cam = p.clone().add(dir.multiplyScalar(2.1)).add(new THREE.Vector3(0, 0.45, 0));
    controls.current?.setLookAt(cam.x, cam.y, cam.z, p.x, p.y, p.z, true);
  }

  function reset() {
    setSelected(null);
    controls.current?.setLookAt(3.8, 2.2, 4.6, 0, 0.5, 0, true);
  }

  return (
    <Canvas
      shadows
      dpr={[1, 1.5]}
      camera={{ position: [3.8, 2.2, 4.6], fov: 45 }}
      gl={{ toneMappingExposure: 0.7 }}
      onPointerMissed={reset}
    >
      <color attach="background" args={["#0b1020"]} />
      <Suspense fallback={<Loader />}>
        <Environment files={cityHdri} environmentIntensity={0.5} />
        <group scale={80}>
          <CarModel />
        </group>
        {HOTSPOTS.map((h) => (
          <Hotspot
            key={h.id}
            data={h}
            status={statuses[h.key] || "ok"}
            active={selected?.key === h.key}
            onSelect={focus}
          />
        ))}
        {selected && (
          <SensorPopover data={selected} status={statuses[selected.key] || "ok"} value={vehicle?.[selected.key]} />
        )}
        <ContactShadows position={[0, 0, 0]} opacity={0.5} scale={9} blur={2.6} far={3} frames={1} />
      </Suspense>
      <CameraControls ref={controls} minDistance={1.4} maxDistance={12} smoothTime={0.45} />
      <AdaptiveDpr pixelated={false} />
    </Canvas>
  );
}

useGLTF.preload("/models/car.glb");
