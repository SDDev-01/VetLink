/* =========================================================
   VETLINK · Mini calendario de disponibilidad
   Días disponibles en azul (con punto), no disponibles en gris y deshabilitados,
   hoy con contorno, seleccionado resaltado. Cada día anuncia su estado a lectores
   de pantalla (no depende solo del color).

   VlMiniCal.crear(contenedor, {
       cargarMes: function (aaaaMm) -> Promise<{ 'aaaa-mm-dd': cupos }>,
       alSeleccionar: function (aaaaMmDd) {}
   })
   ========================================================= */
var VlMiniCal = (function () {
    'use strict';

    var DIAS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

    function iso(d) {
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    }

    function crear(cont, opts) {
        var hoy = new Date();
        hoy.setHours(0, 0, 0, 0);
        var estado = { mes: new Date(hoy.getFullYear(), hoy.getMonth(), 1), dias: {}, sel: null, cargando: false, error: false };

        function pintar() {
            var y = estado.mes.getFullYear(), m = estado.mes.getMonth();
            var etiqueta = estado.mes.toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
            var offset = (new Date(y, m, 1).getDay() + 6) % 7;
            var total = new Date(y, m + 1, 0).getDate();
            var esMesActual = y === hoy.getFullYear() && m === hoy.getMonth();
            var html = '<div class="vl-minical-head">' +
                '<button type="button" class="vl-btn-icon" data-nav="-1" aria-label="Mes anterior"' + (esMesActual ? ' disabled' : '') + '><i class="fas fa-chevron-left"></i></button>' +
                '<h4 aria-live="polite">' + etiqueta + '</h4>' +
                '<button type="button" class="vl-btn-icon" data-nav="1" aria-label="Mes siguiente"><i class="fas fa-chevron-right"></i></button></div>';
            if (estado.cargando) {
                html += '<div class="vl-loading" role="status"><span class="vl-spinner"></span> Consultando disponibilidad…</div>';
            } else if (estado.error) {
                html += '<div class="vl-alert vl-alert-err" role="alert">No pudimos consultar la disponibilidad. Intenta de nuevo.</div>';
            } else {
                html += '<div class="vl-minical-grid" role="grid">';
                DIAS.forEach(function (d) { html += '<div class="vl-minical-dow" aria-hidden="true">' + d + '</div>'; });
                for (var i = 0; i < offset; i++) html += '<span class="vl-minical-day vacio" aria-hidden="true"></span>';
                var hayAlguno = false;
                for (var d = 1; d <= total; d++) {
                    var f = new Date(y, m, d);
                    var clave = iso(f);
                    var cupos = estado.dias[clave] || 0;
                    var pasado = f < hoy;
                    var libre = !pasado && cupos > 0;
                    if (libre) hayAlguno = true;
                    var cls = ['vl-minical-day'];
                    if (libre) cls.push('libre');
                    if (f.getTime() === hoy.getTime()) cls.push('hoy');
                    if (clave === estado.sel) cls.push('sel');
                    var desc = f.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }) +
                        (pasado ? ', fecha pasada' : (libre ? ', ' + cupos + (cupos === 1 ? ' cupo disponible' : ' cupos disponibles') : ', sin disponibilidad')) +
                        (f.getTime() === hoy.getTime() ? ', hoy' : '');
                    html += '<button type="button" class="' + cls.join(' ') + '" data-fecha="' + clave + '" aria-label="' + desc + '"' +
                        (libre ? '' : ' disabled') + (clave === estado.sel ? ' aria-pressed="true"' : '') + '>' + d + '</button>';
                }
                html += '</div>';
                if (!hayAlguno) html += '<p class="vl-small vl-muted" style="margin:10px 0 0">No hay cupos disponibles este mes. Prueba el mes siguiente.</p>';
                html += '<div class="vl-minical-legend"><span><i style="background:#2563eb"></i>Disponible</span>' +
                    '<span><i style="background:#f1f5f9;border:1px solid #cbd5e1"></i>Sin cupos / pasado</span>' +
                    '<span><i style="background:#fff;border:2px solid #0f172a"></i>Hoy</span>' +
                    '<span><i style="background:#0f172a"></i>Seleccionado</span></div>';
            }
            cont.innerHTML = html;
            cont.querySelectorAll('[data-nav]').forEach(function (b) {
                b.addEventListener('click', function () {
                    estado.mes = new Date(y, m + Number(b.dataset.nav), 1);
                    cargar();
                });
            });
            cont.querySelectorAll('.vl-minical-day.libre').forEach(function (b) {
                b.addEventListener('click', function () {
                    estado.sel = b.dataset.fecha;
                    pintar();
                    var nuevo = cont.querySelector('[data-fecha="' + estado.sel + '"]');
                    if (nuevo) nuevo.focus();
                    if (opts.alSeleccionar) opts.alSeleccionar(estado.sel);
                });
            });
        }

        function cargar() {
            estado.cargando = true;
            estado.error = false;
            pintar();
            var y = estado.mes.getFullYear(), m = estado.mes.getMonth() + 1;
            Promise.resolve(opts.cargarMes(y + '-' + String(m).padStart(2, '0')))
                .then(function (dias) { estado.dias = dias || {}; })
                .catch(function () { estado.error = true; })
                .finally(function () { estado.cargando = false; pintar(); });
        }

        cargar();
        return {
            recargar: function () { estado.sel = null; cargar(); },
            seleccion: function () { return estado.sel; }
        };
    }

    return { crear: crear };
})();

/** "14:30" → "2:30 p. m." (mismo formato que el servidor). */
function vlHora(hhmm) {
    if (!hhmm) return '';
    var p = String(hhmm).split(':');
    var h = Number(p[0]), m = Number(p[1] || 0);
    var h12 = h % 12 === 0 ? 12 : h % 12;
    return h12 + ':' + String(m).padStart(2, '0') + (h < 12 ? ' a. m.' : ' p. m.');
}

function vlEsc(s) {
    if (s === null || s === undefined) return '';
    return String(s).replace(/[&<>"']/g, function (c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
}
