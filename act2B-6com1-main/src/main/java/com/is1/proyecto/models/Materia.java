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

    public Integer getNumMateria() {
        return getInteger("num_materia");
    }

    public void setNumMateria(Integer numMateria) {
        set("num_materia", numMateria);
    }
}
