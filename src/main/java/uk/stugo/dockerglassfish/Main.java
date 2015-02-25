package uk.stugo.dockerglassfish;


import org.glassfish.embeddable.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            // port defaults to 80
            String appPortStr = System.getenv("APP_PORT");
            int appPort = 80;

            if (appPortStr != null) {
                appPort = Integer.parseInt(appPortStr);
            }

            GlassFishProperties properties = new GlassFishProperties();
            properties.setPort("http-listener", appPort);

            // start a GlassFish instance on the specified port
            final GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish(properties);
            glassfish.start();

            // deploy archive specified by APP_PATH
            // otherwise, deploy /app/*.war and /app/*.ear
            Deployer deployer = glassfish.getDeployer();
            String appPath = System.getenv("APP_PATH");

            if (appPath != null) {
                deployer.deploy(new File(appPath));
            } else {
                // get a list of archives in the /app dir
                File[] archives = new File("/app").listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String name) {
                        return name.endsWith(".war") || name.endsWith(".ear");
                    }
                });

                // deploy them
                for (File archive: archives) {
                    deployer.deploy(archive);
                }
            }

            // deploy app(s)
            System.out.println("[embedded-glassfish-docker] application ready");

            // handle SIGTERM gracefully
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    // stop glassfish
                    try {
                        glassfish.stop();
                        glassfish.dispose();
                    } catch (GlassFishException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (GlassFishException e) {
            e.printStackTrace();
        }
    }
}
