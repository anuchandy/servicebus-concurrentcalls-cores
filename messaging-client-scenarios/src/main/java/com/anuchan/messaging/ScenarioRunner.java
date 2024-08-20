package com.anuchan.messaging;

import com.anuchan.messaging.scenarios.RunScenario;
import com.anuchan.messaging.util.CmdlineArgs;
import com.anuchan.messaging.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class ScenarioRunner implements ApplicationRunner {

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected CmdlineArgs cmdlineArgs;

    public static void main(String[] args) {
        SpringApplication.run(ScenarioRunner.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String scenarioName = cmdlineArgs.get(Constants.SCENARIO_NAME);
        if (scenarioName != null) {
            scenarioName = "com.anuchan.messaging.scenarios." + scenarioName;
        }
        RunScenario scenario = (RunScenario) applicationContext.getBean(Class.forName(scenarioName));
        scenario.run();
    }
}
