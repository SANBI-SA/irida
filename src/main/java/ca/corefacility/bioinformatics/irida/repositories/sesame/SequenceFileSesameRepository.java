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

import ca.corefacility.bioinformatics.irida.exceptions.StorageException;
import ca.corefacility.bioinformatics.irida.model.Link;
import ca.corefacility.bioinformatics.irida.model.Project;
import ca.corefacility.bioinformatics.irida.model.Sample;
import ca.corefacility.bioinformatics.irida.repositories.sesame.dao.TripleStore;
import ca.corefacility.bioinformatics.irida.model.SequenceFile;
import ca.corefacility.bioinformatics.irida.model.alibaba.ProjectIF;
import ca.corefacility.bioinformatics.irida.model.alibaba.SampleIF;
import ca.corefacility.bioinformatics.irida.model.alibaba.SequenceFileIF;
import ca.corefacility.bioinformatics.irida.model.roles.impl.Identifier;
import ca.corefacility.bioinformatics.irida.repositories.SequenceFileRepository;
import ca.corefacility.bioinformatics.irida.repositories.sesame.dao.RdfPredicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectQuery;
import org.openrdf.result.Result;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 */
public class SequenceFileSesameRepository extends GenericRepository<Identifier, SequenceFileIF, SequenceFile> implements SequenceFileRepository{
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SequenceFileSesameRepository.class);
    
    private ProjectSesameRepository projectRepo;
    private SampleSesameRepository sampleRepo;
    private final RdfPredicate hasFile = new RdfPredicate("irida", "hasFile");
    
    public SequenceFileSesameRepository(){}
    
    public SequenceFileSesameRepository(TripleStore store,AuditRepository auditRepo,LinksRepository linksRepo) {
        super(store,SequenceFileIF.class,SequenceFile.PREFIX,SequenceFile.TYPE,auditRepo,linksRepo);
    }
    
    public void setProjectRepository(ProjectSesameRepository projectRepo){
        this.projectRepo = projectRepo;
    }
    
    public void setSampleRepository(SampleSesameRepository sampleRepo){
        this.sampleRepo = sampleRepo;
    }    
    
    @Override
    public SequenceFile buildObject(SequenceFileIF base, Identifier i) {
        SequenceFile f = new SequenceFile();
        f.setIdentifier(i);
        f.setFile(base.getFile());
        
        return f;
    }
    
    public List<SequenceFile> getFilesForContainer(String uri,Class type){
        List<SequenceFile> files = new ArrayList<>();

        ObjectConnection con = store.getRepoConnection();
        try {
            String qs = store.getPrefixes()
                    + "SELECT ?s "
                    + "WHERE{ ?p a ?type . \n"
                    + "?p "+hasFile.getSparqlNotation()+" ?s . \n"
                    + "}";

            ObjectQuery query = con.prepareObjectQuery(QueryLanguage.SPARQL, qs);
            URI puri = con.getValueFactory().createURI(uri);
            query.setBinding("p", puri);
            query.setType("type", type);

            Result<SequenceFileIF> result = query.evaluate(SequenceFileIF.class);
            while (result.hasNext()) {
                SequenceFileIF o = result.next();
                URI u = con.getValueFactory().createURI(o.toString());

                SequenceFile ret = buildObjectFromResult(o, u, con);
                files.add(ret);
            }
            result.close();

        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
            logger.error(ex.getMessage());
            throw new StorageException("Couldn't list files for project");
        } finally {
            store.closeRepoConnection(con);
        }

        return files;        
    }
    
    public void addFileToContainer(String cID, SequenceFile file){
        ObjectConnection con = store.getRepoConnection();
        
        java.net.URI fNetUri = buildURI(file.getIdentifier().getIdentifier());
        
        try{
            ValueFactory fac = con.getValueFactory();
            URI puri = fac.createURI(cID);
            URI hasFile = fac.createURI(con.getNamespace("irida"),"hasFile");
            URI furi = fac.createURI(fNetUri.toString());
            Statement st = fac.createStatement(puri, hasFile, furi);
            con.add(st);
            
            file.getAuditInformation().setUpdated(new Date());
            
            auditRepo.audit(file.getAuditInformation(), fNetUri.toString());
            
        } catch (RepositoryException ex) {
            logger.error(ex.getMessage());
            throw new StorageException("Couldn't add a file to project");        
        }finally{
            store.closeRepoConnection(con);           
        }
    }
    
    public List<SequenceFile> getFilesForProject(Project project){
        String uri = project.getIdentifier().getUri().toString();
        return getFilesForContainer(uri,ProjectIF.class);
    }
    
    public List<Identifier> listFilesForProject(Project project){
        return linksRepo.listObjects(project.getIdentifier(), hasFile);
    }
    
    public List<SequenceFile> getFilesForSample(Sample sample){
        String uri = sample.getIdentifier().getUri().toString();
        return getFilesForContainer(uri,SampleIF.class);    
    }    
    
    @Override
    public void addFileToProject(Project project, SequenceFile file){
        
        /*java.net.URI pNetUri = projectRepo.buildURI(project.getIdentifier().getIdentifier());
        
        addFileToContainer(pNetUri.toString(), file);
        project.getAuditInformation().setUpdated(new Date());

        auditRepo.audit(project.getAuditInformation(), pNetUri.toString());*/
        
        Link l = new Link();
        l.setSubject(project.getIdentifier());
        l.setObject(file.getIdentifier());
        l.setRelationship(hasFile);
        linksRepo.create(l);
        
    }
    
    @Override
    public void addFileToSample(Sample sample, SequenceFile file){
        
        /*java.net.URI pNetUri = sampleRepo.buildURI(sample.getIdentifier().getIdentifier());
        
        addFileToContainer(pNetUri.toString(), file);
        sample.getAuditInformation().setUpdated(new Date());

        auditRepo.audit(sample.getAuditInformation(), pNetUri.toString());*/
        
        Link l = new Link();
        l.setSubject(sample.getIdentifier());
        l.setObject(file.getIdentifier());
        l.setRelationship(hasFile);
        linksRepo.create(l);        
    }    
    
}