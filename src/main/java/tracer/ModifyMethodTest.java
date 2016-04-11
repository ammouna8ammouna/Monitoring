package tracer;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

public class ModifyMethodTest {
    private static Logger logger = Logger.getLogger(String.valueOf(AgentInstaller.class));
	/**
	 * Creates a new tracer.ModifyMethodTest
	 * @param className The internal form class name to modify
	 * @param methodName  The name of the method to transform
	 * @param methodSignature A regular expression to match the method signature. (if null, matches ".*")
	 * @param classLoader The intrumentation provided classloader
	 * @param byteCode The pre-transform byte code  
	 * @return  the modified byte code if successful, otherwise returns the original unmodified byte code
	 */
	public static byte[] instrument(String className, String methodName, String methodSignature, ClassLoader classLoader, byte[] byteCode) {
        logger.info("---------Call tracer.ModifyMethodTest---------------");
		String binName  = className.replace('/', '.');

		try {
            logger.info("---------Start tracer.ModifyMethodTest---------------");
			ClassPool cPool = new ClassPool(true);
            cPool.appendClassPath(new LoaderClassPath(classLoader));
            cPool.appendClassPath(new ByteArrayClassPath(binName, byteCode));
            CtClass ctClazz = cPool.get(binName);
            Pattern sigPattern = Pattern.compile((methodSignature == null || methodSignature.trim().isEmpty()) ? ".*" : methodSignature);

            int modifies = 0;
            for(CtMethod method: ctClazz.getDeclaredMethods()) {
                logger.info("notre methode:  "+method.getName());
              //  if(method.getName().equals(methodName)) {
                    System.out.println("SIGNATURE: "+method.getSignature());
                    if(sigPattern.matcher(method.getSignature()).matches()) {
                        logger.info("method name iiiiiiiiiiiiis:::> "+method.getName());
                        ctClazz.removeMethod(method);
                        String newCode = "System.out.println(\"\\n\\t-->Invoked method [" + binName + "." + method.getName() + "(" + method.getSignature().toString()+ ")]\");";
                        System.out.println("[tracer.ModifyMethodTest] Adding [" + newCode + "]");
                        method.insertBefore(newCode);
                        ctClazz.addMethod(method);
                        modifies++;
                    }
                }
           // }
			
			System.out.println("[tracer.ModifyMethodTest] Intrumented [" + modifies + "] methods");
			return ctClazz.toBytecode();
		} catch (Exception ex) {
			System.err.println("Failed to compile retransform class [" + binName + "] Stack trace follows...");
			ex.printStackTrace(System.err);
			return byteCode; 
		}
	}

}
