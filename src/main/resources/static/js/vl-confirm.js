/* =========================================================
   VETLINK · Confirmación de acciones sensibles (modal propio)
   Uso en un <form> o en el botón que lo envía:
     data-confirm="Explica las consecuencias"
     data-confirm-title="¿Transferir a Max?"
     data-confirm-ok="Sí, transferir"
     data-confirm-tipo="peligro"           (botón rojo)
     data-confirm-input="motivo"           (pide un texto y lo envía en ese campo)
     data-confirm-input-label="Motivo (opcional)"
     data-confirm-input-required           (el texto es obligatorio)
   No usa confirm() del navegador.
   ========================================================= */
(function () {
    'use strict';

    var overlay, titulo, texto, extra, campo, btnOk, btnCancel, pendiente = null, ultimoFoco = null;

    function construir() {
        if (overlay) return;
        overlay = document.createElement('div');
        overlay.className = 'vl-cmodal-overlay';
        overlay.hidden = true;
        overlay.innerHTML =
            '<div class="vl-cmodal" role="alertdialog" aria-modal="true" aria-labelledby="vl-cmodal-t" aria-describedby="vl-cmodal-d">' +
            '<h2 id="vl-cmodal-t"><i class="fas fa-circle-question" aria-hidden="true"></i><span></span></h2>' +
            '<p id="vl-cmodal-d"></p>' +
            '<div class="vl-cmodal-extra" hidden><label for="vl-cmodal-campo" class="vl-small vl-muted"></label>' +
            '<textarea id="vl-cmodal-campo" rows="3" maxlength="300"></textarea><span class="vl-field-error" aria-live="polite"></span></div>' +
            '<div class="vl-cmodal-actions"><button type="button" class="vl-btn vl-btn-ghost" data-accion="cancelar">Cancelar</button>' +
            '<button type="button" class="vl-btn vl-btn-primary" data-accion="ok">Confirmar</button></div></div>';
        document.body.appendChild(overlay);
        titulo = overlay.querySelector('h2 span');
        texto = overlay.querySelector('#vl-cmodal-d');
        extra = overlay.querySelector('.vl-cmodal-extra');
        campo = overlay.querySelector('#vl-cmodal-campo');
        btnOk = overlay.querySelector('[data-accion=ok]');
        btnCancel = overlay.querySelector('[data-accion=cancelar]');
        btnCancel.addEventListener('click', cerrar);
        btnOk.addEventListener('click', aceptar);
        overlay.addEventListener('click', function (e) { if (e.target === overlay) cerrar(); });
        document.addEventListener('keydown', function (e) {
            if (overlay.hidden) return;
            if (e.key === 'Escape') cerrar();
            if (e.key === 'Tab') {
                var focos = [campo, btnCancel, btnOk].filter(function (x) { return x.offsetParent !== null; });
                var i = focos.indexOf(document.activeElement);
                if (e.shiftKey && i <= 0) { e.preventDefault(); focos[focos.length - 1].focus(); }
                else if (!e.shiftKey && i === focos.length - 1) { e.preventDefault(); focos[0].focus(); }
            }
        });
    }

    function abrir(origen, form, submitter) {
        construir();
        var d = origen.dataset;
        pendiente = { form: form, submitter: submitter, input: d.confirmInput, requerido: d.confirmInputRequired !== undefined };
        titulo.textContent = d.confirmTitle || '¿Confirmas esta acción?';
        texto.textContent = d.confirm || '';
        btnOk.textContent = d.confirmOk || 'Confirmar';
        var peligro = d.confirmTipo === 'peligro';
        overlay.querySelector('.vl-cmodal').classList.toggle('peligro', peligro);
        btnOk.className = 'vl-btn ' + (peligro ? 'vl-btn-danger' : 'vl-btn-primary');
        extra.hidden = !d.confirmInput;
        campo.value = '';
        extra.querySelector('.vl-field-error').textContent = '';
        extra.querySelector('label').textContent = d.confirmInputLabel || 'Comentario (opcional)';
        ultimoFoco = document.activeElement;
        overlay.hidden = false;
        (d.confirmInput ? campo : btnCancel).focus();
    }

    function cerrar() {
        overlay.hidden = true;
        pendiente = null;
        if (ultimoFoco && ultimoFoco.focus) ultimoFoco.focus();
    }

    function aceptar() {
        if (!pendiente) return;
        var p = pendiente;
        if (p.input) {
            if (p.requerido && !campo.value.trim()) {
                extra.querySelector('.vl-field-error').textContent = 'Este campo es obligatorio';
                campo.focus();
                return;
            }
            var oculto = p.form.querySelector('input[type=hidden][name="' + p.input + '"]');
            if (!oculto) {
                oculto = document.createElement('input');
                oculto.type = 'hidden';
                oculto.name = p.input;
                p.form.appendChild(oculto);
            }
            oculto.value = campo.value.trim();
        }
        p.form._vlConfirmado = true;
        overlay.hidden = true;
        pendiente = null;
        if (p.form.requestSubmit) p.form.requestSubmit(p.submitter || undefined);
        else p.form.submit();
    }

    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (form._vlConfirmado) { form._vlConfirmado = false; return; }
        var sub = e.submitter;
        var origen = (sub && sub.hasAttribute('data-confirm')) ? sub : (form.hasAttribute('data-confirm') ? form : null);
        if (!origen) return;
        e.preventDefault();
        abrir(origen, form, sub);
    });

    document.addEventListener('click', function (e) {
        var a = e.target.closest && e.target.closest('a[data-confirm]');
        if (!a) return;
        e.preventDefault();
        var f = document.createElement('form');
        f.method = 'get';
        f.action = a.href;
        f.style.display = 'none';
        document.body.appendChild(f);
        abrir(a, f, null);
    });
})();
