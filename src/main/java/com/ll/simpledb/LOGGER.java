package com.ll.simpledb;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LOGGER {

    // debug 모드 on/off
    static boolean mode = false;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ANSI 색상
    private static final String RESET = "\u001B[0m";
    private static final String INFO_COLOR = "\u001B[32m";     // 초록
    private static final String WARN_COLOR = "\u001B[33m";     // 노랑
    private static final String ERROR_COLOR = "\u001B[31m";    // 빨강
    private static final String DEBUG_COLOR = "\u001B[36m";    // 청록

    private static String now() {
        return LocalDateTime.now().format(FMT);
    }

    public static void info(String msg) {
        System.out.println(INFO_COLOR + now() + " [INFO] " + msg + RESET);
    }

    public static void warn(String msg) {
        System.out.println(WARN_COLOR + now() + " [WARN] " + msg + RESET);
    }

    public static void error(String msg) {
        System.out.println(ERROR_COLOR + now() + " [ERROR] " + msg + RESET);
    }

    public static void debug(String msg) {
        if (!mode) return; // debug 비활성화 시 출력 X
        System.out.println(DEBUG_COLOR + now() + " [DEBUG] " + msg + RESET);
    }
}
