# Bassoon
Standalone web framework for bootstrapping JSF applications into a runnable jar

## Usage

Include the Maven dependency
```
<dependency>
	<groupId>io.timparsons</groupId>
	<artifactId>bassoon</artifactId>
	<version>0.0.1</version>
</dependency>
```

And have it build a fat jar using the maven-shade plugin

```
<build>
	<plugins>
		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-shade-plugin</artifactId>
			<version>2.3</version>
			<configuration>
				<createDependencyReducedPom>true</createDependencyReducedPom>
				<filters>
					<filter>
						<artifact>*:*</artifact>
						<excludes>
							<exclude>META-INF/*.SF</exclude>
							<exclude>META-INF/*.DSA</exclude>
							<exclude>META-INF/*.RSA</exclude>
						</excludes>
					</filter>
				</filters>
			</configuration>
			<executions>
				<execution>
					<phase>package</phase>
					<goals>
						<goal>shade</goal>
					</goals>
					<configuration>
						<transformers>
							<transformer
								implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer" />
							<transformer
								implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
								<mainClass>package.of.my.App</mainClass>
							</transformer>
						</transformers>
					</configuration>
				</execution>
			</executions>
		</plugin>
	</plugins>
</build>
```

Then configure your app.  You will need to create your app's entry point via a class with a `main` method

For example:

```Java
import io.timparsons.bassoon.Application;
import io.timparsons.bassoon.Configuration;

public class App extends Application<Configuration> {

	public static void main(String[] args) {
		new App().run(args);
	}

	@Override
	protected void run(Configuration config) {
    //anything else your app needs to do on startup
	}
}
```

### Configuration

Base configuration is held in a YAML file:

```YAML
host: localhost #optional
port: 5050
idleTimeout: 5000 #optional - default 5000ms
path: /
projectStage: Production #optional - default 'Production', other option is 'Development'
initParams: #optional - map of JSF context-params
  <key>: <value>
```

You can include your own configuration parameters by extending the `Configuration` class, and setting that as the generic type of your application class.

For example:

```Java
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MyConfiguration {
	@JsonProperty("myStringProperty")
	private String myStringProperty;
	
	@NotNull
	@JsonProperty("myRequiredInt")
	private int myRequiredInt;
	
	public String getMyStringProperty() {
	  return myStringProperty;
	}
	
	public void setMyStringProperty(String myStringProperty) {
	  this.myStringProperty = myStringProperty;
	}
	
	public int getMyRequiredInt() {
	  return myRequiredInt;
	}
	
	public void setMyRequiredInt(int myRequiredInt) {
	  this.myRequiredInt = myRequiredInt;
	}
}
```

### Guice Integration

If you would like to use [Google Guice](https://github.com/google/guice), then you will need to create an extension of `GuiceInjectionProvider`.

For example:

```Java
...
import io.timparsons.bassoon.GuiceInjectionProvider;
...

public class InjectionProvider extends GuiceInjectionProvider {

	public InjectionProvider() {
	}

	@Override
	protected List<Module> getModules() {
		// return your list of modules
	}

}
```
