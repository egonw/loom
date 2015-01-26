package net.idea.loom.nm.nanowiki;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.UUID;

import net.idea.i5.io.I5CONSTANTS;
import net.idea.i5.io.I5_ROOT_OBJECTS;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.formats.IResourceFormat;
import org.openscience.cdk.io.iterator.DefaultIteratingChemObjectReader;

import ambit2.base.data.ILiteratureEntry;
import ambit2.base.data.LiteratureEntry;
import ambit2.base.data.Property;
import ambit2.base.data.StructureRecord;
import ambit2.base.data.SubstanceRecord;
import ambit2.base.data.study.EffectRecord;
import ambit2.base.data.study.IParams;
import ambit2.base.data.study.Params;
import ambit2.base.data.study.Protocol;
import ambit2.base.data.study.ProtocolApplication;
import ambit2.base.data.study.ReliabilityParams;
import ambit2.base.data.study.Value;
import ambit2.base.data.substance.ExternalIdentifier;
import ambit2.base.interfaces.ICiteable;
import ambit2.base.interfaces.IStructureRecord;
import ambit2.base.relation.STRUCTURE_RELATION;
import ambit2.base.relation.composition.Proportion;
import ambit2.core.io.IRawReader;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class NanoWikiRDFReader extends DefaultIteratingChemObjectReader implements IRawReader<IStructureRecord>,
		ICiteable {
	protected Model rdf;
	protected ResIterator materials;
	protected SubstanceRecord record;

	public NanoWikiRDFReader(Reader reader) throws CDKException {
		super();
		setReader(reader);
	}

	public static String generateUUIDfromString(String prefix, String id) {
		return prefix + "-" + (id == null ? UUID.randomUUID() : UUID.nameUUIDFromBytes(id.getBytes()));
	}

	@Override
	public void setReader(Reader reader) throws CDKException {
		try {
			rdf = ModelFactory.createDefaultModel();
			rdf.read(reader, null, "RDF/XML");
			Resource materialtype = rdf
					.createResource("http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Category-3AMaterials");
			materials = rdf.listResourcesWithProperty(RDF.type, materialtype);
		} finally {
			try {
				reader.close();
			} catch (Exception x) {
			}
		}
	}

	@Override
	public void setReader(InputStream reader) throws CDKException {
		try {
			setReader(new InputStreamReader(reader, "UTF-8"));
		} catch (Exception x) {
			throw new CDKException(x.getMessage(), x);
		}

	}

	@Override
	public IResourceFormat getFormat() {
		return null;
	}

	@Override
	public void close() throws IOException {
		rdf.close();
	}

	@Override
	public boolean hasNext() {
		if (materials == null)
			return false;
		if (materials.hasNext()) {
			Resource material = materials.next();
			record = new SubstanceRecord();
			record.setExternalids(new ArrayList<ExternalIdentifier>());
			parseMaterial(rdf, material, record);
			parseCoatings(rdf, material, record);
			return true;
		} else {
			record = null;
			return false;
		}
	}

	@Override
	public Object next() {
		if (materials == null)
			return null;
		return record;
	}

	@Override
	public void setReference(ILiteratureEntry reference) {

	}

	@Override
	public ILiteratureEntry getReference() {
		return null;
	}

	@Override
	public IStructureRecord nextRecord() {
		return record;
	}

	/*
	 * private void parseStudy(Model rdf,RDFNode studyNode,SubstanceRecord
	 * record) { StmtIterator ii =
	 * rdf.listStatements(studyNode.asResource(),null,(RDFNode)null); String
	 * endpoint = "unknown"; Protocol protocol = null; ProtocolApplication papp
	 * = new ProtocolApplication(protocol); record.addtMeasurement(papp); while
	 * (ii.hasNext()) { Statement stmt = ii.next(); Property p =
	 * stmt.getPredicate(); RDFNode node = stmt.getObject(); if
	 * (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AHas_Endpoint"
	 * )) { endpoint = node.asResource().getLocalName(); //TODO this is a
	 * resource protocol = new Protocol(endpoint); papp.setProtocol(protocol); }
	 * else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AHas_Identifier"
	 * )) {
	 * 
	 * } else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AHas_Study_Type"
	 * )) { } else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AHas_Source"
	 * )) { } else if
	 * (p.getURI().equals("http://www.w3.org/2000/01/rdf-schema#isDefinedBy")) {
	 * } else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AHas_Q2"
	 * )) { } else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AHas_R2"
	 * )) { } else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AHas_RMSEP"
	 * )) { } else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AUses_Descriptor"
	 * )) { } else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AHas_EA"
	 * )) { } else if (p.getURI().equals(
	 * "http://127.0.0.1/mediawiki/index.php/Special:URIResolver/Property-3AFor_Cell_line"
	 * )) {
	 * 
	 * 
	 * } //printStmt(stmt); }
	 * 
	 * }
	 */
	private static final String m_material = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX mw: <http://127.0.0.1/mediawiki/index.php/Special:URIResolver/>\n"
			+ "SELECT distinct ?composition ?coating ?id ?altid ?label ?type ?id ?label2 ?source ?year ?source_doi ?source_journal ?doilink ?journal_title\n"
			+ "WHERE {\n"
			+ "<%s> rdf:type mw:Category-3AMaterials.\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Chemical_Composition ?composition.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Chemical_Coating ?coating.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Identifier ?id.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Label ?label.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_NM_Type ?type.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_alternative_Identifier ?altid.}\n"
			+ "OPTIONAL {<%s> rdfs:label ?label2.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Source ?source. OPTIONAL {?source owl:sameAs ?doilink.} OPTIONAL {?source mw:Property-3AHas_Year ?year.}  OPTIONAL {?source mw:Property-3AHas_DOI ?source_doi.} OPTIONAL {?source mw:Property-3AHas_Journal ?source_journal. ?source_journal rdfs:label ?journal_title.}}\n"
			+ "}";

	private static final String m_coating = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX mw: <http://127.0.0.1/mediawiki/index.php/Special:URIResolver/>\n"
			+ "SELECT distinct ?coating ?chemical ?smiles\n" + "WHERE {\n"
			+ "<%s> mw:Property-3AHas_Chemical_Coating ?coating.\n"
			+ "?coating mw:Property-3AHas_Chemical ?chemical.\n" + "OPTIONAL {\n"
			+ "?chemical mw:Property-3AHas_SMILES ?smiles.\n" + "}\n" + "} order by ?coating ?chemical ?smiles\n";

	private void parseCoatings(Model rdf, RDFNode material, SubstanceRecord record) {
		ProcessSolution.execQuery(rdf, String.format(m_coating, material.asResource().getURI()), new ProcessCoatings(
				rdf, material, record));
	}

	private void parseMaterial(Model rdf, RDFNode material, SubstanceRecord record) {
		ProcessSolution.execQuery(rdf, String.format(m_material, material.asResource().getURI(), material.asResource()
				.getURI(), material.asResource().getURI(), material.asResource().getURI(), material.asResource()
				.getURI(), material.asResource().getURI(), material.asResource().getURI(), material.asResource()
				.getURI(), material.asResource().getURI()), new ProcessMaterial(rdf, material, record));
	}
}

class ProcessSolution {
	public int process(ResultSet rs) {
		int records = 0;
		processHeader(rs);
		while (rs.hasNext()) {
			records++;
			QuerySolution qs = rs.next();
			process(rs, qs);
		}
		return records;
	}

	void processHeader(ResultSet rs) {
		for (String name : rs.getResultVars()) {
			System.out.print(name);
			System.out.print("\t");
		}
	}

	void process(ResultSet rs, QuerySolution qs) {
		for (String name : rs.getResultVars()) {
			RDFNode node = qs.get(name);
			if (node == null)
				;
			else if (node.isLiteral())
				System.out.print(node.asLiteral().getString());
			else if (node.isResource())
				System.out.print(node.asResource().getURI());
			else
				System.out.print(node.asNode().getName());
			System.out.print("\t");
		}
		System.out.println();
	}

	protected static int execQuery(Model rdf, String sparqlQuery, ProcessSolution processor) {
		// System.out.println(sparqlQuery);
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qe = QueryExecutionFactory.create(query, rdf);
		int records = 0;
		try {
			ResultSet rs = qe.execSelect();
			records = processor.process(rs);
		} finally {
			qe.close();
		}
		return records;
	}

}

class ProcessMeasurement extends ProcessSolution {
	SubstanceRecord record;
	ILiteratureEntry citation;

	public ProcessMeasurement(SubstanceRecord record) {
		this.record = record;
		this.citation = record.getReference();
	}

	@Override
	void processHeader(ResultSet rs) {
	}

	enum endpoints {
		Zeta_Potential {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.ZETA_POTENTIAL;
			}

			@Override
			public String getTag() {
				return I5CONSTANTS.eZETA_POTENTIAL;
			}
		},
		Isoelectric_point {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.ZETA_POTENTIAL;
			}

			@Override
			public String getTag() {
				return I5CONSTANTS.eISOELECTRIC_POINT;
			}
		},
		Aggregation {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.AGGLOMERATION_AGGREGATION;
			}

			@Override
			public String getTag() {
				return I5CONSTANTS.eAGGLO_AGGR_SIZE;
			}
		},
		Primary_Particle_Size {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.PC_GRANULOMETRY;
			}

			@Override
			public String getTag() {
				return I5CONSTANTS.pPARTICLESIZE;
			}
		},
		Particle_Size {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.PC_GRANULOMETRY;
			}

			@Override
			public String getTag() {
				return I5CONSTANTS.pPARTICLESIZE;
			}
		},
		Hydrodynamic_size {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.PC_GRANULOMETRY;
			}

			@Override
			public String getTag() {
				// hydrodynamic and aerodynamic size is the same
				// MMAD is Mass median aerodynamic diameter - is this what
				// NanoWiki assumes here?
				return I5CONSTANTS.pMMAD;
			}
		},

		Shape {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.ASPECT_RATIO_SHAPE;
			}

			@Override
			public String getTag() {
				return I5CONSTANTS.eSHAPE;
			}
		},
		Specific_Surface_Area {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.SPECIFIC_SURFACE_AREA;
			}

			@Override
			public String getTag() {
				return I5CONSTANTS.SPECIFIC_SURFACE_AREA;
			}
		},
		Surface_Area {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				return I5_ROOT_OBJECTS.SPECIFIC_SURFACE_AREA;
			}
		},
		Toxicity {
			// what kind of toxicity endpoint???
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				// best guess
				return I5_ROOT_OBJECTS.TO_GENETIC_IN_VITRO;
			}
		},
		Toxicity_Classifier {
			// what kind of toxicity endpoint???
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				// best guess
				return I5_ROOT_OBJECTS.TO_GENETIC_IN_VITRO;
			}

			@Override
			public String getTag() {
				return name().replace("_", " ");
			}
		},
		Oxidation_State_Concentration, Log_Reciprocal_EC50 {
			// what endpoint?
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				// best guess
				return I5_ROOT_OBJECTS.UNKNOWN_TOXICITY;
			}
		},
		Cytotoxicity {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				// best guess
				return I5_ROOT_OBJECTS.TO_GENETIC_IN_VITRO;
			}
		},
		Log_GI50, Percentage_Non_2DViable_Cells {
			@Override
			public I5_ROOT_OBJECTS getCategory() {
				// best guess
				return I5_ROOT_OBJECTS.UNKNOWN_TOXICITY;
			}

			@Override
			public String getUnit() {
				return "%";
			}
		},
		Bioassay_Profile {
		// ????
		};
		public I5_ROOT_OBJECTS getCategory() {
			return I5_ROOT_OBJECTS.UNKNOWN_TOXICITY;
		}

		public String getTag() {
			return name();
		}

		public String getUnit() {
			return null;
		}
	}

	@Override
	void process(ResultSet rs, QuerySolution qs) {

		String endpoint = null;
		try {
			endpoint = qs.get("endpoint").asResource().getLocalName();
		} catch (Exception x) {
			endpoint = qs.get("endpoint").toString();
		}

		String assayType = null;
		String bao = null;
		String celline = null;
		try {
			assayType = qs.get("assayType").asResource().getURI();
		} catch (Exception x) {
		}
		try {
			if (assayType == null)
				assayType = qs.get("assayType1").asResource().getURI();
		} catch (Exception x) {
		}
		try {
			bao = qs.get("bao").asResource().getURI();
		} catch (Exception x) {
		}

		if (bao == null) {
			try {
				bao = qs.get("bao1").asResource().getURI();
			} catch (Exception x) {
			}

		}

		try {
			if (qs.get("t_celline") != null)
				celline = qs.get("t_celline").asLiteral().getString();
		} catch (Exception x) {
			x.printStackTrace();
		}

		// System.out.println(endpoint);
		// System.out.println(assayType);

		Protocol protocol = new Protocol(endpoint);
		String measuredEndpoint = endpoint;
		I5_ROOT_OBJECTS category = null;

		try {
			if (bao != null) {
				category = I5_ROOT_OBJECTS.valueOf(bao.replace("http://www.bioassayontology.org/bao#", ""));
			}
		} catch (Exception x) {
		}
		try {

			endpoints ep = endpoints.valueOf(endpoint.replace("-", "_").replace(" ", "_"));
			if (category == null)
				category = ep.getCategory();
			measuredEndpoint = ep.getTag();
		} catch (Exception x) {
		}

		if (category == null)
			category = I5_ROOT_OBJECTS.UNKNOWN_TOXICITY;
		protocol.setCategory(category.name() + "_SECTION");
		protocol.setTopCategory(category.getTopCategory());

		RDFNode method = qs.get("method");
		try {
			protocol.addGuideline(method.asResource().getLocalName());
		} catch (Exception x) {
		}

		ProtocolApplication<Protocol, IParams, String, IParams, String> papp = category
				.createExperimentRecord(protocol);
		// papp.setReliability(reliability)
		try {
			if (method != null)
				papp.getParameters().put(I5CONSTANTS.methodType, method.asResource().getLocalName());
		} catch (Exception x) {

		}
		papp.setDocumentUUID(NanoWikiRDFReader.generateUUIDfromString("NWKI", null));
		try {
			if (qs.get("year") != null)
				papp.setReference(qs.get("year").asLiteral().getString());
		} catch (Exception x) {
		}
		try {
			if (qs.get("doilink") != null)
				papp.setReference(qs.get("doilink").asResource().getURI());
			else
				papp.setReference(qs.get("study").asResource().getURI());

			if (qs.get("assayJournalYear") != null)
				papp.setReferenceYear(qs.get("assayJournalYear").asLiteral().getString());

			if (qs.get("assayJournalLabel") != null)
				papp.setReferenceOwner(qs.get("assayJournalLabel").asLiteral().getString());

		} catch (Exception x) {
			if (citation != null) {
				papp.setReference(citation.getURL());
				papp.setReferenceOwner(citation.getTitle());
			}
		}
		try {
			papp.setCompanyName(qs.get("measurement").asResource().getURI());
		} catch (Exception x) {
		}
		try {
			papp.setInterpretationResult(qs.get("resultInterpretation").asLiteral().getString());
		} catch (Exception x) {
		}

		if (celline != null)
			papp.getParameters().put("Cell line", celline);

		EffectRecord<String, IParams, String> effect = category.createEffectRecord();
		effect.setEndpoint(measuredEndpoint);

		try {
			effect.setTextValue(qs.get("resultInterpretation").asLiteral().getString());
		} catch (Exception x) {
		}

		try {
			RDFNode valueMin = qs.get("valueMin");
			if (valueMin != null) {
				effect.setLoValue(valueMin.asLiteral().getDouble());
				effect.setLoQualifier(">=");
			}
		} catch (Exception x) {
		}
		try {
			RDFNode valueMax = qs.get("valueMax");
			if (valueMax != null) {
				effect.setUpValue(valueMax.asLiteral().getDouble());
				effect.setUpQualifier("<=");				
			}
		} catch (Exception x) {
		}
		RDFNode value = qs.get("value");
		try {
			if (value != null)
				effect.setLoValue(Double.parseDouble(value.asLiteral().getString()));
		} catch (Exception x) {
			effect.setTextValue(value.asLiteral().getString());
			papp.setInterpretationResult(value.asLiteral().getString());
		}

		RDFNode valueError = qs.get("valueError");
		try {
			effect.setStdDev(Double.parseDouble(valueError.asLiteral().getString()));
		} catch (Exception x) {
		}

		try {
			effect.setUnit(qs.get("valueUnit").asLiteral().getString());
		} catch (Exception x) {
			//x.printStackTrace();
		}

		RDFNode dose = qs.get("dose");
		if (dose != null) {
			IParams v = new Params();
			try {
				v.setLoValue(Double.parseDouble(dose.asLiteral().getString()));
			} catch (Exception x) {
				v.setLoValue(null);
			}
			try {
				v.setUnits(qs.get("doseUnit").asLiteral().getString());
			} catch (Exception x) {
			}
			IParams conditions = effect.getConditions();
			if (effect.getConditions() == null)
				conditions = new Params();
			conditions.put(I5CONSTANTS.cDoses, v);
			effect.setConditions(conditions);
		}
		papp.addEffect(effect);
		// qs.get("label");
		// qs.get("definedBy");
		record.addMeasurement(papp);
	}
}

class ProcessNMMeasurement extends ProcessSolution {
	SubstanceRecord record;
	String endpoint;
	I5_ROOT_OBJECTS category;
	ILiteratureEntry citation;

	public ProcessNMMeasurement(SubstanceRecord record, I5_ROOT_OBJECTS category, String endpoint) {
		this.record = record;
		this.endpoint = endpoint;
		this.category = category;
		this.citation = record.getReference();
	}

	@Override
	void processHeader(ResultSet rs) {
	}

	@Override
	void process(ResultSet rs, QuerySolution qs) {
		RDFNode value = qs.get("value");
		if (value == null && qs.get("valueMin") == null)
			return;

		String assayType = null;
		String bao = null;
		try {
			assayType = qs.get("assayType").asResource().getURI();
		} catch (Exception x) {
		}
		try {
			bao = qs.get("bao").asResource().getURI();
		} catch (Exception x) {
		}

		Protocol protocol = new Protocol(endpoint);

		protocol.setCategory(category.name() + "_SECTION");
		protocol.setTopCategory(category.getTopCategory());

		RDFNode method = qs.get("method");
		try {
			protocol.addGuideline(method.asResource().getLocalName());
		} catch (Exception x) {
		}

		ProtocolApplication<Protocol, IParams, String, IParams, String> papp = category
				.createExperimentRecord(protocol);
		papp.setDocumentUUID(NanoWikiRDFReader.generateUUIDfromString("NWKI", null));
		papp.setSubstanceUUID(record.getCompanyUUID());
		ReliabilityParams reliability = new ReliabilityParams();
		reliability.setStudyResultType("experimental result");
		papp.setReliability(reliability);
		try {
			if (qs.get("year") != null)
				papp.setReference(qs.get("year").asLiteral().getString());
		} catch (Exception x) {
		}
		try {
			if (qs.get("doilink") != null)
				papp.setReference(qs.get("doilink").asResource().getURI());
			else
				papp.setReference(qs.get("study").asResource().getURI());

		} catch (Exception x) {
			if (citation != null) {
				papp.setReference(citation.getURL());
				papp.setReferenceOwner(citation.getTitle());
			}
		}

		try {
			if (method != null)
				papp.getParameters().put(I5CONSTANTS.methodType, method.asResource().getLocalName());
		} catch (Exception x) {
		}

		EffectRecord effect = category.createEffectRecord();
		effect.setEndpoint(endpoint);
		// effect.setConditions(new Params());

		try {
			if (value != null)
				effect.setLoValue(Double.parseDouble(value.asLiteral().getString()));
		} catch (Exception x) {
			effect.setTextValue(value.asLiteral().getString());
		}
		try {
			effect.setStdDev(qs.get("valueError").asLiteral().getDouble());
		} catch (Exception x) {
		}

		try {
			effect.setLoValue(qs.get("valueMin").asLiteral().getDouble());
			;
			effect.setLoQualifier(">=");
		} catch (Exception x) {
		}
		try {
			effect.setUpValue(qs.get("valueMax").asLiteral().getDouble());
			effect.setUpQualifier("<=");
		} catch (Exception x) {
		}

		try {
			effect.setUnit(qs.get("valueUnit").asLiteral().getString());
		} catch (Exception x) {
		}

		papp.addEffect(effect);
		record.addMeasurement(papp);
	}
}

// "SELECT distinct ?coating ?chemical ?smiles\n"+
class ProcessCoatings extends ProcessSolution {
	SubstanceRecord record;
	Model rdf;
	RDFNode material;
	String composition_uuid;

	public ProcessCoatings(Model rdf, RDFNode material, SubstanceRecord record) {
		this.record = record;
		this.rdf = rdf;
		this.material = material;
		composition_uuid = record.getCompanyUUID();
	}

	@Override
	void processHeader(ResultSet rs) {
	}

	@Override
	void process(ResultSet rs, QuerySolution qs) {

		// now add the same info as measurement - at least to test the approach
		Protocol protocol = I5_ROOT_OBJECTS.SURFACE_CHEMISTRY.getProtocol("Unknown");
		ProtocolApplication<Protocol, IParams, String, IParams, String> experiment = I5_ROOT_OBJECTS.SURFACE_CHEMISTRY
				.createExperimentRecord(protocol);
		experiment.setDocumentUUID(NanoWikiRDFReader.generateUUIDfromString("NWKI", null));
		record.addMeasurement(experiment);// should be one and the same
		// experiment...
		EffectRecord<String, IParams, String> erecord;

		if (record.getRelatedStructures() == null || (record.getRelatedStructures().size() < 2)) {
			if (record.getFormula() != null && !"".equals(record.getFormula())) {
				erecord = I5_ROOT_OBJECTS.SURFACE_CHEMISTRY.createEffectRecord();
				erecord.setEndpoint("ATOMIC COMPOSITION");
				erecord.setTextValue(record.getFormula());
				erecord.getConditions().put("TYPE", new Value("CORE"));
				erecord.getConditions().put("ELEMENT_OR_GROUP", new Value(record.getFormula()));
				experiment.addEffect(erecord);
			}
		}
		// coating
		IStructureRecord coating = new StructureRecord();

		record.addStructureRelation(composition_uuid, coating, STRUCTURE_RELATION.HAS_COATING, new Proportion());
		try {
			coating.setProperty(Property.getTradeNameInstance("COATING"), qs.get("coating").asResource().getLocalName());
		} catch (Exception x) {
		}
		;
		try {
			coating.setProperty(Property.getNameInstance(), qs.get("chemical").asResource().getLocalName());
		} catch (Exception x) {
		}
		;
		try {
			coating.setContent(qs.get("smiles").asLiteral().getString());
			coating.setFormat("INC");
			coating.setSmiles(coating.getContent());
		} catch (Exception x) {
		}
		;
		try {
			coating.setProperty(Property.getI5UUIDInstance(),
					NanoWikiRDFReader.generateUUIDfromString("NWKI", qs.get("chemical").asResource().getLocalName()));
		} catch (Exception x) {
			coating.setProperty(Property.getI5UUIDInstance(), NanoWikiRDFReader.generateUUIDfromString("NWKI", null));
		}
		;

		erecord = I5_ROOT_OBJECTS.SURFACE_CHEMISTRY.createEffectRecord();
		erecord.setEndpoint("ATOMIC COMPOSITION");
		try {
			erecord.getConditions().put("ELEMENT_OR_GROUP", new Value(coating.getContent()));
		} catch (Exception x) {
		}
		;
		try {
			erecord.setTextValue(qs.get("chemical").asResource().getLocalName());
		} catch (Exception x) {
		}
		;
		erecord.getConditions().put("TYPE", new Value("COATING"));

		try {
			erecord.getConditions()
					.put("COATING_DESCRIPTION", new Value(qs.get("coating").asResource().getLocalName()));
		} catch (Exception x) {
		}
		try {
			erecord.getConditions().put("DESCRIPTION", new Value(qs.get("chemical").asResource().getLocalName()));
		} catch (Exception x) {
		}
		experiment.addEffect(erecord);
	}
}

class ProcessMaterial extends ProcessSolution {
	SubstanceRecord record;
	Model rdf;
	RDFNode material;

	public ProcessMaterial(Model rdf, RDFNode material, SubstanceRecord record) {
		this.record = record;
		this.rdf = rdf;
		this.material = material;
	}

	@Override
	void processHeader(ResultSet rs) {
	}

	@Override
	void process(ResultSet rs, QuerySolution qs) {
		String name = null;
		try {
			name = qs.get("label2").asLiteral().getString();
		} catch (Exception x) {
		}
		;

		record.setReferenceSubstanceUUID(NanoWikiRDFReader.generateUUIDfromString("NWKI", name));
		record.setCompanyUUID(NanoWikiRDFReader.generateUUIDfromString("NWKI", name));
		// ?source variable is a pointer to the paper the material
		// try
		// {record.setOwnerName(qs.get("source").asResource().getLocalName());}
		// catch (Exception x) {};
		record.setOwnerName("NanoWiki");
		record.setOwnerUUID("NWKI-" + UUID.nameUUIDFromBytes(record.getOwnerName().getBytes()).toString());
		try {
			record.setSubstancetype(qs.get("type").asResource().getLocalName());
		} catch (Exception x) {
		}
		;
		try {
			record.setCompanyName(name);
		} catch (Exception x) {
		}
		;
		try {
			record.setPublicName(qs.get("label").asLiteral().getString());
		} catch (Exception x) {
			x.printStackTrace();
		}
		;
		try {
			record.getExternalids().add(new ExternalIdentifier("Has_Identifier", qs.get("id").asLiteral().getString()));
		} catch (Exception x) {
		}
		;
		try {
			record.getExternalids().add(
					new ExternalIdentifier("Alternative Identifier", qs.get("altid").asLiteral().getString()));
		} catch (Exception x) {
		}
		;
		try {
			record.getExternalids().add(
					new ExternalIdentifier("Composition", qs.get("composition").asLiteral().getString()));
		} catch (Exception x) {
		}
		;
		try {
			record.getExternalids().add(
					new ExternalIdentifier("Coating", qs.get("coating").asResource().getLocalName()));
		} catch (Exception x) {
		}
		;
		try {
			record.getExternalids().add(new ExternalIdentifier("DATASET", "NanoWiki"));
		} catch (Exception x) {
		}
		;
		try {
			record.getExternalids().add(new ExternalIdentifier("SOURCE", qs.get("source").asResource().getLocalName()));
		} catch (Exception x) {
		}
		;

		try {
			record.setFormula(qs.get("composition").asLiteral().getString());
			if (record.getFormula() != null) {
				String composition_uuid = record.getCompanyUUID();
				IStructureRecord core = new StructureRecord();
				record.addStructureRelation(composition_uuid, core, STRUCTURE_RELATION.HAS_CORE, new Proportion());
				try {
					core.setProperty(Property.getI5UUIDInstance(),
							NanoWikiRDFReader.generateUUIDfromString("NWKI", record.getFormula()));
				} catch (Exception x) {
					core.setProperty(Property.getI5UUIDInstance(),
							NanoWikiRDFReader.generateUUIDfromString("NWKI", null));
				}
				;

				core.setFormula(record.getFormula());
				// hack

				if ("TiO2".equals(core.getFormula())) {
					core.setSmiles("O=[Ti]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;

				} else if ("NiO2".equals(core.getFormula())) {
					core.setSmiles("[Ni](=O)=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("CdSe".equals(core.getFormula())) {
					core.setSmiles("[Se]=[Cd]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
					core.setProperty(Property.getCASInstance(), "1306-24-7");

				} else if ("ZrO2".equals(core.getFormula())) {
					core.setSmiles("O=[Zr]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
					core.setProperty(Property.getCASInstance(), "1314-23-4");
				} else if ("ZnO".equals(core.getFormula())) {
					core.setSmiles("O=[Zn]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
					core.setProperty(Property.getEINECSInstance(), "215-222-5");
					core.setProperty(Property.getCASInstance(), "1314-13-2");
				} else if ("Yb2O3".equals(core.getFormula())) {
					core.setSmiles("[Yb+3].[Yb+3].[O-2].[O-2].[O-2]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
					core.setProperty(Property.getCASInstance(), "1314-37-0");
				} else if ("Y2O3".equals(core.getFormula())) {
					core.setSmiles("[O-2].[O-2].[O-2].[Y+3].[Y+3]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
					core.setProperty(Property.getCASInstance(), "1314-36-9");
				} else if ("WO3".equals(core.getFormula())) {
					core.setSmiles("O=[W](=O)=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
					core.setProperty(Property.getCASInstance(), "1314-35-8");
				} else if ("V2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1314-34-7");
					core.setSmiles("O=[V]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("SnO2".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "18282-10-5");
					core.setSmiles("O=[Sn]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Sb2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1309-64-4");
					core.setSmiles("O=[Sb]O[Sb]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("SiO2".equals(core.getFormula())) {

					core.setProperty(Property.getCASInstance(), "7631-86-9");
					core.setSmiles("O=[Si]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("NiO".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1313-99-1");
					core.setSmiles("[Ni]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Ni2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1314-06-3");
					core.setSmiles("[O-2].[O-2].[O-2].[Ni+3].[Ni+3]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("MgO".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1309-48-4");
					core.setSmiles("O=[Mg]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("La2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1312-81-8");
					core.setSmiles("[O-2].[O-2].[O-2].[La+3].[La+3]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("In2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1312-43-3");
					core.setSmiles("[O-2].[O-2].[O-2].[In+3].[In+3]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("HfO2".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "12055-23-1");
					core.setSmiles("O=[Hf]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Gd2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "12064-62-9");
					core.setSmiles("[Gd+3].[Gd+3].[O-2].[O-2].[O-2]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Fe3O4".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1317-61-9");
					core.setSmiles("O=[Fe].O=[Fe]O[Fe]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Fe2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1309-37-1");
					core.setSmiles("O1[Fe]2O[Fe]1O2");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("CuO".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1317-38-0");
					core.setSmiles("[Cu]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("CrO3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1333-82-0");
					core.setSmiles("O=[Cr](=O)=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Cr2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1308-38-9");
					core.setSmiles("O=[Cr]O[Cr]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("[Co]=O".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1307-96-6");
					core.setSmiles("[Co]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Co3O4".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1308-06-1");
					core.setSmiles("O=[Co].O=[Co]O[Co]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("CoCr".equals(core.getFormula())) {
					core.setSmiles("[Co].[Cr]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("CeO2".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1306-38-3");
					core.setSmiles("O=[Ce]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Bi2O3".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "1304-76-3");
					core.setSmiles("O=[Bi]O[Bi]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Al2O3".equals(core.getFormula())) {
					core.setSmiles("O=[Al]O[Al]=O");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				} else if ("Ag".equals(core.getFormula())) {
					core.setProperty(Property.getCASInstance(), "7440-22-4");
					core.setSmiles("[Ag]");
					try {
						core.setContent(core.getSmiles());
						core.setFormat("INC");
						core.setSmiles(core.getContent());
					} catch (Exception x) {
					}
					;
				}

				// todo more info
				try {
					core.setProperty(Property.getNameInstance(), record.getFormula());
				} catch (Exception x) {
				}
				;
			}
		} catch (Exception x) {
		}
		;
		try {
			LiteratureEntry ref = new LiteratureEntry(qs.get("journal_title") == null ? null : qs.get("journal_title")
					.asLiteral().getString(), qs.get("doilink") == null ? null : (qs.get("doilink")).asResource()
					.getURI());
			record.setReference(ref);
		} catch (Exception x) {
			// System.out.println(record.getCompanyName());
			// x.printStackTrace();
			record.setReference(null);
		}
		;
		parseSize(rdf, material, record);
		parseIEP(rdf, material, record);
		parseZetaPotential(rdf, material, record);
		parseMeasurement(rdf, material, record);
	}

	private static final String m_iep = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX mw: <http://127.0.0.1/mediawiki/index.php/Special:URIResolver/>\n" + "SELECT DISTINCT ?value\n"
			+ "WHERE {\n" + "<%s> mw:Property-3AHas_IEP ?value.\n" + "}";

	private static final String m_size = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX mw: <http://127.0.0.1/mediawiki/index.php/Special:URIResolver/>\n"
			+ "SELECT DISTINCT "
			+ "?method ?value ?valueUnit ?valueError ?valueMin ?valueMax\n"
			+ "WHERE {\n"
			+
			// "?material rdf:type mw:Category-3AMaterials.\n"+
			"OPTIONAL {<%s> mw:Property-3AHas_Size ?value.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Size_Error ?valueError.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Size_Units ?valueUnit.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Size_Min ?valueMin.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Size_Max ?valueMax.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Size_Method ?method.}\n" + "}";

	private static final String m_zetapotential = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX mw: <http://127.0.0.1/mediawiki/index.php/Special:URIResolver/>\n"
			+ "SELECT DISTINCT "
			+ "?method ?value ?valueUnit ?valueError\n"
			+ "WHERE {\n"
			+
			// "?material rdf:type mw:Category-3AMaterials.\n"+
			"OPTIONAL {<%s> mw:Property-3AHas_Zeta_potential ?value.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Zeta_Error ?valueError.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Zeta_Units ?valueUnit.}\n"
			+ "OPTIONAL {<%s> mw:Property-3AHas_Zeta_Method ?method.}\n" + "}";

	private static final String m_sparql =

	"PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX mw: <http://127.0.0.1/mediawiki/index.php/Special:URIResolver/>\n"
			+ "SELECT DISTINCT \n"
			+ "?study ?measurement ?label ?method ?definedBy ?study ?assaySource ?doilink ?year\n"
			+ "?assayType ?bao ?assayType1 ?bao1 ?o_celline ?t_celline ?endpoint ?dose ?doseUnit\n"
			+ "?value ?valueMin ?valueMax ?valueUnit ?valueError ?resultInterpretation ?assayJournalLabel ?assayJournalYear\n"
			+ "WHERE {\n"
			+ "?measurement mw:Property-3AHas_Entity <%s>.\n"
			+ "OPTIONAl {?measurement rdfs:label ?label.}\n"
			+ "OPTIONAl {?measurement mw:Property-3AHas_Method ?method.}\n"
			+ "OPTIONAL {?measurement rdfs:isDefinedBy ?definedBy.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Dose ?dose.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Dose_Units ?doseUnit.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Endpoint_Class ?resultInterpretation.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Endpoint ?endpointResource. ?endpointResource rdfs:label ?endpoint.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Endpoint_Value ?value.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Endpoint_Value_Min ?valueMin.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Endpoint_Value_Max ?valueMax.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Endpoint_Value_Units ?valueUnit.}\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Endpoint_Error ?valueError.}\n"
			+ "OPTIONAL {?endpointResource mw:Property-3AHas_Assay_Type ?assayType. OPTIONAL {?assayType owl:sameAs ?bao.} }\n"
			+ "OPTIONAL {?measurement mw:Property-3AHas_Assay ?assay. \n"
			+ "OPTIONAL {?assay mw:Property-3AFor_Cell_line ?celline.  OPTIONAL {?celline owl:sameAs ?o_celline.} OPTIONAL {?celline rdfs:label ?t_celline.} }\n"
			+ "OPTIONAL {?assay mw:Property-3AHas_Assay_Type ?assayType1. OPTIONAL {?assayType1 owl:sameAs ?bao1.}}\n"
			+ "OPTIONAL {?assay mw:Property-3AHas_Source ?assaySource. OPTIONAL {?assaySource owl:sameAs ?doilink.} OPTIONAL {?assaySource mw:Property-3AHas_Year ?year.} OPTIONAL {?assaySource mw:Property-3AHas_Journal ?assayJournal. ?assayJournal rdfs:label ?assayJournalLabel. ?assaySource mw:Property-3AHas_Year ?assayJournalYear.} }  \n"
			+ "}} ORDER by ?measurement\n";

	// assay tyle linked to the assay, not endpoint
	private void parseIEP(Model rdf, RDFNode material, SubstanceRecord record) {
		execQuery(rdf, String.format(m_iep, material.asResource().getURI(), material.asResource().getURI(), material
				.asResource().getURI(), material.asResource().getURI(), material.asResource().getURI(), material
				.asResource().getURI()), new ProcessNMMeasurement(record, I5_ROOT_OBJECTS.ZETA_POTENTIAL,
				I5CONSTANTS.eISOELECTRIC_POINT));
	}

	private void parseSize(Model rdf, RDFNode material, SubstanceRecord record) {
		execQuery(rdf, String.format(m_size, material.asResource().getURI(), material.asResource().getURI(), material
				.asResource().getURI(), material.asResource().getURI(), material.asResource().getURI(), material
				.asResource().getURI()), new ProcessNMMeasurement(record, I5_ROOT_OBJECTS.PC_GRANULOMETRY,
				I5CONSTANTS.pPARTICLESIZE));
	}

	private void parseZetaPotential(Model rdf, RDFNode material, SubstanceRecord record) {
		execQuery(rdf, String.format(m_zetapotential, material.asResource().getURI(), material.asResource().getURI(),
				material.asResource().getURI(), material.asResource().getURI(), material.asResource().getURI(),
				material.asResource().getURI()), new ProcessNMMeasurement(record, I5_ROOT_OBJECTS.ZETA_POTENTIAL,
				I5CONSTANTS.eZETA_POTENTIAL));
	}

	private void parseMeasurement(Model rdf, RDFNode material, SubstanceRecord record) {
		execQuery(rdf, String.format(m_sparql, material.asResource().getURI()), new ProcessMeasurement(record));
	}

}
