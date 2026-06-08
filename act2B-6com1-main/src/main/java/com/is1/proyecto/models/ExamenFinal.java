package com.is1.proyecto.models;

import java.util.Date;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("examen_final")
@IdName("id_examen")
public class ExamenFinal extends Model {

    public Long getIdExamen() {
        return getLong("id_examen");
    }

    public void setIdExamen(Long idExamen) {
        set("id_examen", idExamen);
    }

    public Date getFecha() {
        return getDate("fecha");
    }

    public void setFecha(Date fecha) {
        set("fecha", fecha);
    }

    public String getCodMateria() {
        return getString("cod_materia");
    }

    public void setCodMateria(String codMateria) {
        set("cod_materia", codMateria);
    }
}
