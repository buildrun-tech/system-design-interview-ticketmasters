package tech.buildrun.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_users", schema = "public")
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    public String username;

    public String email;

    public String password;

    public UserEntity() {
    }

}
