package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("correlatividad")
@IdName("id_correlatividad")
public class Correlatividad extends Model {

    public Long getIdCorrelatividad() {
        return getLong("id_correlatividad");
    }

    public void setIdCorrelatividad(Long idCorrelatividad) {
        set("id_correlatividad", idCorrelatividad);
    }

    public String getCorrel() {
        return getString("correl");
    }

    public void setCorrel(String correl) {
        set("correl", correl);
    }
}
