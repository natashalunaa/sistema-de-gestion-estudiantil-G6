package com.is1.proyecto.models;

import java.util.Date;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("periodo")
@IdName("id_periodo")
public class Periodo extends Model {

    public Long getIdPeriodo() {
        return getLong("id_periodo");
    }

    public void setIdPeriodo(Long idPeriodo) {
        set("id_periodo", idPeriodo);
    }

    public Date getFechaInicio() {
        return getDate("fecha_inicio");
    }

    public void setFechaInicio(Date fechaInicio) {
        set("fecha_inicio", fechaInicio);
    }

    public Date getFechaFin() {
        return getDate("fecha_fin");
    }

    public void setFechaFin(Date fechaFin) {
        set("fecha_fin", fechaFin);
    }

    public String getCargo() {
        return getString("cargo");
    }

    public void setCargo(String cargo) {
        set("cargo", cargo);
    }

    public String getTypePeriodo() {
        return getString("type_periodo");
    }

    public void setTypePeriodo(String typePeriodo) {
        set("type_periodo", typePeriodo);
    }
}
