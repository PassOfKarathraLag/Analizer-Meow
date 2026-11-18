# Analizer Meow

App de diagnóstico de red WiFi inspirada en "WiFi Analyzer", con mejoras propias.

## Funciones incluidas
- **Inicio**: medidor de señal en vivo (%).
- **Canales**: calificación por estrellas y canal recomendado (2.4G y 5G) —
  `ChannelAnalyzer.kt` (penaliza por redes vecinas e intensidad de su señal).
- **WiFi**: lista de redes cercanas + gráfica de "montañas" de señal/ruido
  (con MPAndroidChart).
- **Prueba de velocidad**: ping, jitter y descarga ligera (`SpeedTestManager.kt`),
  pensada para no trabarse con señal débil.
- **Dispositivos conectados, mejorado**: en vez de solo "Dispositivo general /
  192.168.x.x", cruza tabla ARP + fabricante (OUI) + hostname para dar un
  nombre y tipo mucho más útil (`DeviceScanner.kt`, `OuiVendorLookup.kt`).
- **Posibles fallas**: diagnóstico de *por qué* la red está lenta (señal
  débil, canal saturado, latencia del ISP, router sobrecargado) —
  `NetworkDiagnostics.kt`.
- **Configuración del router**: tabla de credenciales de fábrica públicas +
  acceso directo al panel admin — `RouterConfigHelper.kt`.
- **Auditor de seguridad de tu red** (`SecurityChecker.kt`): detecta cifrado
  débil (WEP/WPA), WPS activo y SSID de fábrica, para concientización en
  clase de seguridad informática, **sin** intentar obtener contraseñas de
  ninguna red (eso no está incluido a propósito).

---

## No sé compilar en Android Studio: ¿cómo obtengo el APK?

Este repo incluye un archivo en `.github/workflows/build-apk.yml` que hace
que **GitHub compile el APK por ti**, en sus propios servidores, cada vez
que subas el código. No necesitas instalar Android Studio ni nada en tu
computadora para esa parte. Después de subir el repo (pasos de abajo):

1. Entra a tu repositorio en github.com.
2. Ve a la pestaña **Actions** (arriba, junto a "Code" y "Pull requests").
3. Verás un proceso llamado **"Compilar APK"** corriendo o ya terminado
   (tarda unos minutos la primera vez).
4. Cuando termine (ícono verde ✅), entra a esa ejecución y baja hasta
   **Artifacts**: ahí vas a encontrar `analizer-meow-debug-apk` para
   descargar. Ese .zip contiene el `app-debug.apk` listo para instalar en
   un celular Android (activa "Instalar apps de orígenes desconocidos" en
   el celular para poder abrirlo).

Si el ícono sale en rojo ❌, entra a ver el log: lo más probable es que
falte algo de la configuración del SDK de Android, pero el mensaje de
error te dice exactamente qué línea falló.

---

## Cómo subirlo a GitHub (sin usar la terminal)

Como no usas Android Studio, la forma más simple es con **GitHub Desktop**
(una app con botones, no requiere comandos):

1. Descarga e instala **GitHub Desktop**: https://desktop.github.com
2. Ábrelo e inicia sesión con tu cuenta de GitHub (o crea una gratis).
3. Click en **File → New repository** (o **Add → Create new repository**).
4. En "Name" pon `analizer-meow`, y en "Local path" elige una carpeta en
   tu computadora.
5. Esto crea una carpeta vacía con el repo ya inicializado. **Copia dentro
   de esa carpeta** todo el contenido del ZIP que te compartí (las carpetas
   `app/`, `.github/`, y los archivos `README.md`, `build.gradle.kts`, etc.),
   reemplazando lo que GitHub Desktop haya puesto por defecto.
6. Vuelve a GitHub Desktop: va a mostrar automáticamente todos los
   archivos nuevos detectados.
7. Abajo a la izquierda escribe un mensaje, por ejemplo
   "Primera versión de Analizer Meow", y da click en **Commit to main**.
8. Arriba da click en **Publish repository**. Puedes dejarlo público o
   marcarlo privado.
9. ¡Listo! Ya está en tu cuenta de GitHub, y el workflow de Actions se
   dispara solo.

### Alternativa: subirlo directo desde el navegador (sin instalar nada)
1. Entra a github.com → **New repository** → ponle `analizer-meow` →
   **Create repository** (déjalo vacío).
2. En la página del repo recién creado, busca el enlace que dice
   **"uploading an existing file"**.
3. Arrastra ahí todos los archivos y carpetas del proyecto (puedes
   arrastrar varias carpetas a la vez desde el explorador de archivos).
4. Abajo pon un mensaje de commit y da click en **Commit changes**.

Nota: esta opción web a veces falla con carpetas muy anidadas; si ves que
no sube todo correctamente, usa GitHub Desktop (opción de arriba), que es
más confiable para proyectos con muchas subcarpetas como este.

### Cada vez que quieras actualizar el código
- Con GitHub Desktop: reemplaza/edita los archivos en tu carpeta local,
  vuelve a la app, vas a ver los cambios listados, escribe un mensaje y
  dale **Commit** → **Push origin**.
- Con el navegador: entra al archivo que quieras cambiar en GitHub, botón
  del lápiz (editar), y al final **Commit changes**.

## Permisos que pide la app y por qué
- `ACCESS_FINE_LOCATION`: Android lo exige para listar SSIDs cercanos (no
  se usa para ubicar al usuario).
- `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE`: leer redes y forzar un re-escaneo.
- `INTERNET`: solo para el ping/descarga de la prueba de velocidad.

## Si en algún momento sí quieres abrirlo en Android Studio
1. Instala Android Studio (gratis, de developer.android.com).
2. **File → Open** y selecciona la carpeta `analizer-meow`.
3. Deja que sincronice Gradle (bajará las librerías automáticamente).
4. Aún falta crear los layouts visuales (`activity_main.xml` con las 4
   pestañas) — eso se hace con el editor visual, no a mano, y es el único
   paso pendiente para tener la interfaz completa.
