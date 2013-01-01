package com.bluesmoke.farm.exception;

public class InvalidTimeStampException extends Exception {

    @Override
    public void printStackTrace()
    {
        System.err.println("The timestamps do not coincide");
    }
}
