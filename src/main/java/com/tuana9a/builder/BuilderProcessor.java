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
import java.util.Set;
import java.util.stream.Collectors;

// NOTE: can't overwrite class like lombok
@SupportedAnnotationTypes("com.tuana9a.builder.annotations.Builder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class BuilderProcessor extends AbstractProcessor {
    private static class PropNameAndPropType {
        private String propName;
        private String propType;

        public PropNameAndPropType(String propName, String propType) {
            this.propName = propName;
            this.propType = propType;
        }

        public String getPropName() {
            return propName;
        }

        public String getPropType() {
            return propType;
        }

        @Override
        public String toString() {
            return "{" +
                    "propName='" + propName + '\'' +
                    ", propType='" + propType + '\'' +
                    '}';
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)
                    .stream()
                    .filter(element -> element.getKind().isClass())
                    .collect(Collectors.toSet());
            for (Element element : annotatedElements) {
                String className = ((TypeElement) element).getQualifiedName().toString();
                try {
                    List<PropNameAndPropType> propNameAndPropTypes = element.getEnclosedElements()
                            .stream()
                            .filter(prop -> prop.getKind().isField())
                            .map(prop -> new PropNameAndPropType(prop.getSimpleName().toString(), prop.asType().toString()))
                            .collect(Collectors.toList());
                    String result = writeBuilderClass(className, propNameAndPropTypes);
                    System.out.println("[DONE] " + BuilderProcessor.class.getName() + ": " + className + " -> " + result);
                } catch (IOException e) {
                    System.err.print("[FAILED] " + BuilderProcessor.class.getName() + ": " + className + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private String writeBuilderClass(String className, List<PropNameAndPropType> propNameAndPropTypes) throws IOException {
        String packageName = className.substring(0, className.lastIndexOf('.'));
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        String builderClassName = className + "Builder";
        String simpleBuilderClassName = builderClassName.substring(className.lastIndexOf('.') + 1);

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);

        try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {
            writer.println("package " + packageName + ";");
            writer.println();
            writer.println("import java.lang.reflect.Field;");
            writer.println("import java.lang.reflect.Constructor;");
            writer.println("import java.lang.reflect.Parameter;");
            writer.println("import java.util.Map;");
            writer.println("import java.util.HashMap;");
            writer.println("import " + className + ";");
            writer.println();
            writer.println("public class " + simpleBuilderClassName + " {");
            writer.println("    private final Map<String, Object> parts;");
            writer.println();
            writer.println("    public " + simpleClassName + " build() throws IllegalAccessException {");
            writer.println("        Class<" + simpleClassName + "> klass = " + simpleClassName + ".class;");
            writer.println("        " + simpleClassName + " object = new " + simpleClassName + "();"); // TODO: construct by reflection
            writer.println("        Field[] fields = klass.getDeclaredFields();");
            writer.println("        for (Field field: fields) {");
            writer.println("            String fieldName = field.getName();");
            writer.println("            Object value = this.parts.get(fieldName);");
            writer.println("            field.setAccessible(true);");
            writer.println("            field.set(object, value);");
            writer.println(("       }"));
            writer.println(("       return object;"));
            writer.println(("   }"));
            writer.println();
            writer.println("    public " + simpleBuilderClassName + "() {");
            writer.println("        this.parts = new HashMap<>();");
            writer.println(("   }"));
            writer.println();
            for (PropNameAndPropType x : propNameAndPropTypes) {
                String propName = x.getPropName();
                String propType = x.getPropType();
                writer.println("    public " + simpleBuilderClassName + " " + propName + "(" + propType + " value) {");
                writer.println("        this.parts.put(\"" + propName + "\", value);");
                writer.println("        return this;");
                writer.println("    }");
                writer.println();
            }
            writer.println("}");
        }

        return builderClassName;
    }

}
