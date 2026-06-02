package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("persona")
@IdName("dni")
public class Persona extends Model {

    public String getDni() {
        return getString("dni");
    }

    public void setDni(String dni) {
        set("dni", dni);
    }

    public String getApellido() {
        return getString("apellido");
    }

    public void setApellido(String apellido) {
        set("apellido", apellido);
    }

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    public String getNroContacto() {
        return getString("nro_contacto");
    }

    public void setNroContacto(String nroContacto) {
        set("nro_contacto", nroContacto);
    }

    public String getMail() {
        return getString("mail");
    }

    public void setMail(String mail) {
        set("mail", mail);
    }
}
