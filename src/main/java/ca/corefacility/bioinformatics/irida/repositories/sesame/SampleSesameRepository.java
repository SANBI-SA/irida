/*
 * Copyright 2013 Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.corefacility.bioinformatics.irida.repositories.sesame;

import ca.corefacility.bioinformatics.irida.dao.PropertyMapper;
import ca.corefacility.bioinformatics.irida.dao.TripleStore;
import ca.corefacility.bioinformatics.irida.exceptions.StorageException;
import ca.corefacility.bioinformatics.irida.model.Sample;
import ca.corefacility.bioinformatics.irida.model.roles.impl.Identifier;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 */
public class SampleSesameRepository extends GenericRepository<Identifier, Sample>{
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SampleSesameRepository.class);
    
    public SampleSesameRepository(){}
    
    public SampleSesameRepository(TripleStore store) {
        super(store,Sample.class);

        PropertyMapper map = new PropertyMapper("irida", "Sample");
        
        try{
            map.addProperty("rdfs","label","sampleName", Sample.class.getMethod("getSampleName"), Sample.class.getMethod("setSampleName",String.class), String.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            logger.error(ex.getMessage());
            throw new StorageException("Couldn't build parameters for \"Sample\""); 
        }
        
        setPropertyMap(map);
    }      
}