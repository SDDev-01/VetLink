document.addEventListener('DOMContentLoaded', function () {
    if (typeof VL_CITAS_MIAS === 'undefined') return;
    const citas = VL_CITAS_MIAS.filter(c => c.estado !== 'CANCELADA');

    const marks = {};
    citas.forEach(c => {
        if (!marks[c.fecha]) marks[c.fecha] = [];
        marks[c.fecha].push({ cls: c.estado === 'AGENDADA' ? '' : 'warn' });
    });

    function pintarAgenda(fechaStr) {
        const el = document.getElementById('citas-agenda');
        if (!el) return;
        const delDia = citas.filter(c => c.fecha === fechaStr).sort((a, b) => a.hora.localeCompare(b.hora));
        const titulo = new Date(fechaStr + 'T00:00:00').toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });

        let html = `<h4 style="font-size:0.95rem; text-transform:capitalize; margin-bottom:10px; color:#334155;">${titulo}</h4>`;
        if (!delDia.length) {
            html += `<p style="color:#94a3b8; font-size:0.85rem;">No tienes citas este día.</p>`;
        } else {
            html += '<div class="vl-agenda-list">' + delDia.map(c => `
                <div class="vl-agenda-item">
                    <div><strong style="font-size:0.88rem;">${vlLiteFormatHora(c.hora)} · ${vlLiteEsc(c.mascota)}</strong><br><span style="font-size:0.78rem; color:#64748b;">${vlLiteEsc(c.servicio)}</span></div>
                    <span class="vl-badge ${c.estado === 'AGENDADA' ? 'vl-badge-info' : 'vl-badge-warn'}">${c.estado}</span>
                </div>`).join('') + '</div>';
        }
        el.innerHTML = html;
    }

    VlCalendarLite.render('citas-calendar', {
        initialDate: new Date(),
        marks: marks,
        onSelectDay: pintarAgenda
    });

    pintarAgenda(vlLiteDateToStr(new Date()));
});
