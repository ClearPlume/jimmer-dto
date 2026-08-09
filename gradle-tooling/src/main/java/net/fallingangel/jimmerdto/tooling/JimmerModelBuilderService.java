package net.fallingangel.jimmerdto.tooling;

import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class JimmerModelBuilderService implements ModelBuilderService {
    @Override
    public boolean canBuild(String modelName) {
        return JimmerBuildModel.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        var options = new HashMap<String, String>();

        project.getTasks().withType(JavaCompile.class).forEach(task -> {
            for (var compilerArg : task.getOptions().getCompilerArgs()) {
                if (compilerArg.startsWith("-A")) {
                    var eq = compilerArg.indexOf('=');
                    if (eq > 2) {
                        options.put(compilerArg.substring(2, eq), compilerArg.substring(eq + 1));
                    }
                }
            }
        });

        var ksp = project.getExtensions().findByName("ksp");
        if (ksp != null) {
            var clazz = ksp.getClass();

            Method getArguments;
            try {
                getArguments = clazz.getMethod("getArguments");
            } catch (NoSuchMethodException e) {
                return new JimmerBuildModelImpl(options);
            }

            Object arguments;
            try {
                arguments = getArguments.invoke(ksp);
            } catch (IllegalAccessException | InvocationTargetException e) {
                return new JimmerBuildModelImpl(options);
            }

            if (!(arguments instanceof Map)) {
                try {
                    Method get = arguments.getClass().getMethod("get");
                    arguments = get.invoke(arguments);
                } catch (Exception e) {
                    return new JimmerBuildModelImpl(options);
                }
            }
            if (arguments instanceof Map<?, ?> mapArguments) {
                mapArguments.forEach((k, v) -> {
                    if (k != null && v != null) {
                        options.put(k.toString(), v.toString());
                    }
                });
            }
        }

        return new JimmerBuildModelImpl(options);
    }
}
