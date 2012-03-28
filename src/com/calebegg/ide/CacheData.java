package com.calebegg.ide;

import java.io.Serializable;
import java.util.Map;

public class CacheData implements Serializable {
	private Map<String, String> docs;
	private Map<Acl2Parser.CacheKey, Acl2Parser.CacheSets> bookCache;
	private static final long serialVersionUID = -1613617265816741739L;
	public Map<String, String> getDocs() {
		return docs;
	}
	public void setDocs(Map<String, String> docs) {
		this.docs = docs;
	}
	public Map<Acl2Parser.CacheKey, Acl2Parser.CacheSets> getBookCache() {
		return bookCache;
	}
	public void setBookCache(Map<Acl2Parser.CacheKey, Acl2Parser.CacheSets> parser) {
		this.bookCache = parser;
	}
}
