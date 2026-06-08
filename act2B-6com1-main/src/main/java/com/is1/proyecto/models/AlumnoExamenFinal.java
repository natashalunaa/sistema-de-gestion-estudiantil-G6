package com.is1.proyecto.models;

import java.math.BigDecimal;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("alumno_examen_final")
public class AlumnoExamenFinal extends Model {

    public String getAlumnoDni() {
        return getString("alumno_dni");
    }

    public void setAlumnoDni(String alumnoDni) {
        set("alumno_dni", alumnoDni);
    }

    public Long getIdExamen() {
        return getLong("id_examen");
    }

    public void setIdExamen(Long idExamen) {
        set("id_examen", idExamen);
    }

    public BigDecimal getNota() {
        return getBigDecimal("nota");
    }

    public void setNota(BigDecimal nota) {
        set("nota", nota);
    }
}
