package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("docente")
@IdName("dni")
public class Docente extends Model {

    public String getDni() {
        return getString("dni");
    }

    public void setDni(String dni) {
        set("dni", dni);
    }

    public String getNroLegajo() {
        return getString("nro_legajo");
    }

    public void setNroLegajo(String nroLegajo) {
        set("nro_legajo", nroLegajo);
    }

    public String getTitulo() {
        return getString("titulo");
    }

    public void setTitulo(String titulo) {
        set("titulo", titulo);
    }
}
