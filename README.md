# b2-bundle-utils

Utilities for using Building Block language pack bundles in Java and 
JavaScript.

## Building

The build tool used is called [Gradle](http://www.gradle.org/). It 
will do all the work to build the building block, including downloading 
dependencies, compiling java classes and zipping up the JAR. This is all
done with a single command - no installation necessary. From the root of
the project execute the following command:

**Windows**: `gradlew build`

**GNU/Linux & Mac OSX**: `./gradlew build`

The built file will be output to the 
`build/libs/atd-b2-bundle-utils-[version].jar` location.

## Cleaning

If you want to clean the build artifacts execute this command:

**Windows**: `gradlew clean`

**GNU/Linux & Mac OSX**: `./gradlew clean`

## Using this Library

 -  Add the JAR as a dependency in your project. The steps to do this
will vary depending upon your build tool. In gradle, this can be done
by adding the following line to the dependencies: 

`compile files ('path/to/jar/b2-bundle-utils-[version].jar)`

 - Configure the servlet in web.xml:
 
```xml
<servlet>
    <servlet-name>JsBundleServlet</servlet-name>
    <servlet-class>com.alltheducks.bundleutils.JsBundleServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>JsBundleServlet</servlet-name>
    <url-pattern>/bundle.js</url-pattern>
</servlet-mapping>
```

For the following two examples, the language bundle file looks something
like this:
```
plugin.title=Example Building Block
plugin.description=My awesome building block
...
app.welcome.message=Hi, {0}! Welcome to my amazing building block!
...
```

### In Java

This library exposes the `BundleService` class for interacting with 
Blackboard language bundles. To construct one of these objects, the 
vendor id and the handle must be passed as arguments. The 
`getLocalisationString` method retrieves the values stored in the 
language bundle by looking up the key. This method takes one or more 
arguments; the first argument is the key of the language bundle item, 
and any number more arguments can be used to substitute values.

```java
package com.alltheducks.example;

import com.alltheducks.bundleutils.BundleService;

public class Example {

    private final BundleService bundleService = new BundleService("atd", "example");

    // Simply return the name of the building block
    public String getPluginTitle() {
        // plugin.name=Example Building Block
        return bundleService.getLocalisationString("plugin.title");
    }

    // Demonstrates substitutions
    public String createWelcomeMessage(String name) {
        // app.welcome.message=Hi, {0}! Welcome to my amazing building block!
        // the {0} will be substituted with the `name` variable
        return bundleService.getLocalisationString("app.welcome.message", name);
    }

}
```

### In JavaScript

In this example we have two elements that language bundle strings will 
be injected into. By including the JavaScript file that was specified 
in the servlet mapping in the web.xml file, we get access to the 
atd.bundles object. Like when we construct the object in Java, we need 
to look up the keys for our language bundle by providing a vendor id and
handle. In this instance, the vendor id is "atd" and the handle is 
"example". This object provides a method very similar to the Java one 
which can get simple localisation strings by key and to do 
substitutions.

```javascript
<!DOCTYPE html>
<html>
    <head>
        <script type="text/javascript" src="bundle.js"></script>
        <title>Example</title>
    </head>
    <body>
        <h1 id="title"></h1>
        <div id="welcome"></div>
        <script>
            new function() {
                // the exposed bundles object has a function to get localisation keys
                var getLocalisationString = atd.bundles['atd-example'].getString;
                
                // get the dom elements we want to inject the bundle values into
                var titleEl = document.getElementById("title");
                var welcomeEl = document.getElementById("welcome");
                
                // can be used for simple localisation strings
                titleEl.innerHTML = getLocalisationString("plugin.name");
                
                // and also for strings that contain substitutions
                welcomeEl.innerHTML = getLocalisationString("app.welcome.message", "Fred");
            }();
        </script>
    </body>
</html>
```




