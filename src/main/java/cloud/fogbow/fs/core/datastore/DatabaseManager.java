package cloud.fogbow.fs.core.datastore;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserId;
import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;

@Component
public class DatabaseManager {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PlanPluginRepository planPluginRepository;
	
	public DatabaseManager() {
	}

    public void saveUser(FinanceUser user) {
        userRepository.save(user);
    }
	
	public void removeUser(String userId, String provider) throws InvalidParameterException {
	    FinanceUser user = userRepository.findByUserId(new UserId(userId, provider));
		userRepository.delete(user);
	}

	public List<FinanceUser> getRegisteredUsers() {
	    return userRepository.findAll();
	}

    public List<PersistablePlanPlugin> getRegisteredPlanPlugins() {
        return planPluginRepository.findAll();
    }

    public void savePlanPlugin(PersistablePlanPlugin plugin) {
        planPluginRepository.save(plugin);
    }

    public void removePlanPlugin(PersistablePlanPlugin plugin) {
        planPluginRepository.delete(plugin);
    }
}
