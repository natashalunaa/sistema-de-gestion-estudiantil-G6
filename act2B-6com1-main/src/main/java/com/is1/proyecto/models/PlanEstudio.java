package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("plan_estudio")
@IdName("cod_plan")
public class PlanEstudio extends Model {

    public String getCodPlan() {
        return getString("cod_plan");
    }

    public void setCodPlan(String codPlan) {
        set("cod_plan", codPlan);
    }

    public Integer getAnio() {
        return getInteger("anio");
    }

    public void setAnio(Integer anio) {
        set("anio", anio);
    }

    public String getCarreraTuvo() {
        return getString("carrera_tuvo");
    }

    public void setCarreraTuvo(String carreraTuvo) {
        set("carrera_tuvo", carreraTuvo);
    }

    public String getCarreraVigente() {
        return getString("carrera_vigente");
    }

    public void setCarreraVigente(String carreraVigente) {
        set("carrera_vigente", carreraVigente);
    }
}
