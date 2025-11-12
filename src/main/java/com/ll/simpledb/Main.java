package com.ll.simpledb;

import com.ll.Article;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


public class Main {

    public static void main(String[] args) throws InterruptedException {
        LOGGER.mode = true;
        SimpleDb simpleDb = new SimpleDb("localhost", "root", "root", "simpleDb__test");
        simpleDb.run("DROP TABLE IF EXISTS article");

        simpleDb.run("""
                CREATE TABLE article (
                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY(id),
                    createdDate DATETIME NOT NULL,
                    modifiedDate DATETIME NOT NULL,
                    title VARCHAR(100) NOT NULL,
                    `body` TEXT NOT NULL,
                    isBlind BIT(1) NOT NULL DEFAULT 0
                )
                """);

        AtomicInteger cnt = new AtomicInteger(0);
        IntStream.rangeClosed(1, 6).forEach(no -> {
            boolean isBlind = no > 3;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            simpleDb.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });

        // 쓰레드 풀의 크기를 정의합니다.
        int numberOfThreads = 40;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        Runnable task = () -> {
            try {
                LOGGER.debug("thread [" + cnt.addAndGet(1) + "] run");
                Sql sql = simpleDb.genSql();

                sql.append("SELECT * FROM article WHERE id = 1");

                sql.selectRow(Article.class);

            } finally {
                simpleDb.close();
                latch.countDown();
            }
        };

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(task);
        }

        latch.await();
        executorService.shutdown();
    }
}
