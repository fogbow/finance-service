package cloud.fogbow.fs.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.util.ClassFactory;
import cloud.fogbow.fs.constants.Messages;

// Each package has to have its own ClassFactory

public class FsClassFactory implements ClassFactory {
	
    public Object createPluginInstance(String pluginClassName, String ... params)
            throws FatalErrorException {

        Object pluginInstance;
        Constructor<?> constructor;

        try {
            Class<?> classpath = Class.forName(pluginClassName);
            if (params.length > 0) {
                Class<String>[] constructorArgTypes = new Class[params.length];
                Arrays.fill(constructorArgTypes, String.class);
                constructor = classpath.getConstructor(constructorArgTypes);
            } else {
                constructor = classpath.getConstructor();
            }

            pluginInstance = constructor.newInstance(params);
        } catch (ClassNotFoundException e) {
            throw new FatalErrorException(String.format(Messages.Exception.UNABLE_TO_FIND_CLASS_S, pluginClassName));
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage(), e);
        }
        return pluginInstance;
    }
    
    public Object createPluginInstance(String pluginClassName, Object ... params)
            throws FatalErrorException {

        Object pluginInstance;
        Constructor<?> constructor;

        try {
            Class<?> classpath = Class.forName(pluginClassName);
            if (params.length > 0) {
                Class<?>[] constructorArgTypes = new Class[params.length];

                for (int i = 0; i < params.length; i++) {
                    constructorArgTypes[i] = params[i].getClass();
                }
                
                constructor = classpath.getConstructor(constructorArgTypes);
            } else {
                constructor = classpath.getConstructor();
            }

            pluginInstance = constructor.newInstance(params);
        } catch (ClassNotFoundException e) {
            throw new FatalErrorException(String.format(Messages.Exception.UNABLE_TO_FIND_CLASS_S, pluginClassName));
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage(), e);
        }
        return pluginInstance;
    }

    public Object createPlanPluginInstance(String pluginClassName, String planName, InMemoryUsersHolder usersHolder, 
            Map<String, String> pluginOptions, Object ... params) throws FatalErrorException {

        Object pluginInstance;
        Constructor<?> constructor;

        try {
            Class<?> classpath = Class.forName(pluginClassName);
            Class<?>[] constructorArgTypes = new Class[3 + params.length];

            constructorArgTypes[0] = String.class;
            constructorArgTypes[1] = InMemoryUsersHolder.class;
            constructorArgTypes[2] = Map.class;
            
            for (int i = 0; i < params.length; i++) {
                constructorArgTypes[3 + i] = params[3 + i].getClass();
            }
            
            constructor = classpath.getConstructor(constructorArgTypes);
            
            if (params.length > 0) {
                pluginInstance = constructor.newInstance(planName, usersHolder, pluginOptions, params);
            } else {
                pluginInstance = constructor.newInstance(planName, usersHolder, pluginOptions);
            }
            
        } catch (ClassNotFoundException e) {
            throw new FatalErrorException(String.format(Messages.Exception.UNABLE_TO_FIND_CLASS_S, pluginClassName));
        } catch (InvocationTargetException e) {
            throw new FatalErrorException(e.getCause().getMessage(), e);
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage(), e);
        }
        return pluginInstance;
    }
}

