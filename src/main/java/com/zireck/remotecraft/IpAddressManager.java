package com.zireck.remotecraft;

import org.apache.http.util.TextUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Zireck on 07/08/16.
 */
public class IpAddressManager {

    public String getIpAddress() {
        String ipAddress = null;

        try {
            ipAddress = findIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return isValidIpAddress(ipAddress) ? ipAddress : null;
    }

    private String findIpAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    private boolean isValidIpAddress(String ipAddress) {
        if (TextUtils.isEmpty(ipAddress)) {
            return false;
        }

        if (ipAddress.contains("127.0.0.1")) {
            return false;
        }

        return true;
    }

}
