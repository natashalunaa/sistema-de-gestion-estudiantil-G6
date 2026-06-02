package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("docente_periodo")
public class DocentePeriodo extends Model {

    public String getDocenteDni() {
        return getString("docente_dni");
    }

    public void setDocenteDni(String docenteDni) {
        set("docente_dni", docenteDni);
    }

    public Long getIdPeriodo() {
        return getLong("id_periodo");
    }

    public void setIdPeriodo(Long idPeriodo) {
        set("id_periodo", idPeriodo);
    }
}
