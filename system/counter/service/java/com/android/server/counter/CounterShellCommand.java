package com.android.server.counter;

import android.os.ShellCommand;

import java.io.PrintWriter;

final class CounterShellCommand extends ShellCommand {

    private final CounterService mService;

    CounterShellCommand(CounterService service) {
        mService = service;
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        final PrintWriter pw = getOutPrintWriter();
        switch (cmd) {
            case "get": {
                pw.println(mService.getCount());
                return 0;
            }
            case "inc": {
                int n = Integer.parseInt(getNextArgRequired());
                mService.increment(n);
                pw.println("ok");
                return 0;
            }
            case "reset": {
                mService.reset();
                pw.println("ok");
                return 0;
            }
            case "add": {
                int a = Integer.parseInt(getNextArgRequired());
                int b = Integer.parseInt(getNextArgRequired());
                pw.println(mService.add(a, b));
                return 0;
            }
            default:
                return handleDefaultCommands(cmd);
        }
    }

    @Override
    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Counter service commands:");
        pw.println("  get                       Print current count");
        pw.println("  inc <n>                   Increment by n");
        pw.println("  reset                     Reset count to 0");
        pw.println("  add <a> <b>               Print a + b");
    }
}
