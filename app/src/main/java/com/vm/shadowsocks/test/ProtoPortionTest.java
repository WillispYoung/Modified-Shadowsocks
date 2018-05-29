package com.vm.shadowsocks.test;

public class ProtoPortionTest {
    public static long TcpSent = 0;
    public static long TcpReceived = 0;

    public static long UdpSent = 0;
    public static long UdpReceived = 0;

    public static long IcmpSent = 0;

    public static long HttpSent = 0;
    public static long HttpReceived = 0;

    public static long VideoSent = 0;
    public static long VideoReceived = 0;

    public static long HttpsSent = 0;
    public static long HttpsReceived = 0;

    public static void showResult() {
        System.out.println("TCP " + TcpSent + " " + TcpReceived + "\n" +
                "UDP " + UdpSent + " " + UdpReceived + "\n" +
                "ICMP " + IcmpSent + "\n" +
                "HTTP " + HttpSent + " " + HttpReceived + "\n" +
                "Video " + VideoSent + " " + VideoReceived + "\n" +
                "HTTPS " + HttpsSent + " " + HttpsReceived);
    }


}
