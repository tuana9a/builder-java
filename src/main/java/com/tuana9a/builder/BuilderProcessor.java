package com.tuana9a.builder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// NOTE: can't overwrite class like lombok
@SupportedAnnotationTypes("com.tuana9a.builder.annotations.Builder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class BuilderProcessor extends AbstractProcessor {
    private static final Logger logger = Logger.getLogger(BuilderProcessor.class.getName());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        logger.addHandler(new ConsoleHandler());
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)
                    .stream()
                    .filter(element -> element.getKind().isClass())
                    .collect(Collectors.toSet());
            for (Element element : annotatedElements) {
                String fullClassName = ((TypeElement) element).getQualifiedName().toString();
                try {
                    List<Element> srcPropElements = element.getEnclosedElements()
                            .stream()
                            .filter(prop -> prop.getKind().isField())
                            .collect(Collectors.toList());
                    Map<String, String> srcPropMap = srcPropElements.stream()
                            .collect(Collectors.toMap(
                                    prop -> prop.getSimpleName().toString(),
                                    prop -> prop.asType().toString()
                            ));
                    String result = writeBuilderClass(fullClassName, srcPropMap);
                    logger.info("[DONE] " + BuilderProcessor.class.getName() + " process: " + fullClassName + " output: " + result);
                } catch (IOException e) {
                    logger.warning("[FAILED] " + BuilderProcessor.class.getName() + " process " + fullClassName + " error: " + e.getMessage());
                }
            }
        }
        return true;
    }

    private String writeBuilderClass(String fullClassName, Map<String, String> propsMap) throws IOException {
        String packageName = null;
        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = fullClassName.substring(0, lastDot);
        }

        String className = fullClassName.substring(lastDot + 1);
        String fullBuilderClassName = fullClassName + "Builder";
        String builderClassName = fullBuilderClassName.substring(lastDot + 1);

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(fullBuilderClassName);

        try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                writer.println("package " + packageName + ";\n");
            }

            writer.println("public class " + builderClassName + " {\n");
            writer.println("    private " + className + " object = new " + className + "();\n");

            writer.println("    public " + className + " build() { return object; }\n");

            propsMap.forEach((propName, propType) -> {
                String setMethod = "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
                writer.println("    public " + builderClassName + " " + propName + "(" + propType + " value) {");
                writer.println("        object." + setMethod + "(value);");
                writer.println("        return this;");
                writer.println("    }\n");
            });

            writer.println("}");
        }

        return fullBuilderClassName;
    }

}
