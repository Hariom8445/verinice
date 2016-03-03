/*******************************************************************************
 * Copyright (c) 2016 Benjamin Weißenfels <bw@sernet.de>.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Benjamin Weißenfels <bw@sernet.de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.service.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import sernet.verinice.service.sync.VeriniceArchive;
import sernet.verinice.service.sync.VnaSchemaChecker;
import sernet.verinice.service.sync.VnaSchemaException;
import sernet.verinice.service.sync.VnaSchemaVersion;

/**
 * Tests the vna schema version.
 * 
 * @author Benjamin Weißenfels <bw@sernet.de>
 *
 */
public class VnaSchemaCheckerTest extends CommandServiceProvider {
    
    
    private static final String IMPORT_LEGACY_VNA = "import_legacy_version.vna";
    private static final String IMPORT_WITHOUT_SCHEMA_VNA = "import_no_version.vna";
    private static final String IMPORT_NEW_SCHEMA_VNA = "import_new_version.vna";
    private static final String IMPORT_NO_COMPATIBLE_SCHEMA_VNA = "import_no_compatible_version.vna";


    @Test
    public void testLegacyImport() throws IOException {
        
        VnaSchemaChecker vnaSchemaChecker = initVNA(IMPORT_LEGACY_VNA);
        
        VnaSchemaVersion vnaSchemaVersion = commandService.getVnaSchemaVersion();
        vnaSchemaVersion.setVnaSchemaVersion("vna-1.1");
        vnaSchemaVersion.setCompatibleSchemaVersions(Arrays.asList(new String[]{"vna-1.0", "vna-1.1"}));
        vnaSchemaChecker.checkVnaSchema(vnaSchemaVersion);        
    }
    
    @Test
    public void importNewSchema() throws IOException{
        
        VnaSchemaChecker vnaSchemaChecker = initVNA(IMPORT_NEW_SCHEMA_VNA);        
        VnaSchemaVersion vnaSchemaVersion = new VnaSchemaVersion();
        vnaSchemaVersion.setVnaSchemaVersion("vna-1.0");
        vnaSchemaVersion.setCompatibleSchemaVersions(Arrays.asList(new String[]{"vna-1.0"}));
        vnaSchemaChecker.checkVnaSchema(vnaSchemaVersion);
    }
    
    /**
     * VNA import should work if no schema is available.
     */
    @Test
    public void testImportWithoutSchemaInforamtion() throws IOException{
        
        VnaSchemaChecker vnaSchemaChecker = initVNA(IMPORT_WITHOUT_SCHEMA_VNA);
        VnaSchemaVersion vnaSchemaVersion = commandService.getVnaSchemaVersion();
        vnaSchemaChecker.checkVnaSchema(vnaSchemaVersion);        
    }
    
    
    @Test(expected=VnaSchemaException.class)
    public void testImportNoCompatibleSchemaInformation() throws IOException{
        
        VnaSchemaChecker vnaSchemaChecker = initVNA(IMPORT_NO_COMPATIBLE_SCHEMA_VNA);
        VnaSchemaVersion vnaSchemaVersion = commandService.getVnaSchemaVersion();
        vnaSchemaVersion.setVnaSchemaVersion("1.0");
        
        vnaSchemaChecker.checkVnaSchema(vnaSchemaVersion);        
    }
    
    private VnaSchemaChecker initVNA(String fileName) throws IOException {
        String path = this.getClass().getResource(fileName).getPath();
        byte[] fileData = FileUtils.readFileToByteArray(new File(path));
        VnaSchemaChecker vnaSchemaChecker = new VeriniceArchive(fileData);
        return vnaSchemaChecker;
    }
    
    
}
