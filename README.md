# Project initialized using:
```
mvn archetype:generate \
    -DgroupId=com.yourname.framework \
    -DartifactId=web-framework \
    -DarchetypeArtifactId=maven-archetype-webapp \
    -DinteractiveMode=false
```

## What does that command do?
```
mvn archetype:generate
```
- Run maven
- Create new project from a template
```
-DgroupId=com.yourname.framework
```
- Creates package structure: src/main/java/com/yourname/framework/
```
-DartifactId=web-framework
```
- Creates the root/project folder: web-framework/
```
-DarchetypeArtifactId=maven-archetype-webapp
```
- Specify which template to use
- Other examples include:
    - ```maven-archetype-quickstart``` = basic Java project
    - ```maven-archetype-simple``` = minimal project
- Useful commands regarding templates:
    - ```mvn archetype:generate```  = Shows list of available archetypes
    - ```mvn archetype:crawl``` = Updates local archetype catalog
```
-DinteractiveMode=false
```
- Skip Maven Q&A session
- Without this, Maven would prompt you for each value (groupId, artifactId)

# Compile and deploy to Tomcat
```
mvn clean package
```