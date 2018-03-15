package ie.tcd;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Indexes the documents parsed using ParseDocs.java and saves in disk!
 * @author Ajay Maity Local
 *
 */
public class Indexer {

	private static final String ftDocStr = "ft";
	
	/**
	 * Delete Directory from file system (will delete the contents recursively)
	 * @param file directory or file to delete
	 */
	public static void deleteDir(File file) {
		
		File[] contents = file.listFiles();
		if (contents != null) {
			
			for (File f: contents) {
				
				deleteDir(f);
			}
		}
		file.delete();
	}
	
	/**
	 * Main Method
	 * @param args Command line arguments
	 * @throws IOException when files/directories are not present
	 */
	public static void main(String[] args) throws IOException {
		
		Map<String, String> values = new ParseCLA(args, "Indexer").parse();
		String docsDirStr = values.get("docsDir");

		docsDirStr = docsDirStr.replace("\\", "/");
		if (!docsDirStr.endsWith("/")) docsDirStr += "/";
		
		File docsDir = new File(docsDirStr);
		if (!docsDir.isDirectory()) {
			
			System.out.println(docsDir + " is not a directory. Please input correct arguments, or run with default no arguments.");
			System.out.println("Exiting application.");
			System.exit(1);
		}
		
		File ftDocsFile = new File(docsDirStr + ftDocStr + ".json");
		if (!ftDocsFile.isFile()) {
			
			System.out.println(ftDocsFile + " is not present. Please ensure parsed " + ftDocStr + ".json is present in the directory.");
			System.out.println("Exiting application.");
			System.exit(1);
		}
		
		JSONParser jsonParser = new JSONParser();
		JSONArray ftDocs = null;
		try {
			
			System.out.println("Reading " + ftDocStr + ".json...");
			ftDocs = (JSONArray) jsonParser.parse(new FileReader(docsDirStr + ftDocStr + ".json"));
		} 
		catch (org.json.simple.parser.ParseException e) {
		
			System.out.println("Unable to parse " + ftDocStr + ".json. Please ensure the format is correct.");
			System.out.println("Exiting application.");
			System.exit(1);
		}
		System.out.println("Reading done.\n");
		
		// Create analyzer
		Analyzer analyzer = new EnglishAnalyzer();
		
		System.out.println("Deleting previous index files, if they exist...");
		deleteDir(new File("./outputs/indexes/" + ftDocStr + ".index"));
		System.out.println("Done!\n");
		
		// Store index on disk
		Directory directory = FSDirectory.open(Paths.get("./outputs/indexes/" + ftDocStr + ".index"));
		
		// Create index writer
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		config.setSimilarity(new BM25Similarity());
		
		IndexWriter iwriter = new IndexWriter(directory, config);
		
		System.out.println("Creating index using English analyzer and BM25 similarity...");		
		for (Object obj : ftDocs) {
				
			JSONObject ftDoc = (JSONObject) obj;
			
			Document document = new Document();
			@SuppressWarnings("unchecked")
			Set<String> elements = ftDoc.keySet();
			for (String element : elements) 
				document.add(new TextField(element, (String) ftDoc.get(element), Field.Store.YES));			
			iwriter.addDocument(document);
		}
		System.out.println("Indexing done, and saved on disk.");
		
		iwriter.close();
		directory.close();
	}
}