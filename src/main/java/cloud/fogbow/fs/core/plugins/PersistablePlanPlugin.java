package cloud.fogbow.fs.core.plugins;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

// TODO documentation
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class PersistablePlanPlugin implements PlanPlugin {
    @Id
    protected String name;
}
