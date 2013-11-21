package org.motechproject.config.monitor;

import org.apache.commons.vfs.FileChangeEvent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileMonitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.motechproject.config.core.domain.ConfigLocation;
import org.motechproject.config.core.service.CoreConfigurationService;
import org.motechproject.config.service.ConfigurationService;
import org.motechproject.server.config.service.ConfigLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFileMonitorTest {
    @Mock
    private ConfigurationService configurationService;
    @Mock
    private DefaultFileMonitor fileMonitor;
    @Mock
    ConfigLoader configLoader;
    @Mock
    CoreConfigurationService coreConfigurationService;

    @InjectMocks
    private ConfigurationFileMonitor configFileMonitor = new ConfigurationFileMonitor();

    @Test
    public void shouldProcessExistingFilesAndStartFileMonitorWhileInitializing() throws IOException {
        final Path tempDirectory = Files.createTempDirectory("motech-config-");
        String configLocation = tempDirectory.toString();
        when(coreConfigurationService.getConfigLocation()).thenReturn(new ConfigLocation(configLocation));

        configFileMonitor.init();

        verify(configLoader).processExistingConfigs();
        final ArgumentCaptor<FileObject> argCaptor = ArgumentCaptor.forClass(FileObject.class);
        verify(fileMonitor).addFile(argCaptor.capture());
        final FileObject monitoredLocation = argCaptor.getValue();
        assertEquals(configLocation, monitoredLocation.getName().getPath());
        verify(fileMonitor).start();
    }

    @Test
    public void shouldSaveConfigWhenNewFileCreated() throws IOException {
        final String fileName = "res:config/org.motechproject.motech-module1/somemodule.properties";
        FileObject fileObject = VFS.getManager().resolveFile(fileName);

        configFileMonitor.fileCreated(new FileChangeEvent(fileObject));

        verify(configurationService).addOrUpdate(new File(fileObject.getName().getPath()));
    }

    @Test
    public void shouldNotSaveConfigWhenNewFileCreatedIsNotSupported() throws IOException {
        final String fileName = "res:config/motech-settings.conf";
        FileObject fileObject = VFS.getManager().resolveFile(fileName);

        configFileMonitor.fileCreated(new FileChangeEvent(fileObject));

        verifyZeroInteractions(configurationService);
    }

    @Test
    public void shouldSaveConfigWhenFileIsChanged() throws IOException {
        final String fileName = "res:config/org.motechproject.motech-module1/somemodule.properties";
        FileObject fileObject = VFS.getManager().resolveFile(fileName);

        configFileMonitor.fileChanged(new FileChangeEvent(fileObject));

        verify(configurationService).addOrUpdate(new File(fileObject.getName().getPath()));
    }
}
