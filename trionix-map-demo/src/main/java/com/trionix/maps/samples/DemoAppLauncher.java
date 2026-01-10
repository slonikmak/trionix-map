package com.trionix.maps.samples;

public class DemoAppLauncher {

    public static void main(String[] args) {
        // Force enable MCP for development/testing
        System.setProperty("mcp.ui", "true");
        System.setProperty("mcp.allowActions", "true");
        System.setProperty("mcp.auth", "false");
        System.setProperty("mcp.port", "55667");

    }
}
