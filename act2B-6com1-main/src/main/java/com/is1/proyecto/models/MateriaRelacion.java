package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("materia_relacion")
public class MateriaRelacion extends Model {

    public String getMateriaOrigen() {
        return getString("materia_origen");
    }

    public void setMateriaOrigen(String materiaOrigen) {
        set("materia_origen", materiaOrigen);
    }

    public String getMateriaDestino() {
        return getString("materia_destino");
    }

    public void setMateriaDestino(String materiaDestino) {
        set("materia_destino", materiaDestino);
    }
}
