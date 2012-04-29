package org.proofpad;

import java.util.regex.*;
import java.io.*;
import java.util.*;

import org.proofpad.Acl2Parser.CacheKey;
import org.proofpad.Acl2Parser.CacheSets;


public class GenerateData {
	public final static String pathToAcl2 = "/Users/calebegg/Code/acl2/";
	public static void main(String[] args) throws Exception {
		CacheData cache = new CacheData();
		Main.cache = cache;

		// Documentation
		Map<String, String> docs = new HashMap<String, String>();
		File docdir = new File(pathToAcl2 + "doc" + File.separator + "HTML");
		for (File f : docdir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String s) {
				return s.endsWith(".html");
			}
		})) {
			Scanner docScanner = null;
			try {
				docScanner = new Scanner(f);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
			docScanner.useDelimiter("\\Z");
			String doc = docScanner.next();
			Matcher m = Pattern.compile("</h2>(.*?)\\n").matcher(doc);
			if (!m.find()) {
				continue;
			}
			String shortdoc = m.group(1)
					.replaceAll("<.*?>", "");
			String fun = f.getName();
			fun = fun.substring(0, fun.length() - 5)
					.replaceAll("_colon_", ":")
					.replaceAll("_bang_", "!")
					.replaceAll("_slash_", "/")
					.replaceAll("_star_", "*")
					.replaceAll("_hyphen_", "-")
					.replaceAll("_gt_", ">")
					.replaceAll("_lt_", "<")
					.replaceAll("_lparen_", "(")
					.replaceAll("_rparen_", ")")
					.replaceAll("_qm_", "?")
					.replaceAll("_at_", "@");
			if ((m = Pattern.compile("_.*?_").matcher(fun)).find()) {
				continue;
			}
			docs.put(fun, shortdoc);
		}
		docs.put("DEFUN", "Defines a new function and adds it to the logical world so it can be " +
				"used in other functions and in the console.");
		cache.setDocs(docs);

		// Parameters

		// Included book parse results
		File bookdir = new File(pathToAcl2 + "books");
		List<File> booksToParse = new LinkedList<File>();
		Map<CacheKey, CacheSets> bookCache = new HashMap<CacheKey, CacheSets>();
		booksToParse.addAll(Arrays.asList(bookdir.listFiles()));
		do {
			if (booksToParse.size() == 0) {
				break;
			}
			File book = booksToParse.remove(0);
			if (book.getName().equals("rtl")) continue;
			if (book.isDirectory()) {
				booksToParse.addAll(Arrays.asList(book.listFiles()));
			} else if (book.getName().endsWith(".lisp") &&
					!bookCache.containsKey(new CacheKey(book, Long.MAX_VALUE))) {
				bookCache.put(new CacheKey(book, Long.MAX_VALUE),
						Acl2Parser.parseBook(book, bookdir, bookCache));
			}
		} while (booksToParse.size() > 0);
		cache.setBookCache(bookCache);

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("data/cache.dat"));
		oos.writeObject(cache);
	}
}
