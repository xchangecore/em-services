package com.leidos.xchangecore.core.em.util;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import x0.oasisNamesTcEmergencyEDXLDE1.ValueSchemeType;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;

import com.leidos.xchangecore.core.infrastructure.service.CommunicationsService;

public class BroadcastUtil {

    // JID regex parsing
    private static final Pattern RE_PATTERN = Pattern.compile("(([^@]+)@)?([^/]+)(/(.*))?",
        Pattern.DOTALL);
    private static final int POS_NODE = 2;
    private static final int POS_DOMAIN = 3;
    private static final int POS_RESOURCE = 5;

    public static String getCoreFromJID(String jid) {

        String core = null;
        Matcher match = RE_PATTERN.matcher((jid != null) ? jid : "");

        if (!match.matches())
            return core;

        core = match.group(POS_DOMAIN);

        return core;
    }

    public static HashSet<String> getCoreList(EDXLDistribution edxl) {

        HashSet<String> cores = new HashSet<String>();
        if (edxl.sizeOfExplicitAddressArray() > 0) {
            // Find core name for each explicit address.
            for (ValueSchemeType type : edxl.getExplicitAddressArray()) {
                if (type.getExplicitAddressScheme().equals(CommunicationsService.UICDSExplicitAddressScheme)) {
                    for (String address : type.getExplicitAddressValueArray()) {
                        String core = getCoreFromJID(address);
                        if (core != null) {
                            cores.add(core);
                        }
                    }
                }
            }
        }
        return cores;
    }

    public static HashSet<String> getJidList(EDXLDistribution edxl) {

        HashSet<String> jids = new HashSet<String>();
        if (edxl.sizeOfExplicitAddressArray() > 0) {
            // Find and explicit addresses of type xmpp
            for (ValueSchemeType type : edxl.getExplicitAddressArray()) {
                if (type.getExplicitAddressScheme().equals(CommunicationsService.XMPPAddressScheme)) {
                    for (String address : type.getExplicitAddressValueArray()) {
                        jids.add(address);
                    }
                }
            }
        }
        return jids;
    }
}
