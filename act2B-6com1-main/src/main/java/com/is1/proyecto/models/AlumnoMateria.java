package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("alumno_materia")
public class AlumnoMateria extends Model {

    public String getAlumnoDni() {
        return getString("alumno_dni");
    }

    public void setAlumnoDni(String alumnoDni) {
        set("alumno_dni", alumnoDni);
    }

    public String getCodMateria() {
        return getString("cod_materia");
    }

    public void setCodMateria(String codMateria) {
        set("cod_materia", codMateria);
    }
}
