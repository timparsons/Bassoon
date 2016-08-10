package io.timparsons.bassoon;

import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sun.faces.spi.InjectionProvider;
import com.sun.faces.spi.InjectionProviderException;
import com.sun.faces.vendor.WebContainerInjectionProvider;
 
/**
 * JSF injection provider for Guice.
 * @author Joel Weight
 */
public abstract class GuiceInjectionProvider implements InjectionProvider
{
	/**
	 * default injector provided by the web container.
	 */
	private static final WebContainerInjectionProvider con = new WebContainerInjectionProvider();

	/**
	 * Custom guice injector that will load our modules.
	 */
	private static Injector injector;

	private final List<Module> modules;
	
    protected GuiceInjectionProvider(List<Module> modules) {
    	this.modules = modules;
    }
 
    @Override
    public void inject( Object managedBean ) throws InjectionProviderException
    {
        // allow the default injector to inject the bean.
        con.inject( managedBean );
        // then inject with the google injector.
        getInjector().injectMembers( managedBean );
    }
 
    private Injector getInjector() {
    	if(injector == null) {
    		injector = Guice.createInjector(modules);
    	}
    	
    	return injector;
	}

	@Override
    public void invokePostConstruct( Object managedBean )
            throws InjectionProviderException
    {
        // don't do anything here for guice, just let the default do its thing
        con.invokePostConstruct( managedBean );
    }
 
    @Override
    public void invokePreDestroy( Object managedBean ) throws
            InjectionProviderException
    {
        con.invokePreDestroy( managedBean );
    }
}
