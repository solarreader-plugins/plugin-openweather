/*
 * Copyright (c) 2024-2025 Stefan Toengi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.schnippsche.solarreader.plugins.openweather;

import de.schnippsche.solarreader.backend.calculator.MapCalculator;
import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.connection.network.HttpConnection;
import de.schnippsche.solarreader.backend.connection.network.HttpConnectionFactory;
import de.schnippsche.solarreader.backend.protocol.KnownProtocol;
import de.schnippsche.solarreader.backend.provider.AbstractHttpProvider;
import de.schnippsche.solarreader.backend.provider.CommandProviderProperty;
import de.schnippsche.solarreader.backend.provider.ProviderProperty;
import de.schnippsche.solarreader.backend.provider.SupportedInterface;
import de.schnippsche.solarreader.backend.table.Table;
import de.schnippsche.solarreader.backend.util.JsonTools;
import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.backend.util.StringConverter;
import de.schnippsche.solarreader.database.Activity;
import de.schnippsche.solarreader.frontend.ui.HtmlInputType;
import de.schnippsche.solarreader.frontend.ui.HtmlWidth;
import de.schnippsche.solarreader.frontend.ui.UIInputElementBuilder;
import de.schnippsche.solarreader.frontend.ui.UIList;
import de.schnippsche.solarreader.frontend.ui.UITextElementBuilder;
import de.schnippsche.solarreader.plugin.PluginMetadata;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import org.tinylog.Logger;

/**
 * OpenWeather is a provider class that facilitates interaction with the OpenWeather API. It extends
 * {@link AbstractHttpProvider} and implements methods to fetch weather data from OpenWeather's web
 * service, including current weather, forecasts, and other meteorological information.
 *
 * <p>This class includes methods to configure API requests, process responses, and handle errors
 * specific to the OpenWeather service. It supports flexible configuration, allowing users to set
 * API keys, endpoints, and query parameters to retrieve weather information for specified
 * locations.
 */
@PluginMetadata(
    name = "OpenWeather",
    version = "1.0.1",
    author = "Stefan Töngi",
    url = "https://github.com/solarreader-plugins/plugin-OpenWeather",
    svgImage = "openweather.svg",
    supportedInterfaces = {SupportedInterface.URL},
    usedProtocol = KnownProtocol.HTTP,
    supports = "OpenWeather V2.5")
public class OpenWeather extends AbstractHttpProvider {
  private static final String LOCATION = "location";
  private static final String APPID = "appid";
  private static final String BASE_URL =
      "http://{provider_host}/data/2.5/weather?id={location}&APPID={appid}&lang=de&units=metric";

  /**
   * Constructs a new instance of the {@link OpenWeather} class using the default HTTP connection
   * factory. This constructor is a shortcut that initializes the object with the default
   * configuration for HTTP connections. It internally calls the constructor that accepts a {@link
   * ConnectionFactory} to create an HTTP connection.
   */
  public OpenWeather() {
    this(new HttpConnectionFactory());
  }

  /**
   * Constructs a new instance of the {@link OpenWeather} class with a custom {@link
   * ConnectionFactory} for managing HTTP connections. This constructor allows for more control over
   * how HTTP connections are created, which can be useful for using custom connection settings or
   * libraries.
   *
   * @param connectionFactory the {@link ConnectionFactory} to use for creating HTTP connections
   */
  public OpenWeather(ConnectionFactory<HttpConnection> connectionFactory) {
    super(connectionFactory);
    Logger.debug("instantiate {}", this.getClass().getName());
  }

  /**
   * Retrieves the resource bundle for the plugin based on the specified locale.
   *
   * <p>This method overrides the default implementation to return a {@link ResourceBundle} for the
   * plugin using the provided locale.
   *
   * @return The {@link ResourceBundle} for the plugin, localized according to the specified locale.
   */
  @Override
  public ResourceBundle getPluginResourceBundle() {
    return ResourceBundle.getBundle("openweather", locale);
  }

  @Override
  public Activity getDefaultActivity() {
    return new Activity(LocalTime.of(1, 0, 0), LocalTime.of(18, 0, 0), 1, TimeUnit.HOURS);
  }

  @Override
  public Optional<UIList> getProviderDialog() {
    UIList uiList = new UIList();
    uiList.addElement(
        new UITextElementBuilder()
            .withLabel(resourceBundle.getString("openweather.title.text"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withId("id-appid")
            .withRequired(true)
            .withType(HtmlInputType.TEXT)
            .withPattern("[A-Za-z0-9]{32}")
            .withColumnWidth(HtmlWidth.HALF)
            .withLabel(resourceBundle.getString("openweather.appid.text"))
            .withName(APPID)
            .withPlaceholder(resourceBundle.getString("openweather.appid.text"))
            .withTooltip(resourceBundle.getString("openweather.appid.tooltip"))
            .withInvalidFeedback(resourceBundle.getString("openweather.appid.error"))
            .build());

    uiList.addElement(
        new UIInputElementBuilder()
            .withId("id-locationid")
            .withRequired(true)
            .withType(HtmlInputType.TEXT)
            .withPattern("\\d{7}")
            .withColumnWidth(HtmlWidth.HALF)
            .withLabel(resourceBundle.getString("openweather.location.text"))
            .withName(LOCATION)
            .withPlaceholder(resourceBundle.getString("openweather.location.text"))
            .withTooltip(resourceBundle.getString("openweather.location.tooltip"))
            .withInvalidFeedback(resourceBundle.getString("openweather.location.error"))
            .build());

    return Optional.of(uiList);
  }

  @Override
  public Optional<List<ProviderProperty>> getSupportedProperties() {
    return getSupportedPropertiesFromFile("openweather_fields.json");
  }

  @Override
  public Optional<List<Table>> getDefaultTables() {
    return getDefaultTablesFromFile("openweather_tables.json");
  }

  @Override
  public Setting getDefaultProviderSetting() {
    Setting setting = new Setting();
    setting.setProviderHost("api.openweathermap.org");
    setting.setReadTimeoutMilliseconds(5000);
    return setting;
  }

  @Override
  public String testProviderConnection(Setting testSetting)
      throws IOException, InterruptedException {
    HttpConnection connection = connectionFactory.createConnection(testSetting);
    URL testUrl = getApiUrl(testSetting, BASE_URL);
    connection.test(testUrl, HttpConnection.CONTENT_TYPE_JSON);
    return resourceBundle.getString("openweather.connection.successful");
  }

  @Override
  public void doOnFirstRun() {
    doStandardFirstRun();
  }

  @Override
  public boolean doActivityWork(Map<String, Object> variables)
      throws IOException, InterruptedException {
    workProperties(getConnection(), variables);
    return true;
  }

  private URL getApiUrl(Setting setting, String urlPattern) throws IOException {

    Map<String, String> configurationValues = new HashMap<>();
    configurationValues.put(LOCATION, setting.getConfigurationValueAsString(LOCATION));
    configurationValues.put(APPID, setting.getConfigurationValueAsString(APPID));
    configurationValues.put(Setting.PROVIDER_HOST, setting.getProviderHost());
    configurationValues.put(Setting.PROVIDER_PORT, String.valueOf(setting.getProviderPort()));
    // http://api.openweathermap.org/data/2.5/weather?id={location}&APPID={appid}&lang=de&units=metric
    String urlString =
        new StringConverter(urlPattern).replaceNamedPlaceholders(configurationValues);
    Logger.debug("url:{}", urlString);
    return new StringConverter(urlString).toUrl();
  }

  @Override
  protected void handleCommandProperty(
      HttpConnection httpConnection,
      CommandProviderProperty commandProviderProperty,
      Map<String, Object> variables)
      throws IOException, InterruptedException {
    String pattern = commandProviderProperty.getCommand();
    URL url = getApiUrl(providerData.getSetting(), pattern);
    Map<String, Object> values =
        new JsonTools().getSimpleMapFromJsonString(httpConnection.getAsString(url));
    new MapCalculator()
        .calculate(values, commandProviderProperty.getPropertyFieldList(), variables);
  }
}
