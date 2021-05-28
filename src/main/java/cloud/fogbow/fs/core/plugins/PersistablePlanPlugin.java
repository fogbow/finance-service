package cloud.fogbow.fs.core.plugins;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

// TODO documentation
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class PersistablePlanPlugin implements PlanPlugin {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, unique = true)
    private Long id;
}
