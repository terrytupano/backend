package models;

import org.javalite.activejdbc.*;
import org.javalite.activejdbc.annotations.*;

@DbName("flicka2")
@Table("magazine") 
@CompositePK({"maedate","maedistance","maerace","maehorsename","mahrace"})
public class Magazine extends Model { 

}  
