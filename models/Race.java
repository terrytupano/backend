package models;

import org.javalite.activejdbc.*;
import org.javalite.activejdbc.annotations.*;

@Table("reslr")
@CompositePK({"redate", "rerace", "rehorse"})
public class Race extends Model {

}
