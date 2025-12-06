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
package de.schnippsche.solarreader.test;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.solarreader.core.EnvironmentProfile;
import de.solarreader.core.VariableContext;
import de.solarreader.core.config.Configuration;
import de.solarreader.core.config.HostConfig;
import de.solarreader.core.connection.host.HostConnection;
import de.solarreader.core.connection.host.HostConnectionFactory;
import de.solarreader.core.field.Field;
import de.solarreader.core.table.TableConfiguration;
import de.solarreader.plugins.openweather.OpenWeather;
class OpenWeatherTest
{
    @Test
    void test() throws Exception
    {
        EnvironmentProfile environmentProfile = EnvironmentProfile.defaults();
        Configuration configuration =
            new OpenWeather(HostConfig.Builder.withDefaults().build(), environmentProfile).defaultConfiguration();
        HostConnectionFactory testFactory = new HostConnectionFactory()
        {
            @Override
            public HostConnection createConnection(Configuration configuration)
            {
                return new OpenWeatherHttpConnection();
            }
        };
        OpenWeather provider = new OpenWeather(testFactory, configuration, environmentProfile);
        List<TableConfiguration> tables = provider.defaultExportTables();
        List<Field> fields = provider.defaultReadableFields();
        VariableContext variableContext = new VariableContext(List.of());
        provider.read(fields, Instant.now(), variableContext);
/*
    Setting setting = new Setting();
    setting.setConfigurationValue("location", "123");
    setting.setConfigurationValue("appid", "45678");
    setting.setProviderHost("api.openweathermap.org");
    providerData.setSetting(setting);
    provider.setProviderData(providerData);
    generalTestHelper.testProviderInterface(provider); */
    }
}
