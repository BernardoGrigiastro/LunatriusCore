package com.github.lunatrius.core.version;

import com.github.lunatrius.core.handler.ConfigurationHandler;
import com.github.lunatrius.core.lib.Reference;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VersionChecker {
	public static final String RECOMMENDED_FORGE = "\n---\nRecommended Forge: %s";
	public static final String URL = "http://mc.lunatri.us/json?latest=1&mc=%s";

	public static final String VERSION = "%s -> %s";
	public static final String UPDATEAVAILABLE = "\nUpdate is available (%s -> %s)!";
	public static final String UPTODATE = "\nUp to date!";

	public static final String UPDATEAVAILABLECON = "Update is available for %s (%s -> %s)!";
	public static final String UPTODATECON = "%s is up to date!";
	public static final String FUTURECON = "Is %s from the future?";

	private static final List<ModMetadata> REGISTERED_MODS = new ArrayList<ModMetadata>();
	private static final Map<String, String> OUTDATED_MODS = new HashMap<String, String>();
	private static boolean done = false;

	public static void registerMod(ModMetadata modMetadata) {
		registerMod(modMetadata, Reference.FORGE);
	}

	public static void registerMod(ModMetadata modMetadata, String forgeVersion) {
		REGISTERED_MODS.add(modMetadata);

		if (modMetadata.description != null) {
			modMetadata.description += String.format(RECOMMENDED_FORGE, forgeVersion);
		}
	}

	public static Set<Map.Entry<String, String>> getOutdatedMods() {
		return OUTDATED_MODS.entrySet();
	}

	public static void setDone(boolean isDone) {
		done = isDone;
	}

	public static boolean isDone() {
		return done;
	}

	public static void startVersionCheck() {
		new Thread("LunatriusCore Version Check") {
			@Override
			public void run() {
				try {
					URL url = new URL(String.format(URL, Reference.MINECRAFT));
					InputStream con = url.openStream();
					String data = new String(ByteStreams.toByteArray(con));
					con.close();

					Map<String, Object> json = new Gson().fromJson(data, Map.class);

					if (json.get("version").equals(1.0)) {
						Map<String, Map<String, Map<String, String>>> mods = (Map<String, Map<String, Map<String, String>>>) json.get("mods");

						for (ModMetadata modMetadata : REGISTERED_MODS) {
							String modid = modMetadata.modId;
							ArtifactVersion versionLocal = new DefaultArtifactVersion(modMetadata.version);

							try {
								DefaultArtifactVersion versionRemote = new DefaultArtifactVersion(mods.get(modid).get("latest").get("version"));
								int diff = versionRemote.compareTo(versionLocal);

								if (diff > 0) {
									if (ConfigurationHandler.canNotifyOfUpdate(modid, versionRemote.getVersionString())) {
										OUTDATED_MODS.put(modMetadata.name, String.format(VERSION, versionLocal, versionRemote));
									}
									modMetadata.description += String.format(UPDATEAVAILABLE, versionLocal, versionRemote);
									Reference.logger.info(String.format(UPDATEAVAILABLECON, modid, versionLocal, versionRemote));
								} else if (diff == 0) {
									modMetadata.description += UPTODATE;
									Reference.logger.info(String.format(UPTODATECON, modid));
								} else {
									Reference.logger.info(String.format(FUTURECON, modid));
								}

								ConfigurationHandler.addUpdate(modid, versionRemote.getVersionString());
							} catch (Exception ignored) {
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				done = true;
			}
		}.start();
	}
}
