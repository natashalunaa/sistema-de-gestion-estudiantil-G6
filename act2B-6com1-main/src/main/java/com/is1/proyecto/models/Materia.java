package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("materia")
@IdName("cod_materia")
public class Materia extends Model {

    public String getCodMateria() {
        return getString("cod_materia");
    }

    public void setCodMateria(String codMateria) {
        set("cod_materia", codMateria);
    }

    // nombre materia
    public String getNombreMateria() {
        return getString("nombre_materia ");
    }

    public void setNombreMateria(String nombreMateria) {
        set("nombre_materia", nombreMateria);
    }

    // Año de la materia
    public Integer getAnioMateria() {
        return getInteger("anio_materia");
    }

    public void setAnioMateria(Integer anioMateria) {
        set("anio_materia", anioMateria);
    }

    // Código de inscripción
    public String getCodInscripcion() {
        return getString("cod_inscripcion");
    }

    public void setCodInscripcion(String codInscripcion) {
        set("cod_inscripcion", codInscripcion);
    }

}
