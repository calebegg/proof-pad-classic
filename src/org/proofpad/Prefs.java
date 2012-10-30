package org.proofpad;

import java.util.prefs.Preferences;

public class Prefs {
	static Preferences javaPrefs = Preferences.userNodeForPackage(Main.class);
	static abstract class Pref<T> {
		protected String name;
		protected T def;
		Pref(String name, T def) {
			this.name = name;
			this.def = def;
		}
		public abstract T get();
		public abstract void set(T val);
	}
	static class BooleanPref extends Pref<Boolean> {
		BooleanPref(String name, Boolean def) {
			super(name, def);
		}
		@Override public Boolean get() {
			return javaPrefs.getBoolean(name, def);
		}
		@Override public void set(Boolean val) {
			javaPrefs.putBoolean(name, val);
		}
	}
	static BooleanPref showErrors = new BooleanPref("showerrors", true);
}
