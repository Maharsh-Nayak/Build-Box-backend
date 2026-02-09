package com.BuildBox.BuildServer.util;

import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CommandRunner {

    public void run(String command) throws Exception {
        CommandExecutor.run(command);
    }
}
