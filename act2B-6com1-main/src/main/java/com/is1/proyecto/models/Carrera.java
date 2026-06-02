package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("carrera")
@IdName("cod_carrera")
public class Carrera extends Model {

    public String getCodCarrera() {
        return getString("cod_carrera");
    }

    public void setCodCarrera(String codCarrera) {
        set("cod_carrera", codCarrera);
    }

    public String getNombreCarrera() {
        return getString("nombre_carrera");
    }

    public void setNombreCarrera(String nombreCarrera) {
        set("nombre_carrera", nombreCarrera);
    }
}
