package tracer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import soliam.Person;
import soliam.Person1;

public class AgentInstaller {

    /**
     * The created agent jar file name
     */
    protected static final AtomicReference<String> agentJar = new AtomicReference<String>(null);
    protected final static Instrumentation instrumentation = null;
    public static TransformerService tr = new TransformerService(instrumentation);
    private static Logger logger = Logger.getLogger(String.valueOf(AgentInstaller.class));


    /**
     * Self installs the agent, then runs a person sayHello in a loop
     *
     * @param args None
     */

    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {

        String[] st = null;

        // JVM's PID
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        logger.info("PID::: " + pid);


        List< VirtualMachineDescriptor > vms = VirtualMachine.list();
        logger.info("numer of available jvm::> "+vms.size());
        for (VirtualMachineDescriptor virtualMachineDescriptor : vms) {
            logger.info("============ Show JVM: pid = "+ virtualMachineDescriptor.id()+ "============ Show JVM: name = "+virtualMachineDescriptor.displayName());

        }


        // Attach (to ourselves)
        VirtualMachine vm = VirtualMachine.attach(pid);

        // Create an agent jar
        String fileName = createAgent();
        // Load the agent into this JVM
        vm.loadAgent(fileName);
        logger.info("Agent Loaded");
        // Check if connector is installed
        String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
        logger.info("try to connect...");
        if (connectorAddress == null) {
            logger.info("connectorAddress==null");
            // It's not, so install the management agent
            String javaHome = vm.getSystemProperties().getProperty("java.home");
            logger.info("javaHome " + javaHome);
            File managementAgentJarFile = new File(javaHome + File.separator + "lib" + File.separator + "management-agent.jar");
            vm.loadAgent(managementAgentJarFile.getAbsolutePath());
            logger.info("getAbsolutePath "+managementAgentJarFile.getAbsolutePath());
            connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
            logger.info("localConnectorAddress " + connectorAddress);
            // Now it's installed
        }

        JMXConnector connector = null;
        try {
            // This is the ObjectName of the MBean registered when loaded.jar was installed.
            ObjectName on = new ObjectName("transformer:service=tracer.DemoTransformer");
            logger.info("ObjectName= " + on);
            // Here we're connecting to the target JVM through the management agent
            connector = JMXConnectorFactory.connect(new JMXServiceURL(connectorAddress));
            logger.info("connector=  " + connector);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            logger.info("server=  " + server.getMBeanInfo(on));
            logger.info("getConnectionId :  "+connector.getConnectionId());

            Object opParams[] = {"tracer.Person",
                  "",
                  ""
            };

            String opSig[] = {
                  String.class.getName(),
                  String.class.getName(),
                  String.class.getName()
            };

            MBeanOperationInfo[] op = server.getMBeanInfo(on).getOperations();

            System.out.println("op de i . get name "+op[0].getName());

            MBeanParameterInfo[] signature = op[0].getSignature();
            //tr.transformClass("soliam.Person","sayHello","");
            server.invoke(on, op[0].getName(), opParams, opSig);
            // Run sayHello in a loop
            //test t1 = new test();
            Person person = new Person();
            Person1 person1 = new Person1();
            person.affiche1();
            for (int i = 0; i < 1000; i++) {
                person.sayHello(i);
                person.sayHello("" + (i * -1));
                Thread.currentThread().join(5000);
            }

        } catch (Exception ex) {
            System.err.println("Agent Installation Failed. Stack trace follows...");
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Creates the temporary agent jar file if it has not been created
     *
     * @return The created agent file name
     */
    public static String createAgent() {
        System.out.println("--------createAgent------------");
        if (agentJar.get() == null) {
            synchronized (agentJar) {
                if (agentJar.get() == null) {
                    FileOutputStream fos = null;
                    JarOutputStream jos = null;
                    try {

                        File tmpFile = File.createTempFile(AgentMain.class.getName(), ".jar");
                        System.out.println("Temp File:" + tmpFile.getAbsolutePath());
                        tmpFile.deleteOnExit();
                        StringBuilder manifest = new StringBuilder();
                        manifest.append("Manifest-Version: 1.0\nAgent-Class: " + AgentMain.class.getName() + "\n");
                        manifest.append("Can-Redefine-Classes: true\n");
                        manifest.append("Can-Retransform-Classes: true\n");
                        manifest.append("Premain-Class: " + AgentMain.class.getName() + "\n");
                        ByteArrayInputStream bais = new ByteArrayInputStream(manifest.toString().getBytes());
                        Manifest mf = new Manifest(bais);
                        fos = new FileOutputStream(tmpFile, false);
                        jos = new JarOutputStream(fos, mf);
                        addClassesToJar(jos, AgentMain.class, DemoTransformer.class, ModifyMethodTest.class, TransformerService.class, TransformerServiceMBean.class);
                        jos.flush();
                        jos.close();
                        fos.flush();
                        fos.close();
                        agentJar.set(tmpFile.getAbsolutePath());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to write Agent installer Jar", e);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }
        return agentJar.get();
    }

    /**
     * Writes the passed classes to the passed JarOutputStream
     *
     * @param jos the JarOutputStream
     * @param clazzes The classes to write
     * @throws IOException on an IOException
     */
    protected static void addClassesToJar(JarOutputStream jos, Class<?>... clazzes) throws IOException {
        System.out.println("--------addClassesToJar------------");
        for (Class<?> clazz : clazzes) {
            jos.putNextEntry(new ZipEntry(clazz.getName().replace('.', '/') + ".class"));
            jos.write(getClassBytes(clazz));
            jos.flush();
            jos.closeEntry();
        }
    }

    /**
     * Returns the bytecode bytes for the passed class
     *
     * @param clazz The class to get the bytecode for
     * @return a byte array of bytecode for the passed class
     */
    public static byte[] getClassBytes(Class<?> clazz) {
        System.out.println("-------- Begin getClassBytes------------");
        System.out.println("----------------------------------------");
        InputStream is = null;
        try {
            is = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class");
            System.out.println("clazz.getName()==========" + clazz.getName());
            ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[8092];
            int bytesRead = -1;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            baos.flush();
            System.out.println("-------- End getClassBytes------------");
            System.out.println("----------------------------------------");
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read class bytes for [" + clazz.getName() + "]", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
    }
}




