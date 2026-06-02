package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("docente_responsable_materia")
public class DocenteResponsableMateria extends Model {

    public String getDocenteDni() {
        return getString("docente_dni");
    }

    public void setDocenteDni(String docenteDni) {
        set("docente_dni", docenteDni);
    }

    public String getCodMateria() {
        return getString("cod_materia");
    }

    public void setCodMateria(String codMateria) {
        set("cod_materia", codMateria);
    }
}
