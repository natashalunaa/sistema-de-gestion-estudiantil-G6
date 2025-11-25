package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("personas")
@IdName("id")
public class Person extends Model {
    public Integer getDNI(){
        return getInteger("id");
    }

    public void setDNI(Integer dni){
        set("id",dni);
    }

    public String getFirstName() {
        return getString("first_name");
    }

    public void setFirstName(String name) {
        set("first_name", name); 
    }

    public String getLastName() {
        return getString("last_name"); 
    }

    public void setLastName(String lastName) {
        set("last_name", lastName); 
    }

}