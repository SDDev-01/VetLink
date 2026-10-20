/* =========================================================================
   registro.js - adaptado del prototipo original de VetLink.
   Se conserva casi toda la navegacion/validacion de pasos tal cual estaba;
   lo que cambia es: (1) los catalogos (especies, servicios, ciudades, dias)
   ya vienen renderizados por el servidor via Thymeleaf en vez de un objeto
   VL_CATALOGOS en memoria, y (2) el envio final es un submit real del
   formulario (POST /registro) en vez de escribir en localStorage.
========================================================================= */

let currentStep = 1;
let serviceIndex = document.querySelectorAll('#services-container .service-item').length;

/* =========================
   SELECCIÓN DE ROL
========================= */
function seleccionarRol(rol) {
    document.getElementById('user-role').value = rol;

    const subtitle = document.getElementById('reg-subtitle');
    const title = document.getElementById('reg-title');
    const boxDireccion = document.getElementById('box-direccion');

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
function showError(id, message) {
    const input = document.getElementById(id);
    if (!input) return;
    let errorText = input.parentElement.querySelector('.error-text');
    if (!errorText) {
        errorText = document.createElement('small');
        errorText.className = 'error-text';
        input.parentElement.appendChild(errorText);
    }
    input.style.borderColor = "var(--error)";
    if (errorText) errorText.innerText = message;
}

function clearError(id) {
    const input = document.getElementById(id);
    if (!input) return;
    const errorText = input.parentElement.querySelector('.error-text');
    input.style.borderColor = "#e2e8f0";
    if (errorText) errorText.innerText = "";
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

/* =========================
   VALIDAR PASO 2 (datos personales)
========================= */
function validateStep2() {
    let valid = true;

    const doc = document.getElementById('reg-num-doc');
    const nombre = document.getElementById('reg-nombre');
    const apellido = document.getElementById('reg-apellido');
    const correo = document.getElementById('reg-correo');
    const tel = document.getElementById('reg-tel');
    const pass = document.getElementById('reg-pass');
    const confirmarPass = document.getElementById('reg-confirmar-pass');
    const role = document.getElementById('user-role').value;
    const direccion = document.getElementById('reg-direccion');

    if (!doc.value.trim()) { showError('reg-num-doc', "El número de documento es obligatorio"); valid = false; }
    else if (!patterns.doc.test(doc.value.trim())) { showError('reg-num-doc', "Usa entre 6 y 12 dígitos, sin espacios ni letras"); valid = false; }
    else clearError('reg-num-doc');
    if (!nombre.value.trim()) { showError('reg-nombre', "El nombre es obligatorio"); valid = false; }
    else if (!patterns.name.test(nombre.value.trim())) { showError('reg-nombre', "Usa entre 3 y 40 letras y espacios"); valid = false; }
    else clearError('reg-nombre');
    if (!apellido.value.trim()) { showError('reg-apellido', "El apellido es obligatorio"); valid = false; }
    else if (!patterns.name.test(apellido.value.trim())) { showError('reg-apellido', "Usa entre 3 y 40 letras y espacios"); valid = false; }
    else clearError('reg-apellido');
    if (!correo.value.trim()) { showError('reg-correo', "El correo es obligatorio"); valid = false; }
    else if (!patterns.email.test(correo.value.trim())) { showError('reg-correo', "Escribe un correo válido, por ejemplo nombre@dominio.com"); valid = false; }
    else clearError('reg-correo');
    if (!tel.value.trim()) { showError('reg-tel', "El teléfono es obligatorio"); valid = false; }
    else if (!patterns.phone.test(tel.value.trim())) { showError('reg-tel', "Debe tener 10 dígitos y comenzar por 3, sin espacios ni guiones"); valid = false; }
    else clearError('reg-tel');
    if (!pass.value) { showError('reg-pass', "La contraseña es obligatoria"); valid = false; }
    else if (pass.value.length < 6) { showError('reg-pass', "La contraseña debe tener al menos 6 caracteres"); valid = false; }
    else clearError('reg-pass');
    if (!confirmarPass.value) { showError('reg-confirmar-pass', "Confirma tu contraseña"); valid = false; }
    else if (confirmarPass.value !== pass.value) { showError('reg-confirmar-pass', "Las contraseñas no coinciden"); valid = false; }
    else clearError('reg-confirmar-pass');
    if (role !== 'VETERINARIA' && direccion && !direccion.value.trim()) {
        showError('reg-direccion', "La dirección es obligatoria"); valid = false;
    } else if (role !== 'VETERINARIA' && direccion && direccion.value.trim().length < 5) {
        showError('reg-direccion', "La dirección debe tener al menos 5 caracteres"); valid = false;
    } else if (direccion) clearError('reg-direccion');

    if (valid) {
        if (role === 'VETERINARIA') {
            document.getElementById('section-vet').classList.remove('hidden');
            document.getElementById('section-pet').classList.add('hidden');
        } else {
            document.getElementById('section-pet').classList.remove('hidden');
            document.getElementById('section-vet').classList.add('hidden');
        }
        nextStep(3);
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
        const nit = document.getElementById('vet-nit');
        const vNombre = document.getElementById('vet-nombre');
        const vTel = document.getElementById('vet-tel');
        const vCorreo = document.getElementById('vet-correo');

        if (!nit.value.trim()) { showError('vet-nit', "El NIT es obligatorio"); valid = false; }
        else if (!patterns.nit.test(nit.value.trim())) { showError('vet-nit', "El NIT debe tener entre 9 y 11 dígitos"); valid = false; }
        else clearError('vet-nit');
        if (!vNombre.value.trim()) { showError('vet-nombre', "El nombre comercial es obligatorio"); valid = false; }
        else if (vNombre.value.trim().length < 3) { showError('vet-nombre', "El nombre debe tener al menos 3 caracteres"); valid = false; }
        else clearError('vet-nombre');
        if (!vTel.value.trim()) { showError('vet-tel', "El teléfono corporativo es obligatorio"); valid = false; }
        else if (!/^\d{7,15}$/.test(vTel.value.trim())) { showError('vet-tel', "Usa entre 7 y 15 dígitos, sin espacios ni guiones"); valid = false; }
        else clearError('vet-tel');
        if (!vCorreo.value.trim()) { showError('vet-correo', "El correo de la empresa es obligatorio"); valid = false; }
        else if (!patterns.email.test(vCorreo.value.trim())) { showError('vet-correo', "Escribe un correo válido, por ejemplo clinica@dominio.com"); valid = false; }
        else clearError('vet-correo');

        if (valid) nextStep(4);
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

        if (pNombre.value.trim().length < 2) { showError('pet-nombre', "Nombre requerido"); valid = false; } else clearError('pet-nombre');
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
function addService() {
    const container = document.getElementById('services-container');
    const plantillaSelect = container.querySelector('.service-item select').cloneNode(true);
    plantillaSelect.value = "";

    const div = document.createElement('div');
    div.classList.add('service-item');

    const select = plantillaSelect;
    select.name = `nuevaVeterinaria.servicios[${serviceIndex}].idServicio`;
    select.id = '';

    const precio = document.createElement('input');
    precio.type = 'number';
    precio.className = 'service-precio';
    precio.placeholder = 'Precio';
    precio.min = '0';
    precio.name = `nuevaVeterinaria.servicios[${serviceIndex}].precio`;

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'btn-remove';
    btn.onclick = function () { quitarServicio(btn); };
    btn.innerHTML = '<i class="fas fa-trash"></i>';

    div.appendChild(select);
    div.appendChild(precio);
    div.appendChild(btn);
    container.appendChild(div);

    serviceIndex++;
}

function quitarServicio(btn) {
    const container = document.getElementById('services-container');
    if (container.children.length <= 1) return; // siempre debe quedar al menos uno
    btn.closest('.service-item').remove();
    reindexarServicios();
}

function reindexarServicios() {
    const items = document.querySelectorAll('#services-container .service-item');
    items.forEach((item, i) => {
        const select = item.querySelector('select');
        const precio = item.querySelector('.service-precio');
        select.name = `nuevaVeterinaria.servicios[${i}].idServicio`;
        precio.name = `nuevaVeterinaria.servicios[${i}].precio`;
    });
    serviceIndex = items.length;
}

function validateStep4() {
    const services = document.querySelectorAll('.service-item');
    const errEl = document.getElementById('err-services');

    let algunoValido = [...services].some(item => item.querySelector('select').value);
    if (services.length === 0 || !algunoValido) {
        errEl.innerText = "Agrega al menos un servicio";
        return;
    }
    errEl.innerText = "";

    initMap();
    nextStep(5);
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
    const direccion = document.getElementById('vet-direccion');

    if (direccion.value.trim().length < 5) {
        showError('vet-direccion', "Ingresa una dirección válida");
        return;
    }
    clearError('vet-direccion');

    document.getElementById('regForm').submit();
}

/* =========================
   LIMPIAR ERRORES EN TIEMPO REAL + RESTAURAR PASO SI HUBO ERRORES DEL SERVIDOR
========================= */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('input, select, textarea').forEach(input => {
        input.addEventListener('input', () => {
            if (input.value.trim() !== "") clearError(input.id);
        });
    });

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

    if (checkbox.checked) {
        hours.classList.remove('hidden');
        status.innerText = "Abierto";
        status.style.color = "#22c55e";
        card.style.background = "#f0f7ff";
        card.style.borderColor = "var(--primary)";
    } else {
        hours.classList.add('hidden');
        status.innerText = "Cerrado";
        status.style.color = "#ef4444";
        card.style.background = "#f8fafc";
        card.style.borderColor = "transparent";
    }
}
