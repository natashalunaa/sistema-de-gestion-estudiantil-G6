package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("examen_final_materia")
public class ExamenFinalMateria extends Model {

    public Long getIdExamen() {
        return getLong("id_examen");
    }

    public void setIdExamen(Long idExamen) {
        set("id_examen", idExamen);
    }

    public String getCodMateria() {
        return getString("cod_materia");
    }

    public void setCodMateria(String codMateria) {
        set("cod_materia", codMateria);
    }
}
