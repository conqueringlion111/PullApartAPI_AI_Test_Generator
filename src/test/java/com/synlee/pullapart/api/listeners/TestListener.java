package com.pullapart.api.listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TestListener implements ITestListener {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String now() {
        return sdf.format(new Date());
    }

    @Override
    public void onStart(ITestContext context) {
        System.out.println("\n====================================================");
        System.out.println("TEST SUITE STARTED: " + context.getSuite().getName());
        System.out.println("START TIME: " + now());
        System.out.println("====================================================\n");
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("\n====================================================");
        System.out.println("TEST SUITE FINISHED: " + context.getSuite().getName());
        System.out.println("END TIME: " + now());
        System.out.println("====================================================\n");
    }

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("----------------------------------------------------");
        System.out.println("TEST STARTED: " + result.getMethod().getMethodName());
        System.out.println("START TIME: " + now());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("TEST PASSED: " + result.getMethod().getMethodName());
        System.out.println("DURATION: " + (result.getEndMillis() - result.getStartMillis()) + " ms");
        System.out.println("----------------------------------------------------\n");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("TEST FAILED: " + result.getMethod().getMethodName());
        System.out.println("FAILURE REASON: " + result.getThrowable());
        System.out.println("DURATION: " + (result.getEndMillis() - result.getStartMillis()) + " ms");
        System.out.println("----------------------------------------------------\n");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("TEST SKIPPED: " + result.getMethod().getMethodName());
        System.out.println("REASON: " + result.getThrowable());
        System.out.println("----------------------------------------------------\n");
    }
}
