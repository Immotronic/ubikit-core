# ubikit-core

Core library of the [Ubikit](http://www.ubikit.org) project

## Compilation

To build this library, you will need Maven 2 or later. The project POM rely on a parent POM that
depends on a root POM. You will need to download and install these files in appropriate
directory on your developpement machine.

- [Download the Ubikit project parent POM](http://www.ubikit.org/resources/misc/maven/ubikit-project-pom.xml)
- [Download the root POM](http://www.ubikit.org/resources/misc/maven/root-pom.xml)

### Tuning the root POM
At least, you need to adapt the "Distribution Management" section of the root POM to your 
developpement environment.

- If you do not wish to use a personal repository proxy server, simply remove the whole
<distributionManagement> element from root-pom.xml file.
- To use a personal repository procy server instead of Immotronic one, adapt <repository> and
<snapshotRepository> elements to match your configuration.

### Compiling

Compile and generate the project JAR file with following command:

	mvn package