package com.getting.util;

import javafx.application.Platform;

public abstract class AsyncTask<R> extends Task {

    public void preTaskOnUi() {
    }

    public abstract R runTask();

    public void postTaskOnUi(R result) {
    }

    @Override
    public final void run() {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                preTaskOnUi();
            }

        });

        R result = runTask();

        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                postTaskOnUi(result);
            }

        });
    }

    public AsyncTask(Object id, long delay) {
        super(id, delay);
    }

}