package com.vm.shadowsocks.tunnel;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Random;

public class TaskScheduler {
    private static ArrayList<SelectWorkerThread> threads = new ArrayList<>();
    private static Random random = new Random();
    private static int THREAD_SIZE = 5;
    
    public static void addTask(SocketChannel channel, InetSocketAddress address) {
        try {
            if (threads.size() == 0) {
                for (int i = 0;i < THREAD_SIZE; i ++) {
                    SelectWorkerThread thread = new SelectWorkerThread();
                    threads.add(thread);
                    thread.start();
                }
            }
            
            int index = random.nextInt(THREAD_SIZE);
            threads.get(index).addTask(channel, address);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void stopTasks() {
        for (SelectWorkerThread thread : threads)
            thread.stop();
        threads.clear();
    }
}
