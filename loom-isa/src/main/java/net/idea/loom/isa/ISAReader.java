package net.idea.loom.isa;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.idea.i5.io.I5_ROOT_OBJECTS;

import org.isatools.isatab.ISATABValidator;
import org.isatools.isatab.gui_invokers.GUIInvokerResult;
import org.isatools.isatab_v1.ISATABLoader;
import org.isatools.tablib.schema.FormatSetInstance;
import org.isatools.tablib.utils.BIIObjectStore;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.formats.IResourceFormat;
import org.openscience.cdk.io.iterator.DefaultIteratingChemObjectReader;

import uk.ac.ebi.bioinvindex.model.AssayResult;
import uk.ac.ebi.bioinvindex.model.Contact;
import uk.ac.ebi.bioinvindex.model.Identifiable;
import uk.ac.ebi.bioinvindex.model.Study;
import uk.ac.ebi.bioinvindex.model.processing.Assay;
import uk.ac.ebi.bioinvindex.model.processing.DataNode;
import uk.ac.ebi.bioinvindex.model.processing.MaterialNode;
import uk.ac.ebi.bioinvindex.model.processing.Processing;
import uk.ac.ebi.bioinvindex.model.processing.ProtocolApplication;
import uk.ac.ebi.bioinvindex.model.term.CharacteristicValue;
import uk.ac.ebi.bioinvindex.model.term.Factor;
import uk.ac.ebi.bioinvindex.model.term.FactorValue;
import uk.ac.ebi.bioinvindex.model.term.OntologyTerm;
import uk.ac.ebi.bioinvindex.model.term.ParameterValue;
import ambit2.base.data.ILiteratureEntry;
import ambit2.base.data.SubstanceRecord;
import ambit2.base.data.study.EffectRecord;
import ambit2.base.data.study.Params;
import ambit2.base.data.study.Protocol;
import ambit2.base.data.study.ReliabilityParams;
import ambit2.base.data.study.Value;
import ambit2.base.data.substance.ExternalIdentifier;
import ambit2.base.interfaces.ICiteable;
import ambit2.base.interfaces.IStructureRecord;
import ambit2.core.io.IRawReader;

public class ISAReader extends DefaultIteratingChemObjectReader implements IRawReader<IStructureRecord>, ICiteable {
	protected SubstanceRecord record;
	protected BIIObjectStore store;
	protected Iterator<Identifiable> studyIterator;
	protected Iterator<AssayResult> assayResultIterator;
	protected Collection<Identifiable> studies;
	
	public ISAReader(File directory) throws Exception {
		try {
			ISATABLoader loader = new ISATABLoader(directory.getAbsolutePath());
			FormatSetInstance isatabInstance = loader.load();

			ISATABValidator validator = new ISATABValidator(isatabInstance);
			
		    if (GUIInvokerResult.WARNING == validator.validate()) {
		         //vlog.warn("ISA-Configurator Validation reported problems, see the messages above or the log file");
		    }
		    store = validator.getStore();
		    studies = new ArrayList<Identifiable>();
		    studies.addAll(store.values(Study.class));
		    studyIterator = studies.iterator();
		    assayResultIterator = null;
		    //parse();
		} catch (Exception x) {
			throw x;
		} finally {
			//anything to close?
		}
	}
	

	@Override
	public boolean hasNext() {
		boolean hasNext = false;
		if (assayResultIterator == null) {
			if (studyIterator.hasNext()) {
				Identifiable object = studyIterator.next();
				assayResultIterator = ((Study) object).getAssayResults().iterator();
				hasNext = assayResultIterator.hasNext();
			}  
		} else {
			hasNext = assayResultIterator.hasNext();
			if (!hasNext) { //go to outer loop
				if (studyIterator.hasNext()) {
					Identifiable object = studyIterator.next();
					assayResultIterator = ((Study) object).getAssayResults().iterator();
					hasNext = assayResultIterator.hasNext();
				}
			}
		}
		return hasNext;
	}

	@Override
	public Object next() {
		return  (assayResultIterator==null)?null:assayResultIterator.next();
	}
	
	@Override
	public IStructureRecord nextRecord() {
		AssayResult result = (AssayResult) next();
		return parseAssayResult(result);
	}

	protected SubstanceRecord parseAssayResult(AssayResult result) {
		if (result==null) return null;
		SubstanceRecord record = new SubstanceRecord();
		Protocol a_protocol = null; 
		for (Assay assay : result.getAssays()) {
			/*
			System.out.println(assay.getAcc());
			System.out.println(assay.getTechnologyName());
			System.out.println(assay.getTechnology());
			System.out.println(assay.getMeasurement());
			*/
			a_protocol = new Protocol(assay.getMeasurement().getName());
			a_protocol.setTopCategory("TOX");
			//a_protocol.setCategory(assay.getTechnology().getName());
			a_protocol.setCategory(assay.getMeasurement().getName());//I5_ROOT_OBJECTS.UNKNOWN_TOXICITY.name());
			


		}
		
		Params params = new Params();
		Params conditions = new Params();
		trackAssayResult(result.getData().getProcessingNode(),record,a_protocol,params,conditions);		
		/*
		UUID docuuid = UUID.nameUUIDFromBytes(
				(a_protocol + 
				params.toString())
				.getBytes()
				);
		*/
		UUID docuuid = UUID.randomUUID();
		ambit2.base.data.study.ProtocolApplication a_papp = new ambit2.base.data.study.ProtocolApplication(a_protocol);
		a_papp.setDocumentUUID("ISTB-"+docuuid);
		a_papp.setReference(result.getStudy().getTitle());
		a_papp.setReferenceOwner("test");
		try {
			Calendar calendar = Calendar.getInstance();  
	        calendar.setTime(result.getStudy().getReleaseDate());  
			a_papp.setReferenceYear(Integer.toString(calendar.get(Calendar.YEAR)));
		} catch (Exception x) {}

		for (Contact contact : result.getStudy().getContacts()) {
			a_papp.setReferenceOwner(contact.getUrl());
		}
		a_papp.setParameters(params);
		
		a_papp.setCompanyName(record.getOwnerName());
		a_papp.setCompanyUUID(record.getOwnerUUID());
		a_papp.setSubstanceUUID(record.getCompanyUUID());
		
		EffectRecord effect = new EffectRecord();
		effect.setEndpoint(result.getData().getName());
		effect.setTextValue(result.getData().getUrl());
		effect.setConditions(conditions);

		a_papp.addEffect(effect);
		ReliabilityParams reliability = new ReliabilityParams();
		reliability.setStudyResultType("experimental result");
		a_papp.setReliability(reliability);

		record.addMeasurement(a_papp);
		return record;
	}
	
	protected int parse() throws Exception {
		
		/*
		Iterator<Class<? extends Identifiable>> i = store.types().iterator();
		while (i.hasNext()) {
			Class<? extends Identifiable> c = i.next();
			System.out.println("=================================");
			System.out.println(c.getName());
			
			objects.addAll(store.values(c));
	        for ( Identifiable object: objects ) {
	        	System.out.print("\t");
	        	System.out.println(object);
	        }
	        objects.clear();
		}
		*/
		int r = 0;
		while (hasNext()) {
				parseAssayResult((AssayResult)next());
				r++;
		}
		return r;
		
	}
	
	
	
	protected void processFactorValues(Collection<FactorValue> factorvalues, Params params, SubstanceRecord record) {
		for (FactorValue pv : factorvalues) {
			Factor f = pv.getType();
			if ("compound".equals(f.getValue())) {
				OntologyTerm term = pv.getSingleOntologyTerm();
				record.setPublicName(pv.getValue().toLowerCase());
				if (term!=null) {
					record.setCompanyName(term.getAcc());
					record.setCompanyUUID("ISTB-"+UUID.nameUUIDFromBytes(term.getAcc().getBytes()));
					record.setOwnerName(term.getSource().getUrl());
					record.setOwnerUUID("ISTB-"+UUID.nameUUIDFromBytes(term.getSource().getName().getBytes()));
					record.setFormat("ISATAB");
					List<ExternalIdentifier> ids = new ArrayList<ExternalIdentifier>();
					record.setExternalids(ids);
					record.setSubstancetype("compound");
					ids.add(new ExternalIdentifier(term.getSource().getUrl(),term.getAcc()));
				}
			} else {
				Value factor = new Value();
				try {
					if (pv.getUnit()!=null)
						factor.setUnits(pv.getUnit().getValue());
				} catch (Exception x) {}
				try {
					factor.setLoValue(Double.parseDouble(pv.getValue()));
				} catch (Exception x) {
					factor.setLoValue(pv.getValue());
				}
				factor.setLoQualifier(" ");
				params.put(pv.getType().getValue(),factor);
			}
		}
	}
	protected void processCharacteristicValues(Collection<CharacteristicValue> characteristicValues, Params params) {
		for (CharacteristicValue pv : characteristicValues) {
			if ("Date".equals(pv.getType().getValue())) continue;
			if ("Performer".equals(pv.getType().getValue())) continue;

			Value value = new Value();
			try {
				if (pv.getUnit()!=null)
					value.setUnits(pv.getUnit().getValue());
			} catch (Exception x) {}
			try {
				value.setLoValue(Double.parseDouble(pv.getValue()));
			} catch (Exception x) {
				value.setLoValue(pv.getValue());
			}
			value.setLoQualifier(" ");
			params.put(pv.getType().getValue(),value);					
		}
	}	
	protected void processParamValues(Collection<ParameterValue> paramValues, Params params) {
		for (ParameterValue pv : paramValues) {
			if ("Date".equals(pv.getType().getValue())) continue;
			if ("Performer".equals(pv.getType().getValue())) continue;
			Value value = new Value();
			try {
				if (pv.getUnit()!=null)
					value.setUnits(pv.getUnit().getValue());
			} catch (Exception x) {}
			try {
				value.setLoValue(Double.parseDouble(pv.getValue()));
			} catch (Exception x) {
				value.setLoValue(pv.getValue());
			}
			value.setLoQualifier(" ");
			params.put(pv.getType().getValue(),value);					
		}
	}		
	protected void trackAssayResult(uk.ac.ebi.bioinvindex.model.processing.Node node,SubstanceRecord record, Protocol a_protocol,Params protocolParams,Params conditions) {
		if (node instanceof MaterialNode) {
			processFactorValues(((MaterialNode)node).getMaterial().getFactorValues(),conditions,record);
			processCharacteristicValues(((MaterialNode)node).getMaterial().getCharacteristicValues(),protocolParams);
		} else if (node instanceof DataNode) {
			processFactorValues(((DataNode)node).getData().getFactorValues(),conditions,record);
		} 		
			
		if (node.getDownstreamProcessings()==null) return;
		for (Object processing : node.getDownstreamProcessings()) 
			if (processing instanceof Processing) {
				Collection papps = ((Processing)processing).getProtocolApplications();
				if (papps!= null)
					for (Object p : papps) if (p instanceof ProtocolApplication) {
						//assay protocol params fo to conditions; study protocol params go to protocol parameters
						ProtocolApplication pa = (ProtocolApplication) p;
						boolean assayField = true;
						for (uk.ac.ebi.bioinvindex.model.Annotation annotation  : pa.getAnnotations()) {
							if ("sampleFileId".equals(annotation.getType().getValue())) {
								assayField = false; break;
							}
						}
						if (assayField) 
							processParamValues(pa.getParameterValues(),conditions);
						else
							processParamValues(pa.getParameterValues(),protocolParams);
						
						uk.ac.ebi.bioinvindex.model.Protocol protocol = ((ProtocolApplication)p).getProtocol();
						a_protocol.addGuideline(protocol.getName());
						//System.out.println(protocol);
						
					}
				for (Object in : ((Processing)processing).getInputNodes()) {
					trackAssayResult((uk.ac.ebi.bioinvindex.model.processing.Node)in,record,a_protocol,protocolParams,conditions);
				}
			}
	}
	
	@Override
	public ILiteratureEntry getReference() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setReference(ILiteratureEntry arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setReader(InputStream reader) throws CDKException {
		throw new CDKException("Not supported");
	}

	@Override
	public IResourceFormat getFormat() {
		return null;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void setReader(Reader reader) throws CDKException {
		throw new CDKException("not supported");
	}


}
