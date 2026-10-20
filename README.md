# 🚀 Guía Git & GitHub para el Equipo | VetLink

¡Hola a todos! 👋 Esta guía está diseñada para que trabajemos en equipo de forma ordenada, evitemos enredos con el código y todos sepamos exactamente cómo subir nuestros avances. 

---

## 🛠️ Requisito previo
Asegúrate de tener **Git Bash** instalado en tu equipo. Si aún no lo tienes, puedes descargarlo e instalarlo antes de continuar.

---

## 1. 📥 Clonar el proyecto (Solo la primera vez)

Si es tu primera vez trabajando en el proyecto, descarga el repositorio a tu máquina:

```bash
cd Documents/
git clone https://github.com/SDDev-01/VetLink.git
cd VetLink/
```

---

## 2. 🔄 Preparar tu entorno antes de programar

Antes de empezar a escribir código o crear nuevas funciones, **SIEMPRE** asegúrate de tener la versión más reciente del proyecto:

```bash
cd Documents/VetLink/
git checkout main
git pull origin main
```

💡 *Tip: Hacer esto siempre antes de trabajar evita que te aparezcan conflictos de código más adelante.*

---

## 3. 🌿 Crear tu propia rama

Para trabajar tranquilos sin afectar el código de los demás, cada tarea se realiza en su propia rama:

```bash
git checkout -b nombre-de-tu-rama
```

### 🏷️ Convenciones para nombrar ramas:
* **Nuevas funcionalidades:** `feature/nombre-tarea` (Ej: `feature/login`)
* **Corrección de errores:** `fix/nombre-bug` (Ej: `fix/error-login`)
* **Diseño / Estilos:** `style/componente` (Ej: `style/navbar`)
* **Documentación:** `docs/tema` (Ej: `docs/diagrama-bd`)

---

## 4. 💻 Desarrollo y guardado local

Una vez estés en tu rama, trabaja normalmente en tus archivos. Cuando termines o quieras guardar un avance significativo:

1. Prepara los archivos:
   ```bash
   git add .
   ```
2. Guarda el punto de control con un mensaje claro:
   ```bash
   git commit -m "Explicación breve de lo que hiciste"
   ```
   *Ejemplo:* `git commit -m "Agrega validación al formulario de registro"`

---

## 5. 📤 Subir tus cambios a GitHub

Cuando tus cambios estén listos y probados localmente, súbelos a tu rama en GitHub:

```bash
git push origin nombre-de-tu-rama
```

> ⚠️ **Importante:** Sube siempre a tu rama personal (`origin tu-rama`). La rama `main` la actualizamos únicamente a través de Pull Requests.

---

## 6. 🔀 Crear un Pull Request (PR)

1. Ve a la página del repositorio en [GitHub](https://github.com/SDDev-01/VetLink).
2. Verás un mensaje emergente con el botón **"Compare & pull request"**. Haz clic en él.
3. Escribe un resumen de los cambios que realizaste.
4. Confirma la creación del PR y avísale al equipo para revisarlo e integrarlo a `main`.

---

## 💡 Consejos para un flujo sin problemas

* **Mantén tus ramas enfocadas:** Es mejor hacer ramas pequeñas para tareas específicas que una sola rama gigante con muchos cambios.
* **Mensajes de commit descriptivos:** Ayudan a todo el equipo a entender rápido qué se cambió.
* **Si te surge una duda o error de Git:** ¡No te preocupes! Escribe por el grupo del equipo antes de forzar comandos y lo resolvemos juntos.

---

### ⚡ Resumen rápido diario
1. Actualiza `main`: `git checkout main` ➡️ `git pull origin main`
2. Crea tu rama: `git checkout -b feature/tu-tarea`
3. Trabaja y guarda: `git add .` ➡️ `git commit -m "tu mensaje"`
4. Subir a GitHub: `git push origin feature/tu-tarea`
5. Abre el **Pull Request** en la web de GitHub.
