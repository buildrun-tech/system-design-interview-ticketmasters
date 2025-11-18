package tech.buildrun.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_scopes", schema = "public")
public class ScopeEntity extends PanacheEntityBase {

    @Id
    @Column(name = "scope_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "name", unique = true, nullable = false)
    public String name;
}
