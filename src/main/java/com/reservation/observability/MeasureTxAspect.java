package com.reservation.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MeasureTxAspect {

    @Around("@annotation(MeasureTx)")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {

        ExecutionContext context = ExecutionContextHolder.get();

        if (context == null) return joinPoint.proceed();

        long start = System.nanoTime();

        try {
            return joinPoint.proceed();
        } finally {
            long end = System.nanoTime();
            context.addWriteTime(end - start);
        }
    }
}