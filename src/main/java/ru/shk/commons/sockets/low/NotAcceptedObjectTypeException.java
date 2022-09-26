package ru.shk.commons.sockets.low;

public class NotAcceptedObjectTypeException extends Exception {
    @Override
    public String getMessage() {
        return "Unrecognized Object type provided in socket message";
    }
}
