package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("alumno_plan_estudio")
public class AlumnoPlanEstudio extends Model {

    public String getAlumnoDni() {
        return getString("alumno_dni");
    }

    public void setAlumnoDni(String alumnoDni) {
        set("alumno_dni", alumnoDni);
    }

    public String getCodPlan() {
        return getString("cod_plan");
    }

    public void setCodPlan(String codPlan) {
        set("cod_plan", codPlan);
    }
}
