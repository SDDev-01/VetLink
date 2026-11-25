/* =========================================================
   VETLINK · Caja de escritura de chat (GEN-01)
   Se aplica a todo <form data-vl-chat> que tenga un <textarea>.
   - Enter envía; Shift+Enter inserta un salto de línea.
   - No envía mientras se compone texto con un IME (acentos, teclados asiáticos).
   - No envía mensajes vacíos o solo con espacios (aviso discreto, sin alert()).
   - Evita el doble envío por pulsaciones repetidas.
   - Si el envío falla, el texto escrito se conserva y se muestra el motivo.
   - La altura del campo crece con el contenido hasta un máximo.
   El botón «Enviar» sigue disponible (móviles). El servidor repite las validaciones.
   ========================================================= */
(function () {
    'use strict';

    var ALTURA_MAX = 160;

    function enlazar(form) {
        if (form._vlChat) return;
        form._vlChat = true;
        var campo = form.querySelector('textarea');
        var boton = form.querySelector('button[type=submit], button:not([type])');
        if (!campo) return;
        var maximo = parseInt(campo.getAttribute('maxlength') || '2000', 10);
        var ayuda = form.parentElement.querySelector('[data-vl-chat-ayuda]');
        var aviso = ayuda ? ayuda.querySelector('.vl-chat-error') : null;
        var contador = ayuda ? ayuda.querySelector('.vl-chat-contador') : null;
        var enviando = false;
        var hilo = document.getElementById(form.dataset.vlChatHilo || 'hilo');

        function avisar(texto) {
            if (aviso) aviso.textContent = texto || '';
            if (texto) campo.setAttribute('aria-invalid', 'true'); else campo.removeAttribute('aria-invalid');
        }

        function ajustarAltura() {
            campo.style.height = 'auto';
            campo.style.height = Math.min(campo.scrollHeight, ALTURA_MAX) + 'px';
        }

        function actualizarContador() {
            if (contador) contador.textContent = campo.value.length + ' / ' + maximo;
        }

        function bloquear(si) {
            enviando = si;
            if (boton) {
                boton.disabled = si;
                boton.setAttribute('aria-busy', si ? 'true' : 'false');
            }
        }

        function enviar() {
            if (enviando) return;
            var texto = campo.value;
            if (!texto.trim()) {
                avisar('Escribe un mensaje antes de enviarlo.');
                campo.focus();
                return;
            }
            if (texto.length > maximo) {
                avisar('El mensaje no puede superar ' + maximo + ' caracteres.');
                return;
            }
            avisar('');
            bloquear(true);
            fetch(form.action, {
                method: 'POST',
                body: new FormData(form),
                credentials: 'same-origin',
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            }).then(function (resp) {
                // Tras guardar, el servidor redirige al hilo; si redirige al login, la sesión expiró.
                if (!resp.ok || /\/login/.test(resp.url)) {
                    throw new Error(/\/login/.test(resp.url) ? 'Tu sesión expiró. Inicia sesión de nuevo; tu mensaje sigue aquí.'
                        : 'No se pudo enviar el mensaje. Inténtalo de nuevo; tu mensaje sigue aquí.');
                }
                campo.value = '';
                window.location.replace(resp.url || window.location.href);
            }).catch(function (err) {
                bloquear(false);
                avisar(err && err.message && !/fetch|network/i.test(err.message) ? err.message
                    : 'Sin conexión: no se pudo enviar. Tu mensaje sigue aquí.');
                campo.focus();
            });
        }

        campo.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter' || e.shiftKey || e.isComposing || e.keyCode === 229) return;
            e.preventDefault();
            enviar();
        });
        campo.addEventListener('input', function () {
            ajustarAltura();
            actualizarContador();
            if (campo.value.trim()) avisar('');
        });
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();
            enviar();
        }, true);

        ajustarAltura();
        actualizarContador();
        if (hilo) hilo.scrollTop = hilo.scrollHeight;
    }

    function iniciar() {
        document.querySelectorAll('form[data-vl-chat]').forEach(enlazar);
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciar);
    else iniciar();
})();
