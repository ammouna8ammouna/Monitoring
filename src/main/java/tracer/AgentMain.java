package tracer;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class AgentMain {
  
	/**
	 * Installs the transformation service
	 * @param agentArgs None supported
	 * @param inst The instrumentation instance
	 * @throws Exception thrown on any error
	 */
    private static Logger logger = Logger.getLogger(String.valueOf(AgentInstaller.class));


	public static void agentmain (String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("Begin agentmain...");
        TransformerService ts = new TransformerService(inst);
        ObjectName on = new ObjectName("transformer:service=tracer.DemoTransformer");
        // Could be a different MBeanServer. If so, pass a JMX Default Domain Name in agentArgs
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        server.registerMBean(ts, on);
        // Set this property so the installer knows we're already here
        System.setProperty("demo.agent.installed", "true");
        System.out.println("tracer.AgentMain Installed");
        System.out.println("End agentmain...");
    }
}
