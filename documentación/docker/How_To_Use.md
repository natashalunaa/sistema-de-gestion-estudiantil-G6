# Guía para usar la aplicación con Docker y Docker Compose

Este documento explica cómo ejecutar la aplicación utilizando **Docker** y **Docker Compose**, incluso si nunca los usaste antes.

---

## 1. Requisitos previos

Antes de comenzar, necesitás instalar:

### Docker Desktop (Windows o macOS)
Descargar desde:  
https://www.docker.com/products/docker-desktop/

### Docker Engine + Docker Compose (Linux)
En distribuciones basadas en Debian/Ubuntu:

```bash
sudo apt update
sudo apt install docker.io docker-compose-plugin
```

## 2. Verificar instalación:
```bash
docker --version
docker compose version
```

## 3. Construir y ejecutar la aplicación
Para iniciar la aplicación por primera vez:

```bash
docker compose up --build
```

## 4. Ejecutar la aplicación nuevamente
Si ya construiste la imagen antes, podés iniciar la aplicación con:

```bash
docker compose up
```

## 5. Detener la aplicación
Para detener los contenedores:

```bash
docker compose down
```

## 6. Ver logs de la aplicación

```bash
docker compose logs -f
```

## 7. Acceder a la aplicación
Una vez que el contenedor esté corriendo, podés acceder desde tu navegador:

```bash
http://localhost:8505
```

## 8. Reconstruir completamente la imagen
Si hiciste cambios en el código:

```bash
docker compose build --no-cache
docker compose up
```

## 9. Eliminar contenedores, imágenes y caché
Para limpiar todo lo generado por Docker:

```bash
docker system prune -a
```

## 10. Problemas comunes
Docker no inicia
Asegurate de que Docker Desktop esté abierto (Windows/macOS).

Permisos en Linux
Si aparece un error de permisos:

```bash
sudo usermod -aG docker $USER
```

Cerrar sesión y volver a entrar.

## 11. Para desarrollar la app (no usar en produccion)
Si estas desarrollando la app, puedes usar el comando

```bash
docker compose -f docker-compose.dev.yaml up
```

el cual se debe correr cada vez que se realiza un cambio en la app

## 12. Comandos útiles
| Acción | Comando |
| :----- | :------ |
| Ver contenedores activos | docker ps |
| Ver todas las imágenes | docker images |
| Detener contenedor | docker stop <nombre> |
| Eliminar contenedor | docker rm <nombre> |
| Eliminar imagen | docker rmi <imagen> |