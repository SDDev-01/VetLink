# Nginx (proxy inverso de VetLink)

`nginx.conf.template` es una plantilla de la imagen oficial `nginx:1.30-alpine`.
Al arrancar, el contenedor la convierte en `/etc/nginx/nginx.conf` y rellena el
DNS interno de la red de Compose.

| Ruta pública | Destino |
|---|---|
| `/internal` y `/internal/...` | 404 devuelto por Nginx (nunca llega a los backends) |
| `/p/...` | `fastapi:8000`, con la URI intacta (prefijo `/p/` incluido) |
| todo lo demás | `spring:8080` |

Solo Nginx publica un puerto (`${NGINX_HTTP_PORT:-80}`). Con Podman rootless,
los puertos por debajo de 1024 no se pueden publicar salvo que ajustes
`net.ipv4.ip_unprivileged_port_start`, así que define `NGINX_HTTP_PORT=8081`
en `.env`.

## Validación

```bash
# Sintaxis de Nginx (no necesita los backends)
podman run --rm -e NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1 -e NGINX_ENVSUBST_OUTPUT_DIR=/etc/nginx \
  -v ./nginx/nginx.conf.template:/etc/nginx/templates/nginx.conf.template:ro,Z \
  docker.io/library/nginx:1.30-alpine nginx -t

# Compose válido (no crea ni borra nada)
podman compose config      # o: docker compose config
```

## Pruebas con la pila levantada

`P` es el puerto público (80, o el valor de `NGINX_HTTP_PORT`).

```bash
P=80
# 1. Spring a través de Nginx
curl -i http://localhost:$P/
# 2. Ninguna redirección apunta al 8080 (debe ser /login o http://localhost:$P/...)
curl -s -o /dev/null -w '%{http_code} %header{location}\n' http://localhost:$P/mascotas
# 3. Llega a FastAPI: 404 de FastAPI ({"detail":"Not Found"}), no la página de Nginx
#    (mientras no exista el servicio fastapi, Nginx devuelve 502)
curl -i http://localhost:$P/p/token-inventado
# 4. 404 de Nginx (cuerpo HTML con "nginx")
curl -i http://localhost:$P/internal/pasaportes
# 5. Los puertos internos no están publicados (estas dos deben fallar al conectar)
curl -m 3 http://localhost:8080/ ; curl -m 3 http://localhost:8000/
podman compose ps          # solo nginx (y mysql, ver nota) muestran puertos
```

Nota: MySQL sigue publicando `${MYSQL_PORT:-3307}` en el host (es configuración
previa, este cambio no la toca).
