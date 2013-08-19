package org.proofpad;

import org.proofpad.Acl2Parser.CacheKey;
import org.proofpad.Acl2Parser.CacheSets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GenerateData {
	public final static String pathToAcl2 = "/Applications/Proof Pad.app/Contents/Resources/Java/acl2/";
	public static void main(String[] args) throws Exception {
		CacheData cache = new CacheData();
		Main.cache = cache;

		// Documentation
		Map<String, String> docs = new HashMap<String, String>();
		File docDir = new File(pathToAcl2 + "doc" + File.separator + "HTML");
        Pattern otherSymbol = Pattern.compile("(_.*?_)");
		for (File f : docDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String s) {
				return s.endsWith(".html");
			}
		})) {
			String doc = Utils.readFile(f);
			Matcher m = Pattern.compile("</h2>(.*?)\\n").matcher(doc);
			if (!m.find()) {
				continue;
			}
			String shortDoc = m.group(1)
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
            docs.put(fun, shortDoc);
        }
		docs.put("DEFUN", "Defines a new function and adds it to the logical world so it can be " +
				"used in other functions and in the console.");
		cache.setDocs(docs);

		// Parameters

		// Included book parse results
		File bookDir = new File(pathToAcl2 + "books");
		List<File> booksToParse = new LinkedList<File>();
		Map<CacheKey, CacheSets> bookCache = new HashMap<CacheKey, CacheSets>();
        File[] newBooks = bookDir.listFiles();
        if (newBooks != null) {
            booksToParse.addAll(Arrays.asList(newBooks));
        }
        do {
			if (booksToParse.size() == 0) {
				break;
			}
			File book = booksToParse.remove(0);
			if (book.getName().equals("rtl")) continue;
			if (book.isDirectory()) {
                File[] children = book.listFiles();
                if (children != null) {
                    booksToParse.addAll(Arrays.asList(children));
                }
            } else if (book.getName().endsWith(".lisp") &&
					!bookCache.containsKey(new CacheKey(book, Long.MAX_VALUE))) {
				bookCache.put(new CacheKey(book, Long.MAX_VALUE),
						Acl2Parser.parseBook(book, bookDir, bookCache));
			}
		} while (booksToParse.size() > 0);
		cache.setBookCache(bookCache);

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("data/cache.dat"));
		oos.writeObject(cache);
		oos.close();
	}
}
