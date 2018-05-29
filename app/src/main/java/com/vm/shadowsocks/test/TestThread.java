package com.vm.shadowsocks.test;

import com.vm.shadowsocks.tunnel.shadowsocks.CryptFactory;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;
import com.vm.shadowsocks.tunnel.shadowsocks.ShadowsocksConfig;

import java.util.Random;

public class TestThread extends Thread {
    public static char[] allChar = "abcdefghijklmnopqrstuvwxyz1234567890,./;'<>{}[]()!@#$%^&*_+-=".toCharArray();
    public static String test1 = generateRandomString(10000);
    public static String test2 = generateRandomString(50000);
    public static String test3 = generateRandomString(100000);
//    public static String test4 = generateRandomString(200000);    // 安卓上不支持创建

    private ShadowsocksConfig config = null;
    private String method;

    // url format: ss://method:password@host:port

    public TestThread(String method) {
        this.method = method;

        try {
            this.config = ShadowsocksConfig.parse("ss://" + method + ":helloworld@8.8.8.8:88");
        }
        catch (Exception e) {
            System.out.println("Invalid config format");
        }
    }

    @Override
    public void run() {
        if (config == null)  {
            System.out.println("Invalid config, test abandoned");
            return;
        }

        ICrypt encryptor = CryptFactory.get(config.EncryptMethod, config.Password);
        byte[] result;

//         10,000 length
        long start = System.currentTimeMillis();
        result = encryptor.encrypt(test1.getBytes());
        long end = System.currentTimeMillis();
        System.out.println(method + " 10000 " + (end-start) + " " + result.length);

        // 50,000 length
        start = System.currentTimeMillis();
        result = encryptor.encrypt(test2.getBytes());
        end = System.currentTimeMillis();
        System.out.println(method + " 50000 " + (end-start) + " " + result.length);

        // 100,000 length
        start = System.currentTimeMillis();
        result = encryptor.encrypt(test3.getBytes());
        end = System.currentTimeMillis();
        System.out.println(method + " 100000 " + (end-start) + " " + result.length);

//        // 200,000 length
//        start = System.currentTimeMillis();
//        result = encryptor.encrypt(test4.getBytes());
//        end = System.currentTimeMillis();
//        System.out.println(method + " 200000 " + (end-start) + " " + result.length);

    }

    private static String generateRandomString(int length) {
        Random random = new Random(System.currentTimeMillis());
        int bound = allChar.length;
        String res = "";

        for (int i = 0;i < length;i ++)
            res += allChar[random.nextInt(bound)];

        return res;
    }
}
