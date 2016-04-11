package tracer;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class TransformerService implements TransformerServiceMBean {
    private static Logger logger = Logger.getLogger(String.valueOf(AgentInstaller.class));
    /**
     * The JVM's instrumentation instance
     */
    protected final Instrumentation instrumentation;

    /**
     * Creates a new tracer.TransformerService
     *
     * @param instrumentation The JVM's instrumentation instance
     */
    public TransformerService(Instrumentation instrumentation) {
        logger.info("---------Call TransformerService----------");
        this.instrumentation = instrumentation;
    }

    @Override
    public void transformClass(String className, String methodName, String methodSignature) {
        logger.info("---------start transformClass----------");
        Class<?> targetClazz = null;
        ClassLoader targetClassLoader = null;
        // first see if we can locate the class through normal means
        try {

            targetClazz = Class.forName(className);
            System.out.println("targetClazz ==W "+targetClazz);

            targetClassLoader = targetClazz.getClassLoader();

            transform(targetClazz, targetClassLoader, methodName, methodSignature);
            return;
        } catch (Exception ex) { /* Nope */ }
        // now try the hard/slow way
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {

            //if (clazz.getName().equals(className)) {
                targetClazz = clazz;
            logger.info("targetClazz ------------------> " + targetClazz);
                targetClassLoader = targetClazz.getClassLoader();
                transform(targetClazz, targetClassLoader, methodName, methodSignature);
                return;
            //}
        }
        throw new RuntimeException("Failed to locate class [" + className + "]");
    }

    /**
     * Registers a transformer and executes the transform
     *
     * @param clazz The class to transform
     * @param classLoader The classloader the class was loaded from
     * @param methodName The method name to instrument
     * @param methodSignature The method signature to match
     */
    protected void transform(Class<?> clazz, ClassLoader classLoader, String methodName, String methodSignature) {
        logger.info("Class name from transformer ---> " + clazz.getName());
        logger.info("method name from transformer ---> " + methodName);
        logger.info("methodSignature name from transformer ---> " + methodSignature);
        DemoTransformer dt = new DemoTransformer(classLoader, clazz.getName(), methodName, methodSignature);
        instrumentation.addTransformer(dt, true);
        try {

            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to transform [" + clazz.getName() + "]", ex);
        } finally {
            instrumentation.removeTransformer(dt);
        }
    }
}
