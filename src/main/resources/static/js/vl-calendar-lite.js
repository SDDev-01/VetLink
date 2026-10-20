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
                const dotsHTML = marks.length ? `<div class="vl-day-dot-row">${marks.slice(0, 4).map(m => `<span class="vl-day-dot ${m.cls || ''}"></span>`).join('')}</div>` : '';
                const classes = ['vl-calendar-day'];
                if (fechaStr === hoyStr) classes.push('vl-day-today');
                if (fechaStr === state.selected) classes.push('vl-day-selected');
                cells += `<div class="${classes.join(' ')}" data-fecha="${fechaStr}"><span class="vl-day-num">${d}</span>${dotsHTML}</div>`;
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
            container.querySelectorAll('.vl-calendar-day[data-fecha]').forEach(cell => cell.addEventListener('click', () => {
                state.selected = cell.dataset.fecha;
                paint();
                if (opts.onSelectDay) opts.onSelectDay(cell.dataset.fecha);
            }));
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
    const ampm = h >= 12 ? 'PM' : 'AM';
    const h12 = h % 12 === 0 ? 12 : h % 12;
    return `${h12}:${String(m).padStart(2, '0')} ${ampm}`;
}
