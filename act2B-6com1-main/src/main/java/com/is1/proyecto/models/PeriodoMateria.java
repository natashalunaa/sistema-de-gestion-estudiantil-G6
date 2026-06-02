package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("periodo_materia")
public class PeriodoMateria extends Model {

    public Long getIdPeriodo() {
        return getLong("id_periodo");
    }

    public void setIdPeriodo(Long idPeriodo) {
        set("id_periodo", idPeriodo);
    }

    public String getCodMateria() {
        return getString("cod_materia");
    }

    public void setCodMateria(String codMateria) {
        set("cod_materia", codMateria);
    }
}
