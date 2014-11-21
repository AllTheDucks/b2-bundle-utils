package com.alltheducks.bundleutils;

import blackboard.persist.Id;
import blackboard.platform.intl.BundleManagerEx;
import blackboard.platform.intl.BundleManagerExFactory;
import blackboard.platform.plugin.PlugIn;
import blackboard.platform.plugin.PlugInManager;
import blackboard.platform.plugin.PlugInManagerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 * Created by Shane Argo on 21/11/14.
 */
public class JsBundleServlet extends HttpServlet {

    private String vendorId;
    private String handle;
    private Id pluginId;

    @Override
    public void init() throws ServletException {
        vendorId = getServletContext().getInitParameter("blackboard.plugin.vendor");
        if(vendorId == null) {
            throw new ServletException("Must specify vendor id (blackboard.plugin.vendor) in the web.xml");
        }

        handle = getServletContext().getInitParameter("blackboard.plugin.handle");
        if(handle == null) {
            throw new ServletException("Must specify b2 handle (blackboard.plugin.handle) in the web.xml");
        }

        PlugInManager pluginMgr = PlugInManagerFactory.getInstance();
        PlugIn plugin = pluginMgr.getPlugIn(vendorId, handle);

        this.pluginId = plugin.getId();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String limitVal = req.getParameter("limit");
        boolean limit = true;
        if(limitVal == null || limitVal.isEmpty()) {
            limit = false;
        }

        final BundleManagerEx bm = BundleManagerExFactory.getInstance();
        final ResourceBundle bundle = bm.getPluginBundle(pluginId).getResourceBundle();

        String b2 = String.format("atd.bundles['%s-%s']", vendorId, handle);

        StringBuilder sb = new StringBuilder();
        sb.append("if(typeof atd === 'undefined') atd = {};")
                .append("if(typeof atd.bundles === 'undefined') atd.bundles = {};")
                .append(b2)
                .append(" = {};")
                .append(b2)
                .append(".keys = {");
        final Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            if(limit && !key.startsWith(limitVal)) {
                continue;
            }

            sb.append("'")
                    .append(key)
                    .append("': '")
                    .append(StringEscapeUtils.escapeJavaScript(cleanQuotes(bundle.getString(key))))
                    .append("'");
            if(keys.hasMoreElements()) {
                sb.append(",");
            }
        }
        sb.append("};")
                .append(b2)
                .append(".getString = function(key) { var r = ")
                .append(b2)
                .append(".keys[key]; if(r == null) { return key; } for (var i = 1; i < arguments.length; i++) { r = r.replace('{' + (i-1) + '}', arguments[i]); } return r; };");

        String js = sb.toString();
        String respTag = DigestUtils.md5Hex(js);
        resp.setHeader("ETag", respTag);

        String reqTag = req.getHeader("If-None-Match");
        if(reqTag != null && reqTag.equals(respTag)) {
            resp.setStatus(304);
            return;
        }

        resp.setHeader("Content-Type", "application/javascript");
        resp.getWriter().append(sb.toString());
    }

    /**
     * Works around Blackboards strange handling of values with replacements.
     * @param str The language pack value.
     * @return A string minus the double quoting.
     */
    private String cleanQuotes(String str) {
        if(str.contains("{0}")) {
            return str.replace("''", "'");
        } else {
            return str;
        }
    }
}
