/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package top.marchand.xml.sxmsgslf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringJoiner;
import javax.xml.transform.SourceLocator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MessageListetener that logs to slf4J.
 * Logger name is extracted from XSL location (systemId), and trimed to eliminates
 * everything that's before rootPackage.
 * 
 * If a <tt>{@link #PROPERTY_FILE_NAME}</tt> file exists at classpath root,
 * and if if contains a property named <tt>{@link #PROPERTY_ENTRY}</tt>, its value is used as rootPackage.<br>
 * rootPackage is overriden via system property {@link #ROOT_PACKAGE_SYSTEM_PROPERTY}, if defined.
 * @author cmarchand
 */
public class MessageListener implements net.sf.saxon.s9api.MessageListener {
    public static final transient String ROOT_PACKAGE_SYSTEM_PROPERTY = "top.marchand.xml.sxmsgslf.RootPackage";
    public static final transient String PROPERTY_FILE_NAME = "top.marchand.xml.sxmsgslf.message-listener.properties";
    public static final transient String PROPERTY_ENTRY = "root.package";
    private final String rootPackage;

    /**
     * Default constructor.
     * It initialize the root package from {@link #ROOT_PACKAGE_SYSTEM_PROPERTY} System property
     * Once initialized, this property can be reset
     */
    public MessageListener() {
        super();
        String _root = System.getProperty(ROOT_PACKAGE_SYSTEM_PROPERTY);
        String ret;
        if(_root!=null && _root.length()>0) {
            ret = _root;
        } else {
            try(InputStream is = this.getClass().getResourceAsStream(PROPERTY_FILE_NAME)) {
                if(is!=null) {
                    Properties props = new Properties();
                    props.load(is);
                    ret = props.getProperty(PROPERTY_ENTRY);
                } else {
                    ret = null;
                }
            } catch(IOException ex) {
                ret = null;
            }
        }
        rootPackage = ret;
    }
    @Override
    public void message(final XdmNode content, final boolean terminate, final SourceLocator sl) {
        String xslName = sl.getPublicId();
        String loggerName = getLoggerName(xslName);
        Logger logger = LoggerFactory.getLogger(loggerName);
        XdmNode xdmMessage = (XdmNode) content.axisIterator(Axis.CHILD).next();
        String textMessage = xdmMessage.getStringValue();
        XdmNodeKind kind = xdmMessage.getNodeKind();
        if (kind.equals(XdmNodeKind.TEXT)) {
             logger.info((terminate?"[TERMINATE] ":"")+textMessage);
        } else {
            String level = xdmMessage.getAttributeValue(new QName("level"));
            switch (level) {
                case "trace":
                    logger.trace((terminate?"[TERMINATE] ":"")+textMessage);
                    break;
                case "debug":
                    logger.debug((terminate?"[TERMINATE] ":"")+textMessage);
                    break;
                case "info":
                    logger.info((terminate?"[TERMINATE] ":"")+textMessage);
                    break;
                case "warn":
                    logger.warn((terminate?"[TERMINATE] ":"")+textMessage);
                    break;
                case "error":
                    logger.error((terminate?"[TERMINATE] ":"")+textMessage);
                    break;
                default:
                    break;
            }
        }
    }
    
    private String getLoggerName(final String xslName) {
        String[] pathElements = xslName.split("/");
        StringJoiner joiner = new StringJoiner(".");
        for(String s:pathElements) {
            if(s!=null && s.length()>0) joiner.add(s);
        }
        String completePath = joiner.toString();
        if(rootPackage!=null) {
            int pos = completePath.indexOf(rootPackage);
            if(pos>=0) return completePath.substring(pos);
            else return completePath;
        } else return completePath;
    }
    
}
