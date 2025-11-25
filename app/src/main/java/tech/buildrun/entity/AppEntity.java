package tech.buildrun.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_apps", schema = "public")
public class AppEntity extends PanacheEntityBase {

    @Id
    @Column(name = "app_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "name", unique = true, nullable = false)
    public String name;

    @Column(unique = true, nullable = false)
    public UUID clientId;

    @Column(nullable = false)
    public UUID clientSecret;

    @ManyToMany
    @JoinTable(
            name = "tb_apps_scopes",
            joinColumns = @JoinColumn(name = "app_id"),
            inverseJoinColumns = @JoinColumn(name = "scope_id"))
    public Set<ScopeEntity> scopes;
}
