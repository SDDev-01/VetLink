/* VETLINK · Razas dependientes de la especie (DUE-02).
   - La raza inicia vacía («Seleccione una raza»), nunca en la primera opción.
   - Solo se listan las razas de la especie elegida, en orden alfabético.
   - Buscador: filtra las razas mientras se escribe (sin distinguir tildes ni mayúsculas).
   - «Otra raza»: muestra un campo para escribirla; el servidor evita duplicados.
   - Si cambia la especie y la raza ya no corresponde, se limpia.
   Uso:
     <select|input data-especie-de="idSelectRaza" data-otra-raza="idContenedorOtra"> (la especie)
     <select id="idSelectRaza"> con data-especie en cada opción
     <div id="idContenedorOtra" hidden> con <input name="razaPersonalizada"> </div>
   La especie puede ser un <input type="hidden"> (edición: la especie no se cambia). */
(function () {
    'use strict';
    var OTRA = '__otra';

    function sinTildes(s) {
        return (s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
    }

    function enlazar(selEspecie) {
        var selRaza = document.getElementById(selEspecie.dataset.especieDe);
        if (!selRaza || selRaza._vlRazas) return;
        selRaza._vlRazas = true;
        var contOtra = selEspecie.dataset.otraRaza ? document.getElementById(selEspecie.dataset.otraRaza) : null;
        var inputOtra = contOtra ? contOtra.querySelector('input') : null;
        var todas = Array.prototype.slice.call(selRaza.querySelectorAll('option[data-especie]')).map(function (o) { return o.cloneNode(true); });
        var collator = new Intl.Collator('es', { sensitivity: 'base' });
        todas.sort(function (a, b) { return collator.compare(a.textContent, b.textContent); });

        // Buscador accesible encima del selector.
        var buscador = document.createElement('input');
        buscador.type = 'search';
        buscador.className = 'vl-raza-buscador';
        buscador.placeholder = 'Buscar raza…';
        buscador.setAttribute('aria-label', 'Buscar raza');
        buscador.setAttribute('aria-controls', selRaza.id);
        buscador.autocomplete = 'off';
        selRaza.insertAdjacentElement('beforebegin', buscador);

        var otraElegida = !!(inputOtra && inputOtra.value.trim());

        function mostrarOtra(si) {
            if (!contOtra) return;
            contOtra.hidden = !si;
            if (inputOtra) {
                inputOtra.required = si;
                if (!si) inputOtra.value = '';
            }
        }

        function pintar() {
            var especie = selEspecie.value;
            var actual = otraElegida ? OTRA : selRaza.value;
            var filtro = sinTildes(buscador.value);
            selRaza.innerHTML = '';
            var ph = document.createElement('option');
            ph.value = '';
            ph.textContent = especie ? 'Seleccione una raza' : 'Primero seleccione la especie';
            selRaza.appendChild(ph);
            var sigue = false, visibles = 0;
            todas.forEach(function (o) {
                if (o.dataset.especie !== especie) return;
                var coincide = !filtro || sinTildes(o.textContent).indexOf(filtro) !== -1;
                if (!coincide && o.value !== actual) return;
                var n = o.cloneNode(true);
                n.selected = false;
                if (n.value === actual) { n.selected = true; sigue = true; }
                selRaza.appendChild(n);
                visibles++;
            });
            if (contOtra && especie) {
                var otra = document.createElement('option');
                otra.value = OTRA;
                otra.textContent = 'Otra raza (escribirla)';
                if (actual === OTRA) { otra.selected = true; sigue = true; }
                selRaza.appendChild(otra);
            }
            if (!sigue) selRaza.value = '';
            selRaza.disabled = !especie;
            buscador.disabled = !especie;
            if (filtro && visibles === 0) ph.textContent = 'Sin coincidencias: elige «Otra raza»';
            mostrarOtra(selRaza.value === OTRA);
        }

        selEspecie.addEventListener('change', function () {
            otraElegida = false;
            buscador.value = '';
            pintar();
            if (selRaza._vlTocado && window.VlValidar) window.VlValidar.campo(selRaza);
        });
        buscador.addEventListener('input', function () {
            otraElegida = selRaza.value === OTRA;
            pintar();
        });
        buscador.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') { e.preventDefault(); selRaza.focus(); }
        });
        selRaza.addEventListener('change', function () {
            otraElegida = selRaza.value === OTRA;
            mostrarOtra(otraElegida);
            if (otraElegida && inputOtra) inputOtra.focus();
        });
        // «Otra raza» no es un id: no se envía el selector y el servidor usa razaPersonalizada.
        var form = selRaza.form;
        if (form) {
            form.addEventListener('submit', function (e) {
                if (e.defaultPrevented) return;
                if (selRaza.value === OTRA) {
                    selRaza.disabled = true;
                    setTimeout(function () { selRaza.disabled = false; }, 0);
                } else if (inputOtra) {
                    inputOtra.value = '';
                }
            });
        }
        pintar();
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-especie-de]').forEach(enlazar);
    });
})();
