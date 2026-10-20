/* =========================================================================
   directorio.js - adaptado del prototipo original de VetLink.
   Se quito el dataset "VITRINA" (clinicas ficticias de muestra con fotos de
   stock) porque ahora el directorio muestra unicamente veterinarias reales
   de la base de datos (variable VETE_DATOS, inyectada por el servidor via
   Thymeleaf en veterinarias/lista.html). El resto de la logica de mapa y
   filtros se conserva igual que en el original.
========================================================================= */

let map;
let markers = {};
let filtroServicio = "";
let filtroFecha = "";

const DIAS_SEMANA_ORDEN = ["Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"];

document.addEventListener('DOMContentLoaded', () => {
    initMap();
    solicitarUbicacion();
    renderCards(VETE_DATOS);
});

function initMap() {
    const colombiaBounds = L.latLngBounds([(-4.3), (-79.1)], [12.6, (-66.8)]);
    map = L.map('map', { zoomControl: false, maxBounds: colombiaBounds })
        .setView([4.57, -74.30], 6);

    const mapaColombia = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>',
        maxZoom: 19
    }).addTo(map);

    mapaColombia.on('tileerror', () => {
        const infoEl = document.querySelector('.map-overlay-info');
        if (infoEl) {
            infoEl.innerHTML = '<i class="fas fa-triangle-exclamation"></i> No se pudo cargar el mapa';
        }
    });

    const coordenadas = [];
    VETE_DATOS.filter(v => Number.isFinite(Number(v.lat)) && Number.isFinite(Number(v.lng)))
    .forEach(v => {
        v.lat = Number(v.lat);
        v.lng = Number(v.lng);
        const marker = L.marker([v.lat, v.lng]).addTo(map);
        marker.bindPopup(`<b>${v.nombre}</b><br><small style="color:#2563eb">Registrada en VetLink</small>`);
        markers[v.id] = marker;
        coordenadas.push([v.lat, v.lng]);
    });

    if (coordenadas.length > 1) {
        map.fitBounds(coordenadas, { padding: [30, 30], maxZoom: 13 });
    } else if (coordenadas.length === 1) {
        map.setView(coordenadas[0], 13);
    }
    setTimeout(() => map.invalidateSize(), 200);
}

/* ---------- Geolocalización ---------- */
function solicitarUbicacion() {
    const infoEl = document.querySelector('.map-overlay-info');
    if (!navigator.geolocation) return;

    navigator.geolocation.getCurrentPosition(
        (pos) => {
            window.vlUbicacionActual = { lat: pos.coords.latitude, lng: pos.coords.longitude };
            if (infoEl) infoEl.innerHTML = '<i class="fas fa-location-crosshairs"></i> Mostrando veterinarias cerca de ti';
            if (map) {
                L.circleMarker([pos.coords.latitude, pos.coords.longitude], { radius: 8, color: '#2563eb', fillColor: '#60a5fa', fillOpacity: 0.9 })
                    .addTo(map).bindPopup('Tu ubicación');
                map.flyTo([pos.coords.latitude, pos.coords.longitude], 13, { duration: 1.2 });
            }
            renderCards(aplicarFiltros());
        },
        () => { if (infoEl) infoEl.innerHTML = '<i class="fas fa-map-marked-alt"></i> Explorando Bogotá'; },
        { timeout: 6000 }
    );
}

function vlDistanciaKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

/* ---------- Filtros: servicio + fecha ---------- */
function aplicarFiltros() {
    const search = (document.getElementById('searchInput').value || "").toLowerCase();
    filtroServicio = document.getElementById('filterServicio') ? document.getElementById('filterServicio').value : "";
    filtroFecha = document.getElementById('filterFecha') ? document.getElementById('filterFecha').value : "";

    let data = VETE_DATOS.filter(v =>
        v.nombre.toLowerCase().includes(search) || (v.servicios || []).some(t => t.toLowerCase().includes(search))
    );

    if (filtroServicio) {
        data = data.filter(v => (v.servicios || []).includes(filtroServicio));
    }

    if (filtroFecha) {
        // Nota: el filtro de horario por fecha requeriria consultar horario_veterinaria
        // por veterinaria; se deja pasante mientras esa integracion no este conectada aqui.
    }

    if (window.vlUbicacionActual) {
        data = data.map(v => Object.assign({}, v, { distancia: vlDistanciaKm(window.vlUbicacionActual.lat, window.vlUbicacionActual.lng, v.lat, v.lng) }));
        data.sort((a, b) => (a.distancia ?? 999) - (b.distancia ?? 999));
    }

    return data;
}

function renderCards(data) {
    const grid = document.getElementById('directoryGrid');
    if (!data.length) {
        grid.innerHTML = `<div class="vl-empty" style="grid-column:1/-1;"><i class="fas fa-magnifying-glass"></i><h4>Sin resultados</h4><p>Intenta con otro servicio o texto de búsqueda.</p></div>`;
        return;
    }
    grid.innerHTML = data.map(v => `
        <div class="vet-card" id="card-${v.id}" ${Number.isFinite(Number(v.lat)) && Number.isFinite(Number(v.lng)) ? `onclick="focusVet(${v.id}, ${Number(v.lat)}, ${Number(v.lng)})"` : ""}>
            <div class="vet-img-container">
                <img src="https://ui-avatars.com/api/?name=${encodeURIComponent(v.nombre)}&background=2563eb&color=fff&size=500" alt="${v.nombre}">
                <div class="rating-tag">⭐ ${v.rating}</div>
                <div class="rating-tag" style="left:14px; right:auto; background:#dcfce7; color:#15803d;">En VetLink</div>
            </div>
            <div class="vet-details">
                <p class="badge-top" style="background:#e0e7ff; color:#4338ca; margin-bottom:10px">${(v.servicios || []).slice(0, 2).join(" • ") || "Consulta General"}</p>
                <h3>${v.nombre}</h3>
                <p><i class="fas fa-map-marker-alt"></i> ${v.ciudad || "Colombia"} ${v.distancia != null ? `· <strong>${v.distancia.toFixed(1)} km</strong>` : ""}</p>
                <a href="/directorio/${v.id}" class="vl-btn vl-btn-ghost vl-btn-sm" style="margin-top:8px;" onclick="event.stopPropagation();">Ver perfil</a>
                <a href="/citas/agendar?idVeterinaria=${v.id}" class="vl-btn vl-btn-primary vl-btn-sm" style="margin-top:8px;" onclick="event.stopPropagation();">Agendar</a>
            </div>
        </div>
    `).join('');
}

function focusVet(id, lat, lng) {
    if (!map || !Number.isFinite(Number(lat)) || !Number.isFinite(Number(lng))) return;
    document.querySelectorAll('.vet-card').forEach(c => c.style.borderColor = "transparent");
    const card = document.getElementById(`card-${id}`);
    if (card) card.style.borderColor = "var(--primary)";

    map.flyTo([lat, lng], 16, { duration: 1.2 });
    if (markers[id]) markers[id].openPopup();
}

function filtrarDirectorio() {
    renderCards(aplicarFiltros());
}
