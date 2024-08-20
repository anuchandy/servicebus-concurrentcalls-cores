package com.anuchan.messaging.scenarios;

import com.anuchan.messaging.util.CmdlineArgs;
import com.azure.core.util.logging.ClientLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Service
public abstract class RunScenario {
    @Autowired
    protected CmdlineArgs cmdlineArgs;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    private void postConstruct() {
    }

    public abstract void run();

    protected boolean blockingWait(ClientLogger logger, Duration duration) {
        if (duration.toMillis() <= 0) {
            return true;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            logger.warning("wait interrupted");
            return false;
        }
        return true;
    }

    protected boolean close(Disposable d) {
        if (d == null) {
            return true;
        }
        try {
            d.dispose();
        } catch (Exception e) {
            return false;
        }
        return true;

    }

    protected boolean close(AutoCloseable c) {
        if (c == null) {
            return true;
        }
        try {
            c.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
