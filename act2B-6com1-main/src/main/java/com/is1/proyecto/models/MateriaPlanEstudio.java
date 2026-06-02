package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("materia_plan_estudio")
public class MateriaPlanEstudio extends Model {

    public String getCodMateria() {
        return getString("cod_materia");
    }

    public void setCodMateria(String codMateria) {
        set("cod_materia", codMateria);
    }

    public String getCodPlan() {
        return getString("cod_plan");
    }

    public void setCodPlan(String codPlan) {
        set("cod_plan", codPlan);
    }
}
