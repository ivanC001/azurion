package com.azurion.saascore.modulos.application.services;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RequireModuleAspect {

    private final ModuleAccessService moduleAccessService;

    @Around("@within(com.azurion.saascore.modulos.application.services.RequireModule) || @annotation(com.azurion.saascore.modulos.application.services.RequireModule)")
    public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
        LinkedHashSet<String> requiredModules = new LinkedHashSet<>();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequireModule methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, RequireModule.class);
        if (methodAnnotation != null) {
            requiredModules.addAll(java.util.List.of(methodAnnotation.value()));
        }

        RequireModule classAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                joinPoint.getTarget().getClass(),
                RequireModule.class
        );
        if (classAnnotation != null) {
            requiredModules.addAll(java.util.List.of(classAnnotation.value()));
        }

        for (String moduleCode : requiredModules) {
            moduleAccessService.requireCurrentTenantModule(moduleCode);
        }

        return joinPoint.proceed();
    }
}
