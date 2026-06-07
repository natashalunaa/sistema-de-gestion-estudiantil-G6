package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("materia_correlatividad")
@IdName("id")
public class MateriaCorrelatividad extends Model {

    public Long getId() {
        return getLong("id");
    }

    public void setId(Long id) {
        set("id", id);
    }

    public Long getIdCorrelatividad() {
        return getLong("id_correlatividad");
    }

    public void setIdCorrelatividad(Long idCorrelatividad) {
        set("id_correlatividad", idCorrelatividad);
    }

    public String getMateriaOrigen() {
        return getString("materia_origen");
    }

    public void setMateriaOrigen(String materiaOrigen) {
        set("materia_origen", materiaOrigen);
    }

    public String getMateriaRequerida() {
        return getString("materia_requerida");
    }

    public void setMateriaRequerida(String materiaRequerida) {
        set("materia_requerida", materiaRequerida);
    }
}
