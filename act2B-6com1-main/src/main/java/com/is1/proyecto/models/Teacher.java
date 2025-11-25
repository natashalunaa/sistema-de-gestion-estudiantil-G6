package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;  // le indica a jdbc cual es la primary key
import org.javalite.activejdbc.annotations.Table;

@Table("teachers")
@IdName("id")
public class Teacher extends Model {
    public String getEmail() {
        return getString("email"); 
    }

    public void setEmail(String email) {
        set("email", email); 
    }

    public String getDegree() {
        return getString("degree"); 
    }

    public void setDegree(String degree) {
        set("degree", degree); 
    }

    public Integer getDni() {
        return getInteger("id"); 
    }

    public void setDni(Integer dni) {
        set("id", dni); 
    }

}