package com.stephenfairchild.jmx.mbean.dump;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Timer;

final class Main {

    public static void main(String[] args) throws IOException, URISyntaxException {
        new MBeanTimerTask().run();
    }

    @SuppressWarnings("unused")
    public static void premain(String args) {
        System.out.println("Initializing PreMain of java-jmx-mbean-dump");
        new Timer(true).scheduleAtFixedRate(new MBeanTimerTask(), 0L, MBeanTimerTask.getPeriodInMillis());
    }
}
