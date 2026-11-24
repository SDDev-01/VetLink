/* VETLINK · Razas dependientes de la especie.
   - La raza inicia vacía («Seleccione una raza»), nunca en la primera opción.
   - Solo se listan las razas de la especie elegida.
   - Si cambia la especie y la raza ya no corresponde, se limpia.
   Uso: <select data-especie-de="idSelectRaza"> ... <select id="idSelectRaza"> con data-especie en cada opción. */
(function () {
    'use strict';
    function enlazar(selEspecie) {
        var selRaza = document.getElementById(selEspecie.dataset.especieDe);
        if (!selRaza) return;
        var todas = Array.prototype.slice.call(selRaza.querySelectorAll('option[data-especie]')).map(function (o) { return o.cloneNode(true); });
        var placeholder = selRaza.querySelector('option[value=""]');
        if (!placeholder) {
            placeholder = document.createElement('option');
            placeholder.value = '';
            placeholder.textContent = 'Seleccione una raza';
        }
        function pintar() {
            var especie = selEspecie.value;
            var actual = selRaza.value;
            selRaza.innerHTML = '';
            var ph = placeholder.cloneNode(true);
            ph.textContent = especie ? 'Seleccione una raza' : 'Primero seleccione la especie';
            selRaza.appendChild(ph);
            var sigue = false;
            todas.forEach(function (o) {
                if (o.dataset.especie === especie) {
                    var n = o.cloneNode(true);
                    n.selected = false;
                    if (n.value === actual) { n.selected = true; sigue = true; }
                    selRaza.appendChild(n);
                }
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
        document.querySelectorAll('select[data-especie-de]').forEach(enlazar);
    });
})();
