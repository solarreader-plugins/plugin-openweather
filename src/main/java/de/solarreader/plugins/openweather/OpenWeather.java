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
package de.solarreader.plugins.openweather;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.tinylog.Logger;

import de.solarreader.core.EnvironmentProfile;
import de.solarreader.core.Result;
import de.solarreader.core.config.Configuration;
import de.solarreader.core.config.HostConfig;
import de.solarreader.core.connection.host.HostConnection;
import de.solarreader.core.connection.host.HostConnectionFactory;
import de.solarreader.core.field.Field;
import de.solarreader.core.field.HostField;
import de.solarreader.core.frontend.ui.HtmlInputType;
import de.solarreader.core.frontend.ui.HtmlWidth;
import de.solarreader.core.frontend.ui.UIInputElementBuilder;
import de.solarreader.core.frontend.ui.UIList;
import de.solarreader.core.frontend.ui.UITextElementBuilder;
import de.solarreader.core.plugin.AbstractHostPlugin;
import de.solarreader.core.table.TableConfiguration;
import de.solarreader.core.util.JsonFlattener;
import de.solarreader.core.util.StringConverter;
import de.solarreader.core.util.UrlBuilder;
import de.solarreader.core.value.Converter;

import static de.solarreader.core.connection.host.HostConnection.CONTENT_TYPE_JSON;
/**
 * OpenWeather is a provider class that facilitates interaction with the OpenWeather API. It extends
 * {@link AbstractHostPlugin} and implements methods to fetch weather data from OpenWeather's web service, including
 * current weather, forecasts, and other meteorological information.
 *
 * <p>This class includes methods to configure API requests, process responses, and handle errors
 * specific to the OpenWeather service. It supports flexible configuration, allowing users to set API keys, endpoints,
 * and query parameters to retrieve weather information for specified locations.
 */
public class OpenWeather extends AbstractHostPlugin
{
    private static final String LOCATION = "location";
    private static final String APPID = "appid";
    private final ResourceBundle resourceBundle;
    public OpenWeather(Configuration config, EnvironmentProfile environmentProfile)
    {
        this(new HostConnectionFactory(), config, environmentProfile);
    }
    public OpenWeather(HostConnectionFactory connectionFactory, Configuration config,
        EnvironmentProfile environmentProfile)
    {
        super(connectionFactory, config, environmentProfile);
        this.resourceBundle = ResourceBundle.getBundle("openweather", environmentProfile.locale());
        Logger.debug("instantiate {}", this.getClass().getName());
    }
    @Override
    public Optional<UIList> installDialog()
    {
        UIList uiList = new UIList();
        uiList.addElement(
            new UITextElementBuilder().withLabel(resourceBundle.getString("openweather.title.text")).build());
        uiList.addElement(new UIInputElementBuilder().withId("id-appid").withRequired(true).withType(HtmlInputType.TEXT)
            .withPattern("[A-Za-z0-9]{32}").withColumnWidth(HtmlWidth.HALF)
            .withLabel(resourceBundle.getString("openweather.appid.text")).withName(APPID)
            .withPlaceholder(resourceBundle.getString("openweather.appid.text"))
            .withTooltip(resourceBundle.getString("openweather.appid.tooltip"))
            .withInvalidFeedback(resourceBundle.getString("openweather.appid.error")).build());
        uiList.addElement(
            new UIInputElementBuilder().withId("id-locationid").withRequired(true).withType(HtmlInputType.TEXT)
                .withPattern("\\d{7}").withColumnWidth(HtmlWidth.HALF)
                .withLabel(resourceBundle.getString("openweather.location.text")).withName(LOCATION)
                .withPlaceholder(resourceBundle.getString("openweather.location.text"))
                .withTooltip(resourceBundle.getString("openweather.location.tooltip"))
                .withInvalidFeedback(resourceBundle.getString("openweather.location.error")).build());
        return Optional.of(uiList);
    }
    @Override
    public List<Field> defaultReadableFields()
    {
        return loadFields("openweather_fields.yaml").orElse(Collections.emptyList());
    }
    @Override
    public List<TableConfiguration> defaultExportTables()
    {
        return loadTables("openweather_tables.yaml").orElse(Collections.emptyList());
    }
    public Configuration defaultConfiguration()
    {
        return HostConfig.Builder.withDefaults().withHost("api.openweathermap.org").withReadTimeoutMillis(5000).build();
    }
    @Override
    public Result verifyConnection(HostConnection connection)
    {
        String testUrl = UrlBuilder.buildUrl(hostConfig);
        try {
            connection.test(UrlBuilder.buildUri(testUrl), CONTENT_TYPE_JSON);
        } catch (IOException e) {
            return Result.error(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error(e.getMessage());
        }
        return Result.success(resourceBundle.getString("openweather.connection.successful"));
    }
    @Override
    protected void processHostField(HostConnection connection, HostField hostField, Map<String, Object> variables)
        throws IOException, InterruptedException
    {
        Map<String, String> placeHolderMap = new HashMap<>();
        if (hostConfig.extraSettings().isPresent()) {
            Map<String, Object> settings = hostConfig.extraSettings().get();
            placeHolderMap.put(LOCATION, String.valueOf(settings.get(LOCATION)));
            placeHolderMap.put(APPID, String.valueOf(settings.get(APPID)));
            placeHolderMap.put("provider_url", hostField.url());
            String url = new StringConverter(hostField.url()).replaceNamedPlaceholders(placeHolderMap);
            String json = connection.getAsString(UrlBuilder.buildUri(url));
            Map<String, String> map = JsonFlattener.flatten(json);
            Converter.convertAndPopulateIndexedVariables(hostField.dataLayout().values(), map, variables);
        } else {
            Logger.error("no settings found ");
        }
    }
}
