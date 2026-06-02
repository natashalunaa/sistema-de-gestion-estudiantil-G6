package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("alumno")
@IdName("dni")
public class Alumno extends Model {

    public String getDni() {
        return getString("dni");
    }

    public void setDni(String dni) {
        set("dni", dni);
    }

    public String getTipoAlumno() {
        return getString("tipo_alumno");
    }

    public void setTipoAlumno(String tipoAlumno) {
        set("tipo_alumno", tipoAlumno);
    }
}
