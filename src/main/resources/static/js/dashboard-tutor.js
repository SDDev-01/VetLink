/* =========================================================
   VETLINK · DASHBOARD DEL DUEÑO — calendario + agenda del día
   Usa VL_CITAS_TUTOR (inyectado por Thymeleaf en dashboard/tutor.html)
   ========================================================= */
document.addEventListener('DOMContentLoaded', function () {
    if (typeof VL_CITAS_TUTOR === 'undefined') return;

    const citas = VL_CITAS_TUTOR.filter(c => c.estado !== 'CANCELADA');

    const marks = {};
    citas.forEach(c => {
        if (!marks[c.fecha]) marks[c.fecha] = [];
        marks[c.fecha].push({ cls: c.estado === 'CONFIRMADA' || c.estado === 'ATENDIDA' ? '' : (c.estado === 'CANCELADA' || c.estado === 'RECHAZADA' ? 'danger' : 'warn') });
    });

    function pintarAgenda(fechaStr) {
        const el = document.getElementById('mini-agenda');
        if (!el) return;
        const delDia = citas.filter(c => c.fecha === fechaStr).sort((a, b) => a.hora.localeCompare(b.hora));
        const fechaObj = new Date(fechaStr + 'T00:00:00');
        const titulo = fechaObj.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });

        if (!delDia.length) {
            el.innerHTML = `<p style="color:#94a3b8; font-size:0.82rem;">Sin citas el ${titulo}.</p>`;
            return;
        }

        // Hora, mascota, servicio y estado en zonas separadas: el estado nunca tapa el servicio
        // y la hora no se parte (ver .vl-agenda-item en vl-v10.css).
        el.innerHTML = '<div class="vl-agenda-lista">' + delDia.map(c => `
            <a class="vl-agenda-item" href="/citas/${encodeURIComponent(c.idCita)}">
                <span class="vl-agenda-hora">${vlLiteEsc(c.horaTexto || vlLiteFormatHora(c.hora))}</span>
                <span class="vl-agenda-main">
                    <strong>${vlLiteEsc(c.mascota)}</strong>
                    <span class="vl-agenda-servicio">${vlLiteEsc(c.servicio)}</span>
                </span>
                <span class="vl-st ${vlLiteEsc(c.estadoClase)}">${vlLiteEsc(c.estadoEtiqueta || c.estado)}</span>
            </a>
        `).join('') + '</div>';
    }

    VlCalendarLite.render('mini-calendar', {
        mini: true,
        initialDate: new Date(),
        marks: marks,
        onSelectDay: pintarAgenda
    });

    pintarAgenda(vlLiteDateToStr(new Date()));
});
