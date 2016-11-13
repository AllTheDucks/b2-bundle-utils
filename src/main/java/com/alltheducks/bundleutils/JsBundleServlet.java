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

    private String b2;
    private String jsPackage;

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

        String packageStr = getServletContext().getInitParameter("atd.bundle.package");
        if(packageStr == null || packageStr.trim().isEmpty()) {
            packageStr = "atd.bundle";
        }

        this.b2 = String.format("%s['%s-%s']", packageStr, vendorId, handle);

        StringBuilder packageDef = new StringBuilder();
        StringBuilder jsPackageBuilder = new StringBuilder();
        final String[] split = packageStr.split("\\.");
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if(i > 0) {
                packageDef.append(".");
            }
            packageDef.append(s);
            jsPackageBuilder.append("if(typeof ")
                    .append(packageDef)
                    .append(" === 'undefined') ")
                    .append(packageDef)
                    .append(" = {};");
        }
        jsPackageBuilder.append(this.b2).append(" = {};");

        this.jsPackage = jsPackageBuilder.toString();

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

        StringBuilder sb = new StringBuilder();
        sb.append(this.jsPackage)
                .append(this.b2)
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
