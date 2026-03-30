<div style="display: flex; justify-content: space-around">
   <img src="https://www.unrc.edu.ar/img/escudounrc.jpg" alt="Unrc" style="height: 100px;">
   <img src="https://www.exa.unrc.edu.ar/wp-content/uploads/2022/03/LOGO-ICONO-WEB.png" alt="Facultad de Ciencias Exactas" style="height: 100px;">
</div>
# Contenidos
* [Integrantes](#integrantes)
* [Introduccion](#sistema-de-gestión-estudiantil)
* [Problema a resolver](problema-a-resolver)
* [Funcionalidades](#usuario-de-sistema-y-funcionalidades-principales)
* [Restricciones tecnicas](#restricciones-técnicas)
* [Equipo](#tamaño-del-equipo)
* [Tecnologias elegidas](#tamaño-del-equipo)
* [Plazo Estimado](#plazo-estimado)
* [Cambios Ocurridos](#cambios-de-alcance-ocurridos)
* [Problemas Encontrados](#problemas-encontrados)
* [Organización del equipo](#formas-de-organización-del-equipo)

# Proyecto Ingeniería de Software

## Integrantes
* *Cerrudo Leila*
* *Fernandez Nicolas*
* *Gonzalez Joaquin*
* *Luna Natasha*
* *Pari Jennifer*
## Sistema de Gestión Estudiantil
>
La oficina de alumnos de la universidad necesita de un sistema para gestionar la información académica tanto de sus estudiantes como de los docentes. Se busca centralizar la información, llevar un registro de la situación académica de cada individuo.
El mayor desafío al que nos enfrentamos como analistas es unificar toda la información necesaria en una misma plataforma que lleve un control de los diferentes roles de usuario (docente, estudiantil, administrativo) y de los procesos involucrados en hacer el seguimiento del estudiante; teniendo en cuenta que anteriormente la universidad estaba trabajando con planillas y sistemas viejos que no se conectan entre sí.
### Problema a resolver
>Se busca resolver la desorganización de la información académica mediante la implementación de un sistema centralizado que permita su gestión y seguimiento.
### Usuario de sistema y Funcionalidades Principales
* Personal de la oficina de alumnos:
> * Registrar y actualizar datos de estudiantes y docentes
> * Gestionar planes de estudio
> * Administrar correlativas
> * Mantener actualizada la información académica
> * Controlar y gestionar el sistema en general
* Estudiante
> * Consultar su información personal
> * Ver su situación académica (materias aprobadas, en curso)
> * Consultar qué materias puede cursar (según correlativas)
> * Actualizar sus datos personales
* Docente
> * Consultar el listado de estudiantes de sus materias
> * Ver las materias a su cargo
> * Identificar su rol en la cátedra (jefe, ayudante, etc.)
> * Cargar notas de exámenes
### Restricciones Técnicas
> * Consistencia fuerte
>>Debe existir una base de datos única que evite duplicación.
> * Gestión de usuarios y roles
>>El sistema debe soportar distintos roles (administrativo, docente, estudiante) con permisos diferenciados.
> * Seguridad de la información
>>Protección de datos académicos y personales (autenticación de usuarios, control de accesos).
> * Disponibilidad
>>El sistema debe estar disponible en todo momento para consultas y carga de información.
> * Actualización en tiempo real
>>Los datos deben reflejarse actualizados para todos los usuarios.
### Tamaño del equipo
> Cinco estudiantes de Lic. en Computación
### Tecnologías elegidas
* Base de datos en Posgres SQL
> * Porque no tiene licencias comerciales
> * Es uno de los motores más fuertes del mercado
> * Cuenta con una fácil uso
* Java
> * Mayor confiabilidad personal

* Mustache
> * Parte gráfica que utiliza plantillas ya hechas que facilitan el uso y agiliza tiempo

### Plazo Estimado
90 días

### Cambios de alcance ocurridos
* Cambiar el motor SQLite por PostgreSQL


### Problemas Encontrados
Uno de los problemas que tuvimos es que no sabíamos que había que activar las Foreign Key en SQLite para que el motor realice las comprobaciones.
