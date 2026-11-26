/* =========================================================
   VETLINK · Validación de formularios en tiempo real
   - Valida al salir de cada campo (blur), no al abrir el formulario.
   - Una vez tocado, el mensaje se actualiza mientras se corrige.
   - Borde de error + mensaje bajo el campo + aria-invalid/aria-describedby.
   - Impide enviar formularios inválidos. El servidor repite las validaciones críticas.
   Atributos opcionales en los campos:
     data-msg="..."            mensaje general para cualquier error
     data-msg-pattern="..."    mensaje para pattern
     data-max-mb="5"           tamaño máximo de archivo
     data-igual-a="idCampo"    debe coincidir con otro campo (confirmar contraseña)
     data-tipo="password"      exige 8+ caracteres con letras y números
     data-tipo="nombre"        nombres de personas o mascotas: letras, tildes, ñ, espacios y - ' . (sin números)
     data-tipo="telefono"      solo dígitos (7 a 15)
     data-no-pasado            fecha/fecha-hora no anterior a hoy/ahora
   Para excluir un formulario: data-sin-validacion
   Errores del servidor (th:errors con clase vl-error, vl-field-error o error-text junto al
   campo): se reutilizan como mensaje del campo y desaparecen en cuanto el valor cambia y
   pasa las validaciones del cliente. Un resumen con data-vl-resumen-errores se oculta
   cuando ya no queda ningún campo inválido.
   ========================================================= */
(function () {
    'use strict';

    var SELECTOR = 'input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=reset]), select, textarea';
    var contador = 0;

    function hoyISO() {
        var d = new Date();
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    }

    function ahoraISO() {
        var d = new Date();
        return hoyISO() + 'T' + String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
    }

    function contenedor(el) {
        return el.closest('.vl-field, .form-group, .input-group, .field, .vl-col') || el.parentElement;
    }

    /* Mensajes que pinta el servidor (Thymeleaf th:errors) junto al campo. */
    var SELECTOR_ERROR_SERVIDOR = '.vl-error, .vl-field-error, .error-text';

    /**
     * Busca el mensaje de error que el servidor dejó para este campo. Solo se adopta si
     * pertenece sin ambigüedad al campo: es un hermano posterior antes del siguiente
     * campo, o el contenedor tiene un único campo (o un único grupo de radios).
     */
    function errorDelServidor(el) {
        if (el.type === 'radio' || el.type === 'checkbox') return null;
        var sig = el.nextElementSibling;
        while (sig && !sig.matches(SELECTOR)) {
            if (sig.matches(SELECTOR_ERROR_SERVIDOR) && !sig._vlPropio) return sig;
            sig = sig.nextElementSibling;
        }
        var cont = contenedor(el);
        if (!cont || cont.tagName === 'FORM') return null;
        var campos = cont.querySelectorAll(SELECTOR);
        if (campos.length !== 1) return null;
        var err = cont.querySelector(SELECTOR_ERROR_SERVIDOR);
        return err && !err._vlPropio ? err : null;
    }

    /**
     * Si el servidor rechazó el campo, se adopta su mensaje como el del campo (así no se
     * acumulan dos mensajes) y se recuerda el valor rechazado: el aviso del servidor sigue
     * visible solo mientras el campo conserve ese valor.
     */
    function adoptarErrorServidor(el) {
        if (el._vlAdoptado) return;
        el._vlAdoptado = true;
        var err = errorDelServidor(el);
        if (!err) return;
        err._vlPropio = true;
        if (!err.id) err.id = 'vlerr-srv-' + (++contador);
        var desc = (el.getAttribute('aria-describedby') || '').split(' ').filter(Boolean);
        if (desc.indexOf(err.id) < 0) desc.push(err.id);
        el.setAttribute('aria-describedby', desc.join(' '));
        err.setAttribute('aria-live', 'polite');
        el._vlError = err;
        var texto = (err.textContent || '').trim();
        if (texto) {
            el._vlMsgServidor = texto;
            el._vlValorServidor = el.value;
            el._vlTocado = true;
            el.classList.add('vl-invalid');
            el.setAttribute('aria-invalid', 'true');
        }
    }

    /** Resúmenes de errores del formulario (data-vl-resumen-errores): visibles solo si queda algún campo inválido. */
    function actualizarResumen(form) {
        var resumenes = form.querySelectorAll('[data-vl-resumen-errores]');
        if (!resumenes.length) return;
        var hayErrores = !!form.querySelector('.vl-invalid');
        resumenes.forEach(function (r) { r.hidden = !hayErrores; });
    }

    function spanError(el) {
        if (el._vlError) return el._vlError;
        var span = document.createElement('span');
        span._vlPropio = true;
        span.className = 'vl-field-error';
        span.id = 'vlerr-' + (el.id || el.name || 'campo').replace(/[^\w-]/g, '') + '-' + (++contador);
        span.setAttribute('aria-live', 'polite');
        var cont = contenedor(el);
        if (el.type === 'radio' || el.type === 'checkbox') cont.appendChild(span);
        else el.insertAdjacentElement('afterend', span);
        var desc = (el.getAttribute('aria-describedby') || '').split(' ').filter(Boolean);
        desc.push(span.id);
        el.setAttribute('aria-describedby', desc.join(' '));
        el._vlError = span;
        return span;
    }

    function mensaje(el) {
        var v = el.validity;
        var valor = (el.value || '').trim();
        var tipo = (el.getAttribute('type') || el.tagName).toLowerCase();
        var nombre = (el.name || '').toLowerCase();

        if (v.valueMissing || (el.required && valor === '' && tipo !== 'file')) return 'Este campo es obligatorio';
        if (el.required && tipo === 'file' && (!el.files || el.files.length === 0)) return 'Este campo es obligatorio';
        if (valor === '' && tipo !== 'file') return '';

        if (tipo === 'file' && el.files && el.files.length) {
            var f = el.files[0];
            var accept = (el.getAttribute('accept') || '').toLowerCase().split(',').map(function (s) { return s.trim(); }).filter(Boolean);
            if (accept.length) {
                var ext = '.' + (f.name.split('.').pop() || '').toLowerCase();
                var okTipo = accept.some(function (a) {
                    if (a.startsWith('.')) return a === ext || (a === '.jpg' && ext === '.jpeg') || (a === '.jpeg' && ext === '.jpg');
                    if (a.endsWith('/*')) return (f.type || '').startsWith(a.slice(0, -1));
                    return a === (f.type || '').toLowerCase();
                });
                if (!okTipo) return 'El formato del archivo no está permitido';
            }
            var maxMb = parseFloat(el.dataset.maxMb || '0');
            if (maxMb > 0 && f.size > maxMb * 1024 * 1024) return 'El archivo supera el tamaño máximo de ' + maxMb + ' MB';
        }

        if (v.typeMismatch && tipo === 'email') return 'Ingresa un correo electrónico válido';
        if (tipo === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(valor)) return 'Ingresa un correo electrónico válido';
        if (tipo === 'tel' && !/^[+]?[\d\s()-]{7,15}$/.test(valor)) return 'Ingresa un número de teléfono válido';
        if (v.typeMismatch && tipo === 'url') return 'Ingresa una dirección web válida';

        if (el.hasAttribute('data-no-pasado')) {
            if (tipo === 'date' && valor < hoyISO()) return 'No puedes seleccionar una fecha anterior a la actual';
            if (tipo === 'datetime-local' && valor < ahoraISO()) return 'No puedes seleccionar una fecha anterior a la actual';
        }
        if (v.rangeUnderflow) {
            if (tipo === 'date' || tipo === 'datetime-local') return 'No puedes seleccionar una fecha anterior a la actual';
            if (/peso/.test(nombre)) return 'Ingresa un peso mayor que cero';
            return 'El valor mínimo permitido es ' + el.min;
        }
        if (v.rangeOverflow) {
            if (tipo === 'date') return 'La fecha no puede ser posterior a ' + el.max;
            return 'El valor máximo permitido es ' + el.max;
        }
        if (tipo === 'number' && /peso/.test(nombre) && !(parseFloat(valor) > 0)) return 'Ingresa un peso mayor que cero';
        if (v.badInput || v.stepMismatch) return 'Ingresa un valor válido';
        if (v.tooShort) return 'Debe tener al menos ' + el.minLength + ' caracteres';
        if (v.tooLong) return 'No puede superar ' + el.maxLength + ' caracteres';
        if (v.patternMismatch) return el.dataset.msgPattern || el.title || 'El formato no es válido';

        // Misma regla que com.vetlink.comun.validacion.Textos (el servidor la repite).
        if (el.dataset.tipo === 'nombre') {
            var limpio = valor.replace(/\s+/g, ' ');
            if (/\d/.test(limpio)) return 'El nombre no puede contener números';
            if (!/^[\p{L}\p{M}]+(?:[ '’.\-]{1,2}[\p{L}\p{M}]+)*\.?$/u.test(limpio)) {
                return 'Usa solo letras, espacios, guiones o apóstrofos';
            }
        }
        if (el.dataset.tipo === 'telefono' && !/^\d{7,15}$/.test(valor)) {
            return 'El teléfono solo puede tener números (entre 7 y 15 dígitos)';
        }
        if (el.dataset.tipo === 'password' && !(valor.length >= 8 && /[A-Za-z]/.test(valor) && /\d/.test(valor))) {
            return 'La contraseña debe tener mínimo 8 caracteres e incluir letras y números';
        }
        // data-posterior-a="idCampo": fechas/horas ISO; el valor debe ser posterior al del otro campo.
        if (el.dataset.posteriorA) {
            var previo = document.getElementById(el.dataset.posteriorA);
            if (previo && previo.value && valor <= previo.value) {
                return el.dataset.msgPosterior || 'Debe ser posterior a la fecha de inicio';
            }
        }
        if (el.dataset.igualA) {
            var otro = document.getElementById(el.dataset.igualA);
            if (otro && otro.value !== el.value) return el.dataset.msg || 'Los valores no coinciden';
        }
        if (el.validationMessage && !el.checkValidity()) return el.dataset.msg || el.validationMessage;
        return '';
    }

    function validar(el, forzar) {
        if (el.disabled || el.closest('[hidden]') || el.type === 'hidden') return true;
        if (!forzar && !el._vlTocado) return true;
        var m = mensaje(el);
        if (m && el.dataset.msg && !/obligatorio/.test(m)) m = el.dataset.msg;
        // Error que solo detecta el servidor (p. ej. documento ya registrado): sigue vigente
        // mientras el valor sea el mismo que se rechazó; al cambiarlo, desaparece.
        // No bloquea el envío: el servidor vuelve a decidir (puede depender de datos que cambiaron).
        var soloServidor = false;
        if (!m && el._vlMsgServidor) {
            if (el.value === el._vlValorServidor) { m = el._vlMsgServidor; soloServidor = true; }
            else el._vlMsgServidor = null;
        }
        var span = spanError(el);
        span.textContent = m;
        el.classList.toggle('vl-invalid', !!m);
        el.classList.toggle('vl-valid', !m && (el.value || '').trim() !== '');
        if (m) el.setAttribute('aria-invalid', 'true'); else el.removeAttribute('aria-invalid');
        if (el.form) actualizarResumen(el.form);
        return !m || soloServidor;
    }

    function enlazar(form) {
        if (form._vlValidado || form.hasAttribute('data-sin-validacion')) return;
        form._vlValidado = true;
        form.setAttribute('novalidate', 'novalidate');
        form.querySelectorAll(SELECTOR).forEach(adoptarErrorServidor);
        form.addEventListener('focusout', function (e) {
            var el = e.target;
            if (!el.matches || !el.matches(SELECTOR)) return;
            el._vlTocado = true;
            if (el.dataset.tipo === 'nombre' && typeof el.value === 'string') el.value = el.value.replace(/\s+/g, ' ').trim();
            validar(el);
            if (el.id) form.querySelectorAll('[data-igual-a="' + el.id + '"], [data-posterior-a="' + el.id + '"]').forEach(function (o) { if (o._vlTocado) validar(o); });
        });
        ['input', 'change'].forEach(function (ev) {
            form.addEventListener(ev, function (e) {
                var el = e.target;
                if (!el.matches || !el.matches(SELECTOR)) return;
                if (ev === 'change' && (el.type === 'file' || el.tagName === 'SELECT' || el.type === 'checkbox' || el.type === 'radio')) {
                    el._vlTocado = true;
                }
                if (el._vlTocado) validar(el);
            });
        });
        form.addEventListener('submit', function (e) {
            var ok = true;
            var primero = null;
            form.querySelectorAll(SELECTOR).forEach(function (el) {
                el._vlTocado = true;
                if (!validar(el, true)) {
                    ok = false;
                    if (!primero) primero = el;
                }
            });
            if (!ok) {
                e.preventDefault();
                e.stopImmediatePropagation();
                if (primero) primero.focus();
            }
        }, true);
    }

    function iniciar(raiz) {
        (raiz || document).querySelectorAll('form').forEach(enlazar);
    }

    window.VlValidar = { iniciar: iniciar, campo: function (el) { el._vlTocado = true; return validar(el, true); } };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', function () { iniciar(); });
    else iniciar();
})();
