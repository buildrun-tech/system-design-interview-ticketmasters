package tech.buildrun.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tb_users", schema = "public")
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    @Column(unique = true)
    public String username;

    @Column(unique = true)
    public String email;

    public String password;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    public RoleEntity role;

    public UserEntity() {
    }

}
