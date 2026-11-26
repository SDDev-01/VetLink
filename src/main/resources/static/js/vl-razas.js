/* VETLINK · Razas dependientes de la especie (DUE-02).
   - La raza inicia vacía («Seleccione una raza»), nunca en la primera opción.
   - Solo se listan las razas de la especie elegida, en orden alfabético, con «Otra» al final
     (cada especie tiene una raza «Otra» en el catálogo: migración V12).
   - Si cambia la especie y la raza ya no corresponde, se limpia.
   Uso:
     <select data-especie-de="idSelectRaza"> (la especie)
     <select id="idSelectRaza"> con data-especie en cada opción */
(function () {
    'use strict';

    function esOtra(o) {
        return (o.textContent || '').trim().toLowerCase() === 'otra';
    }

    function enlazar(selEspecie) {
        var selRaza = document.getElementById(selEspecie.dataset.especieDe);
        if (!selRaza || selRaza._vlRazas) return;
        selRaza._vlRazas = true;
        var todas = Array.prototype.slice.call(selRaza.querySelectorAll('option[data-especie]')).map(function (o) { return o.cloneNode(true); });
        var collator = new Intl.Collator('es', { sensitivity: 'base' });
        todas.sort(function (a, b) {
            if (esOtra(a) !== esOtra(b)) return esOtra(a) ? 1 : -1;
            return collator.compare(a.textContent, b.textContent);
        });

        function pintar() {
            var especie = selEspecie.value;
            var actual = selRaza.value;
            selRaza.innerHTML = '';
            var ph = document.createElement('option');
            ph.value = '';
            ph.textContent = especie ? 'Seleccione una raza' : 'Primero seleccione la especie';
            selRaza.appendChild(ph);
            var sigue = false;
            todas.forEach(function (o) {
                if (o.dataset.especie !== especie) return;
                var n = o.cloneNode(true);
                n.selected = n.value === actual;
                if (n.selected) sigue = true;
                selRaza.appendChild(n);
            });
            if (!sigue) selRaza.value = '';
            selRaza.disabled = !especie;
        }

        selEspecie.addEventListener('change', function () {
            pintar();
            if (selRaza._vlTocado && window.VlValidar) window.VlValidar.campo(selRaza);
        });
        pintar();
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-especie-de]').forEach(enlazar);
    });
})();
