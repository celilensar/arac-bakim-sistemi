import { loadGltf } from "node-three-gltf";
import * as THREE from "three";

const gltf = await loadGltf("./public/models/car.glb");
const scene = gltf.scene;

// Genel sinir kutusu
const whole = new THREE.Box3().setFromObject(scene);
const size = whole.getSize(new THREE.Vector3());
const center = whole.getCenter(new THREE.Vector3());
console.log("=== GENEL ===");
console.log("boyut (x,y,z):", size.x.toFixed(3), size.y.toFixed(3), size.z.toFixed(3));
console.log("merkez:", center.x.toFixed(3), center.y.toFixed(3), center.z.toFixed(3));

// Malzemeler
const mats = new Map();
scene.traverse((o) => {
  if (o.isMesh && o.material) {
    const arr = Array.isArray(o.material) ? o.material : [o.material];
    for (const m of arr) mats.set(m.uuid, m);
  }
});
console.log("\n=== MALZEMELER (" + mats.size + ") ===");
for (const m of mats.values()) {
  console.log(
    (m.name || "(isimsiz)").padEnd(22),
    "renk:", m.color?.getHexString?.() ?? "-",
    "| metal:", m.metalness ?? "-",
    "| rough:", m.roughness ?? "-",
    "| transp:", m.transparent
  );
}

// Mesh'ler + dunya sinir kutusu (parca konumlarini bulmak icin)
console.log("\n=== MESH'LER (adi + dunya merkezi + boyut) ===");
scene.traverse((o) => {
  if (o.isMesh) {
    const b = new THREE.Box3().setFromObject(o);
    const c = b.getCenter(new THREE.Vector3());
    const s = b.getSize(new THREE.Vector3());
    console.log(
      (o.name || "(isimsiz)").padEnd(26),
      "merkez:", c.x.toFixed(2), c.y.toFixed(2), c.z.toFixed(2),
      "| boyut:", s.x.toFixed(2), s.y.toFixed(2), s.z.toFixed(2)
    );
  }
});
