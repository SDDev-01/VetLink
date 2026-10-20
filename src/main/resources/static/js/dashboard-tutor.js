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
        marks[c.fecha].push({ cls: c.estado === 'AGENDADA' ? '' : 'warn' });
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

        el.innerHTML = delDia.map(c => `
            <div class="vl-agenda-item">
                <div>
                    <strong style="font-size:0.85rem;">${vlLiteFormatHora(c.hora)} · ${vlLiteEsc(c.mascota)}</strong><br>
                    <span style="font-size:0.75rem; color:#64748b;">${vlLiteEsc(c.servicio)}</span>
                </div>
                <span class="vl-badge ${c.estado === 'AGENDADA' ? 'vl-badge-info' : c.estado === 'PENDIENTE' ? 'vl-badge-warn' : 'vl-badge-muted'}">${c.estado}</span>
            </div>
        `).join('');
    }

    VlCalendarLite.render('mini-calendar', {
        mini: true,
        initialDate: new Date(),
        marks: marks,
        onSelectDay: pintarAgenda
    });

    pintarAgenda(vlLiteDateToStr(new Date()));
});
