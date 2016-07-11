package org.aksw.simba.dbpedia.search;

import static spark.Spark.port;
import static spark.Spark.get;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.net.URLDecoder;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.aksw.simba.dbpedia.indexcreation.Handler_SparqlEndpoint;
import org.aksw.simba.dbpedia.output.JsonLdOutput;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;

public class SearchLuceneLabel {
	public static final String APP_PACKAGE = "org.aksw.simba.dbpedia";
	final static int TIMES_MORE_RESULTS = 10;

	public DirectoryReader readerFromIndex(NIOFSDirectory dir) throws IOException {
		return DirectoryReader.open(dir);
	}

	static boolean flag = false;

	public BooleanQuery queryFromString(String queryString) throws UnsupportedEncodingException {
		BooleanQuery query = new BooleanQuery();
		PhraseQuery phraseQuery = new PhraseQuery();
		String[] words = URLDecoder.decode(queryString, "UTF-8").toLowerCase().split(" ");
		for (String it : words) {
			it.trim();
			if (!(it.isEmpty()) && (it != "*")) {
				Term term = new Term("label", it);
				phraseQuery.add(term);
				TermQuery parse = new TermQuery(term);
				parse.setBoost(0.9f);
				BooleanClause clause = new BooleanClause(parse, Occur.SHOULD);
				query.add(clause);
			}
		}

		query.add(new BooleanClause(phraseQuery, Occur.SHOULD));
		return query;
	}

	public List<Result> search(IndexSearcher searcher, String queryString, Integer limit) throws IOException {
		if (limit == 0)
			limit = 10;
		BooleanQuery query = queryFromString(queryString);

		int hitsPerPage = limit * TIMES_MORE_RESULTS;
		Sort sort = new Sort(SortField.FIELD_SCORE,
				new SortedNumericSortField("pagerank_sort", SortField.Type.FLOAT, true));
		TopFieldDocs hits = searcher.search(query, hitsPerPage, sort);

		List<Result> res = new ArrayList<Result>();

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			Result result = new Result(doc.get("url"), doc.get("label"), Double.parseDouble(doc.get("pagerank")));
			res.add(result);
		}
		return res;
	}

	/*
	 * public List<Result> search_dump(IndexSearcher searcher, String
	 * queryString, Integer limit) throws IOException { if (limit == 0) limit =
	 * 10; BooleanQuery query = queryFromString(queryString);
	 *
	 * int hitsPerPage = limit * TIMES_MORE_RESULTS; Sort sort = new Sort(new
	 * SortField("label", SortField.Type.STRING, true)); TopFieldDocs hits =
	 * searcher.search(query, hitsPerPage, sort);
	 *
	 * List<Result> res = new ArrayList<Result>();
	 *
	 * for (ScoreDoc scoreDoc : hits.scoreDocs) { Document doc =
	 * searcher.doc(scoreDoc.doc); Result result = new Result(doc.get("url"),
	 * doc.get("label")); res.add(result); } return res; }
	 */
	/*
	 * public static List<Result> getRDFDumpResult(String term) throws
	 * IOException { Properties prop = new Properties(); InputStream input = new
	 * FileInputStream("src/main/java/properties/autoindex.properties");
	 * prop.load(input); IndexSearcher searcher = null; List<Result> resultlist
	 * = null;
	 *
	 * String indexDir = prop.getProperty("index_dump");
	 *
	 * @SuppressWarnings("deprecation") IndexReader reader =
	 * IndexReader.open(NIOFSDirectory.open(new File(indexDir))); searcher = new
	 * IndexSearcher(reader);
	 *
	 * SearchLuceneLabel tester;
	 *
	 * try { tester = new SearchLuceneLabel();
	 *
	 * resultlist = tester.search_dump(searcher, term, 0);
	 *
	 * } catch (IOException e) { e.printStackTrace(); } return resultlist; }
	 */
	public static List<Result> searchEndpoint(String index, String term, int limit) throws IOException {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("src/main/java/properties/autoindex.properties");
		prop.load(input);
		IndexSearcher searcher = null;
		List<Result> resultlist = null;
		switch (index.toUpperCase()) {
		case "CLASS": {

			String indexDir = prop.getProperty("index_class");

			@SuppressWarnings("deprecation")
			IndexReader reader = IndexReader.open(NIOFSDirectory.open(new File(indexDir)));
			searcher = new IndexSearcher(reader);

			break;
		}
		case "INSTANCE":

		{

			String indexDir = prop.getProperty("index_instance");

			@SuppressWarnings("deprecation")
			IndexReader reader = IndexReader.open(NIOFSDirectory.open(new File(indexDir)));
			searcher = new IndexSearcher(reader);
			break;
		}
		case "PROPERTY": {
			String indexDir = prop.getProperty("index_property");

			@SuppressWarnings("deprecation")
			IndexReader reader = IndexReader.open(NIOFSDirectory.open(new File(indexDir)));
			searcher = new IndexSearcher(reader);
			break;
		}
		default:
			String indexDir = prop.getProperty("index_instance");

			@SuppressWarnings("deprecation")
			IndexReader reader = IndexReader.open(NIOFSDirectory.open(new File(indexDir)));
			searcher = new IndexSearcher(reader);
			flag = true;
			break;

		}

		SearchLuceneLabel tester;

		try {
			tester = new SearchLuceneLabel();

			resultlist = tester.search(searcher, term, 0);

		} catch (IOException e) {
			e.printStackTrace();
		}

		Collections.sort(resultlist, new Comparator<Result>() {
			public int compare(Result a, Result b) {
				return a.getPagerank().compareTo(b.getPagerank());
			}
		});
		resultlist.subList(limit + 1, resultlist.size()).clear();
		return resultlist;
	}

	private static Logger log = LoggerFactory.getLogger(SearchLuceneLabel.class);

	public static void main(String[] args) throws IOException {
		Handler_SparqlEndpoint.generateIndexforClass();
		Handler_SparqlEndpoint.generateIndexforProperties();
		Handler_SparqlEndpoint.generateIndexforInstances();
		// final String swaggerJson = SwaggerParser.getSwaggerJson(APP_PACKAGE);

		port(8080);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// List<Result> query_result =
		// SearchLuceneLabel.searchEndpoint("instance", "berlin");
		// String x = JsonLdOutput.getJsonLDoutput(query_result,"instance");

		// gson.toJson(query_result);

		get("/search", (req, res) -> {
			String index = req.queryParams("index");
			String searchlabel = req.queryParams("query");
			String indent = req.queryParams("indent");
			int limit = Integer.parseInt(req.queryParams("limit"));
			List<Result> query_result = SearchLuceneLabel.searchEndpoint(index, searchlabel, limit);
			res.type("application/json");
			log.info("Responding to Query");
			if (flag == true) {

				log.info("Choosing default index");
				flag = false;
				if (indent.toUpperCase().equals("YES")) {
					System.out.println(JsonLdOutput.getJsonLDoutput(query_result, index, limit));
					return JsonLdOutput.getJsonLDoutput(query_result, index, limit);
				}

				return gson.toJson(query_result);

			} else {
				if (indent.toUpperCase().equals("YES")) {
					System.out.println(JsonLdOutput.getJsonLDoutput(query_result, index, limit));
					return JsonLdOutput.getJsonLDoutput(query_result, index, limit);
				} else

					return gson.toJson(query_result);
			}
		});

	}

}
