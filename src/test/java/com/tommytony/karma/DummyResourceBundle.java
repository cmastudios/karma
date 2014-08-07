package com.tommytony.karma;

import java.util.ListResourceBundle;

/**
* Created because PowerMockito sucks
*/
class DummyResourceBundle extends ListResourceBundle {

    private Object[][] contents = new Object[][]{
        {"WELCOME", "welcome message"},
        {"PREFIX", "karma: "}
    };

    public DummyResourceBundle() {
    }

    protected Object[][] getContents() {
        return contents;
    }
}
