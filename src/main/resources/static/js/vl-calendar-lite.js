/* =========================================================
   VETLINK · CALENDARIO MENSUAL LIGERO (Thymeleaf-friendly)
   No depende de localStorage: recibe los datos ya renderizados
   por el servidor como argumentos de JS puro.
   ========================================================= */

const VlCalendarLite = {
    /**
     * render(containerId, opts)
     * opts: {
     *   mini: bool,
     *   initialDate: Date,
     *   marks: { 'YYYY-MM-DD': [{cls:'warn'|'danger'|''}, ...] },
     *   onSelectDay: function(fechaStr)
     * }
     */
    render(containerId, opts) {
        const container = document.getElementById(containerId);
        if (!container) return;
        const state = { current: opts.initialDate || new Date(), selected: null };

        const paint = () => {
            const year = state.current.getFullYear();
            const month = state.current.getMonth();
            const first = new Date(year, month, 1);
            const startOffset = (first.getDay() + 6) % 7; // lunes=0
            const daysInMonth = new Date(year, month + 1, 0).getDate();
            const daysInPrevMonth = new Date(year, month, 0).getDate();
            const monthLabel = first.toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
            const hoyStr = vlLiteDateToStr(new Date());

            let cells = '';
            for (let i = startOffset - 1; i >= 0; i--) {
                cells += `<div class="vl-calendar-day vl-day-out"><span class="vl-day-num">${daysInPrevMonth - i}</span></div>`;
            }
            for (let d = 1; d <= daysInMonth; d++) {
                const fechaStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
                const marks = opts.marks && opts.marks[fechaStr] ? opts.marks[fechaStr] : [];
                // Marcas con «tipo» (REC-01): se distinguen por forma y color; las antiguas ({cls}) siguen como puntos.
                const tipos = { cita: 0, pendiente: 0, cancelada: 0, cupo: 0 };
                const legado = [];
                marks.forEach(m => { if (m.tipo && m.tipo in tipos) tipos[m.tipo]++; else legado.push(m); });
                let dotsHTML = '';
                let etiqueta = '';
                if (tipos.cita || tipos.pendiente || tipos.cancelada || tipos.cupo) {
                    const puntos = (tipos.cita ? '<span class="vl-mk-cita"></span>' : '')
                        + (tipos.pendiente ? '<span class="vl-mk-pendiente"></span>' : '')
                        + (tipos.cancelada ? '<span class="vl-mk-cancelada"></span>' : '');
                    dotsHTML = `<div class="vl-day-marks" aria-hidden="true">${puntos ? `<div class="vl-day-dots">${puntos}</div>` : ''}`
                        + (tipos.cupo ? '<span class="vl-mk-cupo"></span>' : '') + '</div>';
                    const partes = [];
                    if (tipos.cita) partes.push(tipos.cita + (tipos.cita === 1 ? ' cita' : ' citas'));
                    if (tipos.pendiente) partes.push(tipos.pendiente + (tipos.pendiente === 1 ? ' reserva pendiente' : ' reservas pendientes'));
                    if (tipos.cancelada) partes.push(tipos.cancelada + (tipos.cancelada === 1 ? ' cancelada' : ' canceladas'));
                    if (tipos.cupo) partes.push(tipos.cupo + (tipos.cupo === 1 ? ' cupo disponible' : ' cupos disponibles'));
                    etiqueta = partes.join(', ');
                } else if (legado.length) {
                    dotsHTML = `<div class="vl-day-dot-row">${legado.slice(0, 4).map(m => `<span class="vl-day-dot ${m.cls || ''}"></span>`).join('')}</div>`;
                }
                const classes = ['vl-calendar-day'];
                if (fechaStr === hoyStr) classes.push('vl-day-today');
                if (fechaStr === state.selected) classes.push('vl-day-selected');
                const titulo = etiqueta ? ` title="${vlLiteEsc(etiqueta)}" aria-label="${d}: ${vlLiteEsc(etiqueta)}"` : '';
                cells += `<div class="${classes.join(' ')}" data-fecha="${fechaStr}" role="button" tabindex="0"${titulo}><span class="vl-day-num">${d}</span>${dotsHTML}</div>`;
            }
            const totalCells = startOffset + daysInMonth;
            const trailing = (7 - (totalCells % 7)) % 7;
            for (let d = 1; d <= trailing; d++) {
                cells += `<div class="vl-calendar-day vl-day-out"><span class="vl-day-num">${d}</span></div>`;
            }

            container.innerHTML = `
                <div class="vl-calendar ${opts.mini ? 'vl-calendar-mini' : ''}">
                    <div class="vl-calendar-head">
                        <h4>${monthLabel}</h4>
                        <div class="vl-calendar-nav">
                            <button type="button" class="vl-btn-icon" data-nav="-1"><i class="fas fa-chevron-left"></i></button>
                            <button type="button" class="vl-btn-icon" data-nav="1"><i class="fas fa-chevron-right"></i></button>
                        </div>
                    </div>
                    <div class="vl-calendar-grid">
                        <div class="vl-calendar-dow">Lun</div><div class="vl-calendar-dow">Mar</div><div class="vl-calendar-dow">Mié</div>
                        <div class="vl-calendar-dow">Jue</div><div class="vl-calendar-dow">Vie</div><div class="vl-calendar-dow">Sáb</div><div class="vl-calendar-dow">Dom</div>
                        ${cells}
                    </div>
                </div>`;

            container.querySelectorAll('[data-nav]').forEach(btn => btn.addEventListener('click', () => {
                state.current.setMonth(state.current.getMonth() + Number(btn.dataset.nav));
                paint();
            }));
            const elegir = (cell) => {
                state.selected = cell.dataset.fecha;
                paint();
                if (opts.onSelectDay) opts.onSelectDay(cell.dataset.fecha);
                const enfocada = container.querySelector(`.vl-calendar-day[data-fecha="${state.selected}"]`);
                if (enfocada) enfocada.focus();
            };
            container.querySelectorAll('.vl-calendar-day[data-fecha]').forEach(cell => {
                cell.addEventListener('click', () => elegir(cell));
                cell.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); elegir(cell); }
                });
            });
        };

        paint();
        return { repaint: paint, seleccionar: (f) => { state.selected = f; paint(); } };
    }
};

function vlLiteDateToStr(d) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function vlLiteEsc(str) {
    if (str === null || str === undefined) return '';
    return String(str).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function vlLiteFormatHora(horaStr) {
    if (!horaStr) return '';
    const [h, m] = horaStr.split(':').map(Number);
    const h12 = h % 12 === 0 ? 12 : h % 12;
    return `${h12}:${String(m).padStart(2, '0')} ${h < 12 ? 'a. m.' : 'p. m.'}`;
}
