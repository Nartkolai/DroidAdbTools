package com.nartkolai.droidadbtools.Utils;

public class AdbCfg {
    private String cmd;
    private String[] path;

    AdbCfg(String cmd, String[] path){
        this.cmd = cmd;
        this.path = path;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public void setPath(String[] path) {
        this.path = path;
    }

    public String getCmd() {
        return cmd;
    }

    public String[] getPath() {
        return path;
    }
}
