package com.ttt.vivm.connector.real;

final class ShellArgument {

    private ShellArgument() {
    }

    static String quote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
