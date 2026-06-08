# Sistema de Gestión Estudiantil - Grupo 6

Proyecto académico desarrollado para la materia **Ingeniería de Software II** de la Universidad Nacional de Río Cuarto. Este sistema representa la evolución del desarrollo iniciado en 2025, diseñado para la gestión integral de información académica.

##  Requisitos Previos

Para ejecutar el proyecto, debe tener instalado:
* **Docker Desktop** instalado y en ejecución.

##  Instalación y Ejecución

Hemos configurado el entorno utilizando contenedores Docker para mejor portabilidad. 

### Levantar el entorno
Para compilar, construir la imagen y levantar el servidor web, ejecuta el siguiente comando en la terminal desde la raíz del proyecto:

```bash
Copy-Item .env.example .env 

```bash
docker compose up --build

### Acceso al sistema:
Una vez que el servidor esté listo, ingresa en tu navegador a: http://localhost:8505/

## Restricciones y Tecnologías
Base de Datos: PostgreSQL (elegido por su robustez y ausencia de licencias comerciales).

Backend: Java (seleccionado por su confiabilidad).

Frontend: Mustache (para la creación ágil de plantillas gráficas).

## Equipo de Desarrollo
Cerrudo, Leila
Fernandez, Nicolas
Gonzalez, Joaquin
Luna, Natasha
Pari, Jennifer

---

Universidad Nacional de Río Cuarto - 2026
