package airbrake;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

/**
 * Created by martinjr on 5/15/17.
 */
@Plugin(name = "AirbrakeAppender", category = "Core", elementType = "appender", printObject = true)
public class AirbrakeAppenderPlugin extends AbstractAppender {

	private final AirbrakeNotifier airbrakeNotifier = new AirbrakeNotifier();
	private Backtrace backtrace = new Backtrace();

    private String api_key, env, url;
    private boolean enabled;

    public static AirbrakeAppenderPlugin create(String apiKey) {
        return new AirbrakeAppenderPlugin("airbrake", apiKey, "test", "http://test", true);
    }

    public static AirbrakeAppenderPlugin create(String apiKey, Backtrace backtrace) {
        AirbrakeAppenderPlugin ret=new AirbrakeAppenderPlugin("airbrake", apiKey, "test", "http://test", true);
        ret.backtrace=backtrace;

        return ret;
    }

    @PluginFactory
    public static AirbrakeAppenderPlugin createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("api_key") @Required(message="No API key specified") String api_key,
            @PluginAttribute("env") String env,
            @PluginAttribute("url") String url,
            @PluginAttribute("enabled") boolean enabled
    ) {
        return new AirbrakeAppenderPlugin(name, api_key, env, url, enabled);
    }

    private AirbrakeAppenderPlugin(String name, String api_key, String env, String url, boolean enabled) {
        super(name,
                ThresholdFilter.createFilter(Level.ERROR, null, null),
                PatternLayout.createDefaultLayout(),
                false);

        this.api_key = api_key;
        this.env = env;
        this.url = url;
        this.enabled = enabled;

        System.out.println("Configured notifier:");
        System.out.printf(api_key);
        System.out.println(env);
        System.out.println(name);
        System.out.println(url);
        System.out.println(enabled);

        airbrakeNotifier.setUrl(url);
    }

    @Override
    public void append(LogEvent logEvent) {
        if (!enabled) return;

        Throwable th=logEvent.getThrown();

        if (th==null) {
            th=logEvent.getMessage().getThrowable();
        }

        if (th==null) {
            th=new Exception(logEvent.getMessage().getFormattedMessage());
        }

        airbrakeNotifier.notify(newNoticeFor(th));
    }

    public AirbrakeNotice newNoticeFor(final Throwable throwable) {
        return new AirbrakeNoticeBuilderUsingFilteredSystemProperties(api_key,
                backtrace, throwable, env).newNotice();
    }
}
