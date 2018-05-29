package com.vm.shadowsocks.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EfficiencyTest {
    private String[] methods = {"rc4-md5",
            "aes-128-cfb", "aes-192-cfb", "aes-256-cfb",
            "aes-128-ctr", "aes-192-ctr", "aes-256-ctr",    // 不支持
            "bf-cfb",
            "camellia-128-cfb", "camellia-192-cfb", "camellia-256-cfb",
            "salsa20",      // 不支持
            "chacha20"};

    // url format: ss://method:password@host:port

    public void runTest() {
        ExecutorService service = Executors.newSingleThreadExecutor();

        for (String method : methods) {
            try {
                Thread thread = new EfficiencyTestThread(method);
                service.submit(thread);

                Thread.sleep(1000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Finish test for " + method);
        }
    }
}