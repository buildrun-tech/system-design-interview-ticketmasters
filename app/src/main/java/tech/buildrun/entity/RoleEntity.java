package tech.buildrun.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "tb_roles", schema = "public")
public class RoleEntity extends PanacheEntityBase {

    @Id
    @Column(name = "role_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "name", unique = true, nullable = false)
    public String name;

    @ManyToMany
    @JoinTable(
            name = "tb_role_scopes",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "scope_id"))
    public Set<ScopeEntity> scopes;
}
