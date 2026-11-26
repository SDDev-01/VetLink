/* =========================================================================
   registro.js - adaptado del prototipo original de VetLink.
   Se conserva casi toda la navegacion/validacion de pasos tal cual estaba;
   lo que cambia es: (1) los catalogos (especies, servicios, ciudades, dias)
   ya vienen renderizados por el servidor via Thymeleaf en vez de un objeto
   VL_CATALOGOS en memoria, y (2) el envio final es un submit real del
   formulario (POST /registro) en vez de escribir en localStorage.
========================================================================= */

let currentStep = 1;
let serviceIndex = 0; // lo recalcula reindexarServicios() al cargar

/* =========================
   SELECCIÓN DE ROL
========================= */
function seleccionarRol(rol) {
    document.getElementById('user-role').value = rol;

    const subtitle = document.getElementById('reg-subtitle');
    const title = document.getElementById('reg-title');
    const boxDireccion = document.getElementById('box-direccion');

    actualizarModoPassword(rol);
    if (rol === 'VETERINARIA') {
        subtitle.innerText = "Datos del Administrador de la Empresa";
        title.innerText = "Registro de Veterinaria";
        document.getElementById('btn-omitir-mascota').classList.add('hidden');
        if (boxDireccion) boxDireccion.classList.add('hidden');
    } else {
        subtitle.innerText = "Datos Personales del Dueño";
        title.innerText = "Registro de Dueño";
        document.getElementById('btn-omitir-mascota').classList.remove('hidden');
        if (boxDireccion) boxDireccion.classList.remove('hidden');
    }

    nextStep(2);
}

function updateTitles() {
    const role = document.getElementById('user-role').value;
    const title = document.getElementById('reg-title');
    const subtitle = document.getElementById('reg-subtitle');

    if (currentStep === 2) {
        subtitle.innerText = role === 'VETERINARIA' ? "Datos del Administrador" : "Datos personales";
    }
    if (currentStep === 3) {
        title.innerText = role === 'VETERINARIA' ? "Información de la Veterinaria" : "Información de la Mascota";
    }
    if (currentStep === 4 && role === 'VETERINARIA') {
        title.innerText = "Servicios y Horarios";
    }
    if (currentStep === 5 && role === 'VETERINARIA') {
        title.innerText = "Ubicación de la Clínica";
    }
}

/* =========================
   NAVEGACIÓN
========================= */
function nextStep(step) {
    document.querySelector(`#step-${currentStep}`).classList.add('hidden');
    document.querySelector(`#step-${step}`).classList.remove('hidden');
    currentStep = step;
    updateProgress(step);
    window.scrollTo(0, 0);
    updateTitles();
}

function prevStep(step) {
    document.querySelector(`#step-${currentStep}`).classList.add('hidden');
    document.querySelector(`#step-${step}`).classList.remove('hidden');
    currentStep = step;
    updateProgress(step);
    updateTitles();
}

/* =========================
   PROGRESO (5 PASOS PARA VETERINARIA, 3 PARA DUEÑO)
========================= */
function updateProgress(step) {
    const line = document.getElementById('progress-line');
    const role = document.getElementById('user-role').value;
    const totalSteps = role === 'VETERINARIA' ? 5 : 3;
    const percent = ((Math.min(step, totalSteps) - 1) / (totalSteps - 1)) * 100;
    line.style.width = percent + '%';

    document.querySelectorAll('.reg-step').forEach((dot, i) => {
        dot.classList.toggle('active', i < step);
    });
}

/* =========================
   ERRORES
========================= */
function cajaDe(input) {
    return input.closest('.input-box') || input.parentElement;
}

function showError(id, message) {
    const input = document.getElementById(id);
    if (!input) return;
    const caja = cajaDe(input);
    let errorText = caja.querySelector('.error-text');
    if (!errorText) {
        errorText = document.createElement('small');
        errorText.className = 'error-text';
        caja.appendChild(errorText);
    }
    input.style.borderColor = "var(--error)";
    input.classList.add('is-invalid');
    input.classList.remove('is-valid');
    input.setAttribute('aria-invalid', 'true');
    if (errorText) errorText.innerText = message;
}

function clearError(id) {
    const input = document.getElementById(id);
    if (!input) return;
    const errorText = cajaDe(input).querySelector('.error-text');
    input.style.borderColor = "#e2e8f0";
    input.classList.remove('is-invalid');
    input.removeAttribute('aria-invalid');
    if (errorText) errorText.innerText = "";
}

/* Campo correcto: borde verde (solo cuando tiene valor). */
function markValid(id) {
    const input = document.getElementById(id);
    if (!input) return;
    clearError(id);
    if (input.value.trim() !== "") {
        input.classList.add('is-valid');
        input.style.borderColor = "#22c55e";
    }
}

/* =========================
   VALIDACIONES REGEX
========================= */
const patterns = {
    name: /^[a-zA-ZÀ-ÿ\s]{3,40}$/,
    email: /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/,
    phone: /^3[0-9]{9}$/,
    doc: /^[0-9]{6,12}$/,
    nit: /^[0-9]{9,11}$/,
    pass: /^.{6,}$/
};

/* Cuentas de veterinaria: mínimo 8 caracteres, una mayúscula y un símbolo (igual que el servidor). */
function requisitosPassword(valor) {
    return {
        largo: valor.length >= 8,
        mayuscula: /\p{Lu}/u.test(valor),
        simbolo: /[^\p{L}\p{N}\s]/u.test(valor)
    };
}

function esVeterinaria() {
    return document.getElementById('user-role').value === 'VETERINARIA';
}

function actualizarModoPassword(rol) {
    const lista = document.getElementById('reqs-pass');
    const pass = document.getElementById('reg-pass');
    if (!lista || !pass) return;
    const vet = rol === 'VETERINARIA';
    lista.classList.toggle('hidden', !vet);
    pass.placeholder = vet ? 'Mínimo 8, con mayúscula y símbolo' : 'Mínimo 6 caracteres';
    pintarRequisitos();
}

function pintarRequisitos() {
    const lista = document.getElementById('reqs-pass');
    const pass = document.getElementById('reg-pass');
    if (!lista || !pass) return;
    const r = requisitosPassword(pass.value);
    lista.querySelectorAll('li').forEach(li => {
        const ok = !!r[li.dataset.req];
        li.classList.toggle('ok', ok);
        li.querySelector('i').className = ok ? 'fas fa-circle-check' : 'fas fa-circle';
    });
}

/* Valida un campo en vivo y devuelve el mensaje de error ('' si es correcto). */
function errorDeCampo(id) {
    const el = document.getElementById(id);
    if (!el) return '';
    const v = el.value.trim();
    const vet = esVeterinaria();
    switch (id) {
        case 'reg-num-doc': return !v ? 'El número de documento es obligatorio' : (!patterns.doc.test(v) ? 'Usa entre 6 y 12 dígitos, sin espacios ni letras' : '');
        case 'reg-nombre': return !v ? 'El nombre es obligatorio' : (!patterns.name.test(v) ? 'Usa entre 3 y 40 letras y espacios' : '');
        case 'reg-apellido': return !v ? 'El apellido es obligatorio' : (!patterns.name.test(v) ? 'Usa entre 3 y 40 letras y espacios' : '');
        case 'reg-correo': return !v ? 'El correo es obligatorio' : (!patterns.email.test(v) ? 'Escribe un correo válido, por ejemplo nombre@dominio.com' : '');
        case 'reg-tel': return !v ? 'El teléfono es obligatorio' : (!patterns.phone.test(v) ? 'Debe tener 10 dígitos y comenzar por 3, sin espacios ni guiones' : '');
        case 'reg-pass': {
            const raw = el.value;
            if (!raw) return 'La contraseña es obligatoria';
            if (vet) {
                const r = requisitosPassword(raw);
                if (!r.largo || !r.mayuscula || !r.simbolo) return 'La contraseña no cumple todos los requisitos';
                return '';
            }
            return raw.length < 6 ? 'La contraseña debe tener al menos 6 caracteres' : '';
        }
        case 'reg-confirmar-pass': {
            const pass = document.getElementById('reg-pass').value;
            return !el.value ? 'Confirma tu contraseña' : (el.value !== pass ? 'Las contraseñas no coinciden' : '');
        }
        case 'reg-direccion': return vet ? '' : (!v ? 'La dirección es obligatoria' : (v.length < 5 ? 'La dirección debe tener al menos 5 caracteres' : ''));
        case 'vet-nit': return !v ? 'El NIT es obligatorio' : (!patterns.nit.test(v) ? 'El NIT debe tener entre 9 y 11 dígitos' : '');
        case 'vet-nombre': return !v ? 'El nombre comercial es obligatorio' : (v.length < 3 ? 'El nombre debe tener al menos 3 caracteres' : (v.length > 50 ? 'El nombre puede tener máximo 50 caracteres' : ''));
        case 'vet-tel': return !v ? 'El teléfono corporativo es obligatorio' : (!/^\d{7,15}$/.test(v) ? 'Usa entre 7 y 15 dígitos, sin espacios ni guiones' : '');
        case 'vet-correo': return !v ? 'El correo de la empresa es obligatorio' : (!patterns.email.test(v) ? 'Escribe un correo válido, por ejemplo clinica@dominio.com' : (v.length > 45 ? 'Máximo 45 caracteres' : ''));
        case 'vet-web': return v && !/^https?:\/\/[^\s.]+\.[^\s]{2,}$/i.test(v) ? 'Escribe la dirección completa, por ejemplo https://www.tuweb.com' : (v.length > 50 ? 'Máximo 50 caracteres' : '');
        case 'vet-direccion': return v.length < 5 ? 'Ingresa una dirección válida (mínimo 5 caracteres)' : '';
        default: return '';
    }
}

/* Detección de registros duplicados mientras se escribe (el servidor vuelve a validar al enviar). */
const CAMPOS_DUPLICADOS = {
    'reg-correo': 'correo', 'reg-num-doc': 'documento', 'reg-tel': 'telefono',
    'vet-nit': 'nit', 'vet-nombre': 'nombreVeterinaria', 'vet-correo': 'correoVeterinaria'
};
const duplicados = {};      // id -> mensaje del servidor ('' si está libre)
const temporizadores = {};

function revisarDuplicado(id) {
    const campo = CAMPOS_DUPLICADOS[id];
    const el = document.getElementById(id);
    if (!campo || !el) return;
    const valor = el.value.trim();
    clearTimeout(temporizadores[id]);
    if (!valor || errorDeCampo(id)) { delete duplicados[id]; return; }
    temporizadores[id] = setTimeout(() => {
        fetch('/api/registro/disponible?campo=' + encodeURIComponent(campo) + '&valor=' + encodeURIComponent(valor))
            .then(r => r.ok ? r.json() : null)
            .then(res => {
                if (!res || el.value.trim() !== valor) return; // respuesta vieja
                duplicados[id] = res.disponible ? '' : res.mensaje;
                if (res.disponible) markValid(id); else showError(id, res.mensaje);
            })
            .catch(() => { /* sin conexión: lo valida el servidor al enviar */ });
    }, 450);
}

function validarCampoEnVivo(id) {
    const msg = errorDeCampo(id) || duplicados[id] || '';
    if (msg) showError(id, msg); else markValid(id);
    return !msg;
}

/* =========================
   VALIDAR PASO 2 (datos personales)
========================= */
function validateStep2() {
    const role = document.getElementById('user-role').value;
    const ids = ['reg-num-doc', 'reg-nombre', 'reg-apellido', 'reg-correo', 'reg-tel', 'reg-pass', 'reg-confirmar-pass'];
    if (role !== 'VETERINARIA') ids.push('reg-direccion');
    let valid = true;
    ids.forEach(id => { if (!validarCampoEnVivo(id)) valid = false; });
    pintarRequisitos();

    if (valid) {
        if (role === 'VETERINARIA') {
            document.getElementById('section-vet').classList.remove('hidden');
            document.getElementById('section-pet').classList.add('hidden');
        } else {
            document.getElementById('section-pet').classList.remove('hidden');
            document.getElementById('section-vet').classList.add('hidden');
        }
        nextStep(3);
    } else {
        const primero = document.querySelector('#step-2 .is-invalid');
        if (primero) primero.focus();
    }
}

/* Carga las razas de la especie elegida via el catalogo real (AJAX) */
function cargarRazas() {
    const especieSel = document.getElementById('pet-especie');
    const razaSel = document.getElementById('pet-raza');
    const idEspecie = especieSel.value;

    if (!idEspecie) {
        razaSel.innerHTML = '<option value="">Selecciona especie primero</option>';
        return;
    }
    razaSel.innerHTML = '<option value="">Cargando...</option>';
    fetch(`/api/catalogos/razas/${idEspecie}`)
        .then(r => r.json())
        .then(razas => {
            razaSel.innerHTML = razas.map(r => `<option value="${r.id}">${r.nombre}</option>`).join('');
        })
        .catch(() => { razaSel.innerHTML = '<option value="">No se pudieron cargar las razas</option>'; });
}

/* =========================
   VALIDAR PASO 3
========================= */
function validateStep3() {
    const role = document.getElementById('user-role').value;

    if (role === 'VETERINARIA') {
        let valid = true;
        ['vet-nit', 'vet-nombre', 'vet-tel', 'vet-correo', 'vet-web'].forEach(id => { if (!validarCampoEnVivo(id)) valid = false; });

        if (valid) nextStep(4);
        else { const primero = document.querySelector('#section-vet .is-invalid'); if (primero) primero.focus(); }
    } else {
        let valid = true;
        const pNombre = document.getElementById('pet-nombre');
        const pFecha = document.getElementById('pet-fecha');
        const pPeso = document.getElementById('pet-peso');

        // Si el usuario no escribio nada, se interpreta como "omitir mascota".
        if (!pNombre.value.trim()) {
            omitirMascota();
            return;
        }

        if (pNombre.value.trim().length < 2) { showError('pet-nombre', "Nombre requerido"); valid = false; }
        else if (!/^[\p{L}\p{M}]+(?:[ '’.\-]{1,2}[\p{L}\p{M}]+)*\.?$/u.test(pNombre.value.trim().replace(/\s+/g, ' '))) { showError('pet-nombre', "El nombre no puede tener números ni símbolos"); valid = false; }
        else clearError('pet-nombre');
        if (!pFecha.value) { showError('pet-fecha', "Selecciona fecha"); valid = false; }
        else if (new Date(pFecha.value) > new Date()) { showError('pet-fecha', "Fecha inválida"); valid = false; }
        else clearError('pet-fecha');
        if (!pPeso.value || Number(pPeso.value) <= 0) { showError('pet-peso', "Peso inválido"); valid = false; } else clearError('pet-peso');

        if (valid) document.getElementById('regForm').submit();
    }
}

/* Omitir registro de mascota (solo visible para rol DUEÑO) */
function omitirMascota() {
    document.getElementById('pet-nombre').value = "";
    document.getElementById('regForm').submit();
}

/* =========================
   PASO 4: SERVICIOS Y HORARIOS (solo veterinaria)
========================= */
const OPCION_OTROS = '__otros__';

function sinTildes(t) {
    return (t || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

function catalogo() {
    return (typeof VL_CATALOGO_SERVICIOS !== 'undefined' && VL_CATALOGO_SERVICIOS) ? VL_CATALOGO_SERVICIOS : [];
}

/* Claves de los servicios ya elegidos (por id y por nombre sin tildes). */
function elegidos() {
    const ids = new Set(), nombres = new Set();
    document.querySelectorAll('#services-container .svc-fila').forEach(f => {
        const id = f.querySelector('.svc-id').value;
        if (id) {
            ids.add(String(id));
            const c = catalogo().find(x => String(x.id) === String(id));
            if (c) nombres.add(sinTildes(c.nombre));
        } else {
            nombres.add(sinTildes(f.querySelector('.svc-nombre').value));
        }
    });
    return { ids, nombres };
}

function escaparHtml(t) {
    return String(t == null ? '' : t).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function pintarLista(abrir) {
    const input = document.getElementById('svc-buscar');
    const lista = document.getElementById('svc-lista');
    if (!input || !lista) return;
    const q = sinTildes(input.value);
    const ya = elegidos();
    const opciones = catalogo()
        .filter(c => !ya.ids.has(String(c.id)) && !ya.nombres.has(sinTildes(c.nombre)))
        .filter(c => !q || sinTildes(c.nombre).includes(q));
    let html = opciones.map((c, i) => '<li role="option" id="svc-op-' + c.id + '" data-id="' + c.id + '" aria-selected="' + (i === 0 && q ? 'true' : 'false') + '">'
        + '<i class="fas fa-briefcase-medical" aria-hidden="true"></i>' + escaparHtml(c.nombre) + '</li>').join('');
    if (!opciones.length) html = '<li class="vacio" aria-disabled="true">Sin coincidencias en el catálogo</li>';
    html += '<li role="option" class="otros" data-id="' + OPCION_OTROS + '" aria-selected="false"><i class="fas fa-pen" aria-hidden="true"></i> Otros (escribir un servicio)</li>';
    lista.innerHTML = html;
    if (abrir) { lista.hidden = false; input.setAttribute('aria-expanded', 'true'); }
}

function cerrarLista() {
    const lista = document.getElementById('svc-lista');
    const input = document.getElementById('svc-buscar');
    if (lista) lista.hidden = true;
    if (input) input.setAttribute('aria-expanded', 'false');
}

function agregarFilaServicio(id, nombreVisible, nombrePersonalizado) {
    const cont = document.getElementById('services-container');
    const fila = document.createElement('div');
    fila.className = 'service-item svc-fila';
    fila.innerHTML =
        '<input type="hidden" class="svc-id">' +
        '<input type="hidden" class="svc-nombre">' +
        '<span class="nombre"></span>' +
        '<input type="number" class="service-precio" placeholder="Precio (COP)" min="0" step="100" aria-label="Precio">' +
        '<button type="button" class="btn-remove" aria-label="Quitar servicio"><i class="fas fa-trash"></i></button>';
    fila.querySelector('.svc-id').value = id || '';
    fila.querySelector('.svc-nombre').value = nombrePersonalizado || '';
    cont.appendChild(fila);
    pintarNombreFila(fila, nombreVisible, !id);
    reindexarServicios();
    fila.querySelector('.service-precio').focus();
}

function pintarNombreFila(fila, nombre, personalizado) {
    const span = fila.querySelector('.nombre');
    span.textContent = nombre;
    if (personalizado) {
        const tag = document.createElement('span');
        tag.className = 'tag';
        tag.textContent = 'Nuevo';
        span.appendChild(tag);
    }
}

function elegirServicio(id) {
    if (id === OPCION_OTROS) {
        document.getElementById('svc-otro').classList.remove('hidden');
        const n = document.getElementById('svc-otro-nombre');
        n.value = document.getElementById('svc-buscar').value.trim();
        cerrarLista();
        n.focus();
        return;
    }
    const c = catalogo().find(x => String(x.id) === String(id));
    if (!c) return;
    agregarFilaServicio(c.id, c.nombre, '');
    document.getElementById('svc-buscar').value = '';
    cerrarLista();
    document.getElementById('err-services').innerText = '';
}

function agregarOtro() {
    const input = document.getElementById('svc-otro-nombre');
    const err = document.getElementById('err-svc-otro');
    const nombre = input.value.replace(/\s+/g, ' ').trim();
    if (!nombre) { err.innerText = 'Escribe el nombre del servicio'; input.focus(); return; }
    if (nombre.length < 3 || nombre.length > 45 || !/\p{L}/u.test(nombre)) { err.innerText = 'El nombre debe tener entre 3 y 45 caracteres e incluir letras'; input.focus(); return; }
    const clave = sinTildes(nombre);
    const ya = elegidos();
    if (ya.nombres.has(clave)) { err.innerText = 'Ese servicio ya está en tu lista'; input.focus(); return; }
    const delCatalogo = catalogo().find(c => sinTildes(c.nombre) === clave);
    if (delCatalogo) {
        // Ya existe en el catálogo: se usa ese servicio en vez de crear un duplicado.
        agregarFilaServicio(delCatalogo.id, delCatalogo.nombre, '');
    } else {
        agregarFilaServicio('', nombre, nombre);
    }
    err.innerText = '';
    input.value = '';
    document.getElementById('svc-otro').classList.add('hidden');
    document.getElementById('err-services').innerText = '';
}

function addService() {
    // Compatibilidad: el botón antiguo abre el buscador.
    const input = document.getElementById('svc-buscar');
    if (input) { input.focus(); pintarLista(true); }
}

function quitarServicio(btn) {
    btn.closest('.service-item').remove();
    reindexarServicios();
    pintarLista(false);
}

function reindexarServicios() {
    const items = document.querySelectorAll('#services-container .service-item');
    items.forEach((item, i) => {
        item.querySelector('.svc-id').name = `nuevaVeterinaria.servicios[${i}].idServicio`;
        item.querySelector('.svc-nombre').name = `nuevaVeterinaria.servicios[${i}].nombrePersonalizado`;
        item.querySelector('.service-precio').name = `nuevaVeterinaria.servicios[${i}].precio`;
    });
    serviceIndex = items.length;
    const vacio = document.getElementById('svc-vacio');
    if (vacio) vacio.classList.toggle('hidden', items.length > 0);
}

function initServicios() {
    const input = document.getElementById('svc-buscar');
    const lista = document.getElementById('svc-lista');
    const cont = document.getElementById('services-container');
    if (!input || !lista || !cont) return;
    // Filas que vuelven del servidor: se muestra su nombre.
    cont.querySelectorAll('.svc-fila').forEach(f => {
        const id = f.querySelector('.svc-id').value;
        const c = catalogo().find(x => String(x.id) === String(id));
        pintarNombreFila(f, c ? c.nombre : f.querySelector('.svc-nombre').value, !c);
    });
    reindexarServicios();
    input.addEventListener('focus', () => pintarLista(true));
    input.addEventListener('input', () => pintarLista(true));
    input.addEventListener('keydown', e => {
        const items = [...lista.querySelectorAll('li[role="option"]')];
        let i = items.findIndex(li => li.getAttribute('aria-selected') === 'true');
        if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
            e.preventDefault();
            if (lista.hidden) pintarLista(true);
            i = e.key === 'ArrowDown' ? Math.min(items.length - 1, i + 1) : Math.max(0, i - 1);
            items.forEach((li, k) => li.setAttribute('aria-selected', k === i ? 'true' : 'false'));
            if (items[i]) { items[i].scrollIntoView({ block: 'nearest' }); input.setAttribute('aria-activedescendant', items[i].id || ''); }
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (items[i]) elegirServicio(items[i].dataset.id);
        } else if (e.key === 'Escape') {
            cerrarLista();
        }
    });
    lista.addEventListener('mousedown', e => {
        const li = e.target.closest('li[role="option"]');
        if (li) { e.preventDefault(); elegirServicio(li.dataset.id); }
    });
    document.addEventListener('click', e => { if (!e.target.closest('#svc-combo')) cerrarLista(); });
    cont.addEventListener('click', e => {
        const btn = e.target.closest('.btn-remove');
        if (btn) quitarServicio(btn);
    });
    document.getElementById('svc-otro-agregar').addEventListener('click', agregarOtro);
    document.getElementById('svc-otro-nombre').addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); agregarOtro(); } });
}

function validateStep4() {
    const services = document.querySelectorAll('#services-container .service-item');
    const errEl = document.getElementById('err-services');

    if (services.length === 0) {
        errEl.innerText = "Agrega al menos un servicio";
        document.getElementById('svc-buscar').focus();
        return;
    }
    const sinPrecio = [...services].find(item => {
        const p = item.querySelector('.service-precio');
        return p.value === '' || Number(p.value) < 0;
    });
    if (sinPrecio) {
        errEl.innerText = "Indica el precio de cada servicio (0 o mayor)";
        sinPrecio.querySelector('.service-precio').focus();
        return;
    }
    errEl.innerText = "";

    // VL-14: cada rango abierto debe tener apertura < cierre (el servidor también lo valida).
    const errHorarios = document.getElementById('err-horarios');
    const errorHorario = validarRangosHorario();
    if (errorHorario) {
        if (errHorarios) errHorarios.innerText = errorHorario;
        return;
    }
    if (errHorarios) errHorarios.innerText = "";

    initMap();
    nextStep(5);
}

/* =========================
   VL-14: varios rangos por día
   Cada rango extra se envía como otra entrada de nuevaVeterinaria.horarios[N]
   con el mismo idDiaSemana. Los índices deben ser consecutivos (un hueco haría que
   Spring cree un horario vacío que no pasa la validación), por eso al quitar un
   rango se renumeran todos los rangos extra.
========================= */
function indiceBaseHorarios() {
    return document.querySelectorAll('#days-grid .day-card > input[name$=".idDiaSemana"]').length;
}

function renumerarRangosExtra() {
    let i = indiceBaseHorarios();
    document.querySelectorAll('#days-grid .extra-range').forEach(function (fila) {
        fila.querySelectorAll('input[name^="nuevaVeterinaria.horarios["]').forEach(function (input) {
            input.name = input.name.replace(/horarios\[\d+\]/, 'horarios[' + i + ']');
        });
        i++;
    });
}

function removeRange(btn) {
    btn.closest('.extra-range').remove();
    renumerarRangosExtra();
}

function addRange(btn) {
    const card = btn.closest('.day-card');
    const cont = card.querySelector('.extra-ranges');
    const base = 'nuevaVeterinaria.horarios[0]'; // el índice real lo pone renumerarRangosExtra()
    const fila = document.createElement('div');
    fila.className = 'hours extra-range';
    fila.innerHTML =
        '<input type="hidden" name="' + base + '.idDiaSemana" value="' + cont.dataset.idDia + '">' +
        '<input type="hidden" name="' + base + '.abierto" value="true">' +
        '<div class="time-group"><small>Apertura</small><input type="time" name="' + base + '.horaApertura" value="14:00" class="open"></div>' +
        '<div class="time-group"><small>Cierre</small><input type="time" name="' + base + '.horaCierre" value="18:00" class="close"></div>' +
        '<button type="button" class="btn-remove-range" title="Quitar este horario" onclick="removeRange(this)">' +
        '<i class="fas fa-trash"></i></button>';
    cont.appendChild(fila);
    renumerarRangosExtra();
}

function validarRangosHorario() {
    const cards = document.querySelectorAll('#days-grid .day-card');
    for (const card of cards) {
        const check = card.querySelector('.day-toggle input[type="checkbox"]');
        if (!check || !check.checked) continue;
        const dia = card.dataset.dia;
        const rangos = [];
        card.querySelectorAll('.hours').forEach(function (fila) {
            const open = fila.querySelector('input.open');
            const close = fila.querySelector('input.close');
            if (open && close) rangos.push([open.value, close.value]);
        });
        for (const [a, c] of rangos) {
            if (!a || !c || a >= c) {
                return 'El ' + dia + ', la hora de apertura debe ser anterior a la de cierre.';
            }
        }
        const ordenados = rangos.slice().sort((x, y) => x[0].localeCompare(y[0]));
        for (let k = 1; k < ordenados.length; k++) {
            if (ordenados[k][0] < ordenados[k - 1][1]) {
                return 'El ' + dia + ' tiene horarios que se cruzan.';
            }
        }
    }
    return null;
}

/* =========================
   PASO 5: UBICACIÓN (MAPA)
========================= */
let map;
let marker;

function initMap() {
    if (map) { setTimeout(() => map.invalidateSize(), 200); return; }

    map = L.map('map').setView([4.6482, -74.0648], 12); // Bogotá por defecto

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap'
    }).addTo(map);

    map.on('click', function (e) {
        const { lat, lng } = e.latlng;
        if (marker) marker.setLatLng([lat, lng]);
        else marker = L.marker([lat, lng]).addTo(map);
        document.getElementById('vet-lat').value = lat;
        document.getElementById('vet-lng').value = lng;
    });

    setTimeout(() => map.invalidateSize(), 200);
}

function validateStep5() {
    if (!validarCampoEnVivo('vet-direccion')) return;
    // Revisión final de todos los pasos: si algo cambió o es un duplicado, se vuelve a ese paso.
    const paso2 = ['reg-num-doc', 'reg-nombre', 'reg-apellido', 'reg-correo', 'reg-tel', 'reg-pass', 'reg-confirmar-pass'];
    const paso3 = ['vet-nit', 'vet-nombre', 'vet-tel', 'vet-correo', 'vet-web'];
    if (!paso2.every(id => validarCampoEnVivo(id))) { prevStep(2); return; }
    if (!paso3.every(id => validarCampoEnVivo(id))) { prevStep(3); return; }
    if (!document.querySelectorAll('#services-container .service-item').length) { prevStep(4); validateStep4(); return; }
    document.getElementById('regForm').submit();
}

/* =========================
   LIMPIAR ERRORES EN TIEMPO REAL + RESTAURAR PASO SI HUBO ERRORES DEL SERVIDOR
========================= */
document.addEventListener('DOMContentLoaded', () => {
    const enVivo = ['reg-num-doc', 'reg-nombre', 'reg-apellido', 'reg-correo', 'reg-tel', 'reg-pass', 'reg-confirmar-pass',
        'reg-direccion', 'vet-nit', 'vet-nombre', 'vet-tel', 'vet-correo', 'vet-web', 'vet-direccion'];
    enVivo.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        let tocado = false;
        el.addEventListener('blur', () => { tocado = true; validarCampoEnVivo(id); revisarDuplicado(id); });
        el.addEventListener('input', () => {
            if (id === 'reg-pass') {
                pintarRequisitos();
                const conf = document.getElementById('reg-confirmar-pass');
                if (conf && conf.value) validarCampoEnVivo('reg-confirmar-pass');
            }
            delete duplicados[id];
            if (tocado || el.classList.contains('is-invalid') || id === 'reg-pass' || id === 'reg-confirmar-pass') validarCampoEnVivo(id);
            revisarDuplicado(id);
        });
    });
    document.querySelectorAll('input:not([id^="reg-"]):not([id^="vet-"]), select, textarea').forEach(input => {
        input.addEventListener('input', () => {
            if (input.value.trim() !== "") clearError(input.id);
        });
    });
    document.querySelectorAll('[data-toggle-pass]').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.togglePass);
            const ver = input.type === 'password';
            input.type = ver ? 'text' : 'password';
            btn.setAttribute('aria-pressed', ver ? 'true' : 'false');
            btn.setAttribute('aria-label', ver ? 'Ocultar contraseña' : 'Mostrar contraseña');
            btn.querySelector('i').className = ver ? 'fas fa-eye-slash' : 'fas fa-eye';
        });
    });
    initServicios();

    const direccionInput = document.getElementById('vet-direccion');
    if (direccionInput) {
        direccionInput.addEventListener('change', function () {
            const direccion = this.value;
            if (!direccion || !map) return;
            fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(direccion)}`)
                .then(res => res.json())
                .then(data => {
                    if (data.length > 0) {
                        const lat = data[0].lat, lon = data[0].lon;
                        map.setView([lat, lon], 15);
                        if (marker) marker.setLatLng([lat, lon]);
                        else marker = L.marker([lat, lon]).addTo(map);
                        document.getElementById('vet-lat').value = lat;
                        document.getElementById('vet-lng').value = lon;
                    }
                })
                .catch(() => {});
        });
    }

    // Si el servidor devolvio errores de validacion, volvemos a mostrar el
    // paso correspondiente segun el rol que se habia elegido, en vez de
    // reiniciar todo el wizard desde el paso 1.
    if (typeof VL_FORM_TIENE_ERRORES !== 'undefined' && VL_FORM_TIENE_ERRORES && VL_ROL_ENVIADO) {
        seleccionarRolSilencioso(VL_ROL_ENVIADO);
        if (VL_ROL_ENVIADO === 'VETERINARIA') {
            document.getElementById('section-vet').classList.remove('hidden');
            nextStep(3);
        } else {
            document.getElementById('section-pet').classList.remove('hidden');
            nextStep(3);
        }
    }
});

/* Igual que seleccionarRol() pero sin saltar de paso (usado al recuperar errores del servidor) */
function seleccionarRolSilencioso(rol) {
    document.getElementById('user-role').value = rol;
    actualizarModoPassword(rol);
    const boxDireccion = document.getElementById('box-direccion');
    if (rol === 'VETERINARIA') {
        document.getElementById('btn-omitir-mascota').classList.add('hidden');
        if (boxDireccion) boxDireccion.classList.add('hidden');
    } else {
        document.getElementById('btn-omitir-mascota').classList.remove('hidden');
        if (boxDireccion) boxDireccion.classList.remove('hidden');
    }
    updateTitles();
}

function toggleDay(checkbox) {
    const card = checkbox.closest('.day-card');
    const hours = card.querySelector('.hours');
    const status = card.querySelector('.day-status');

    const extra = card.querySelector('.extra-ranges');
    const addBtn = card.querySelector('.btn-add-range');

    if (checkbox.checked) {
        if (extra) extra.classList.remove('hidden');
        if (addBtn) addBtn.classList.remove('hidden');
        hours.classList.remove('hidden');
        status.innerText = "Abierto";
        status.style.color = "#22c55e";
        card.style.background = "#f0f7ff";
        card.style.borderColor = "var(--primary)";
    } else {
        // Al cerrar el día se quitan sus rangos adicionales.
        if (extra) { extra.innerHTML = ''; extra.classList.add('hidden'); renumerarRangosExtra(); }
        if (addBtn) addBtn.classList.add('hidden');
        hours.classList.add('hidden');
        status.innerText = "Cerrado";
        status.style.color = "#ef4444";
        card.style.background = "#f8fafc";
        card.style.borderColor = "transparent";
    }
}
