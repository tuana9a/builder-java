# builder-java

Simple java builder annotation

# how to use

first install to `.m2` local with

```bash
mvn install
```

then add these config to `pom.xml`

```xml

<project>
    ...
    <!--  Dependencies  -->
    <dependencies>
        <dependency>
            <groupId>com.tuana9a</groupId>
            <artifactId>builder</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
    ...
    <!--  Build configuration  -->
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                    <annotationProcessors>
                        <annotationProcessor>
                            com.tuana9a.builder.BuilderProcessor
                        </annotationProcessor>
                    </annotationProcessors>
                </configuration>
            </plugin>
        </plugins>
    </build>
    ...
</project>
```

then you're good to go