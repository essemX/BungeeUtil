package dev.wolveringer.bungeeutil.plugin.updater;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import dev.wolveringer.bungeeutil.BungeeUtil;
import dev.wolveringer.bungeeutil.Configuration;
import dev.wolveringer.bungeeutil.MathUtil;
import dev.wolveringer.bungeeutil.plugin.Main;
import lombok.NonNull;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;

public class UpdaterV1 implements Updater{

	private String url;
	private JSONObject data;
	private long last;

	public UpdaterV1(String url) {
		this.url = url;
	}

	public boolean checkUpdate() {
		this.updateData();
		if (this.data == null) {
			throw new NullPointerException("HTTP Data is null. Invpoke getData() first");
		}
		if (!this.isNewstVersion()) {
			this.installUpdate();
			BungeeCord.getInstance().stop();
			return true;
		}
		else {
			if(this.getCurrentVersion().isSnapshot()){
				BungeeUtil.getInstance().sendMessage(ChatColor.LIGHT_PURPLE + "Attention: This build is a snapshot. This may containing bugs.");
			} else {
				BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "The current version is already the newest version. That's for keeping BungeeUtil up to date.");
			}
		}
		return false;
	}

	public HashMap<Version, List<String>> createChanges(@NonNull Version lastVersion){
		HashMap<Version, List<String>> out = new HashMap<>();
		if(this.data != null){
			JSONArray changelogArray = this.data.getJSONArray("Changelog");
			Iterator<Object> objects = changelogArray.iterator();
			while (objects.hasNext()) {
				JSONObject object = (JSONObject) objects.next();
				String sversion = object.getString("Verion");
				if(object.has("snapshot") && sversion.endsWith("-SNAPSHOT")) {
					sversion += "-SNAPSHOT";
				}

				Version version = new Version(sversion);
				if(lastVersion.compareTo(version) < 0){
					ArrayList<String> changes = new ArrayList<>();
					Iterator<Object> message = object.getJSONArray("Changed").iterator();
					while (message.hasNext()) {
						changes.add((String) message.next());
					}
					out.put(version, changes);
				}
			}
		} else {
			out.put(new Version("ERROR"), Arrays.asList(ChatColor.RED+"Cant featch versions data.","Make shure you have an valid internet connection."));
		}
		return out;
	}

	/**
	 *
	 * @param url
	 * @param targetFile
	 * @return errormask
	 * errors:
	 * 0: Create new file exception
	 * 1: Invalid jar
	 * 2: cant delete invalid jar
	 * 3: Download IO error
	 * 3: Finaly error
	 */
	private int downloadUpdate(String url, File targetFile) {
		BigInteger errorMask = new BigInteger("0");
		errorMask.setBit(8);
		BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "Updating from "+this.getCurrentVersion().getVersion()+" to "+this.getNewestVersion().getVersion());
		BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "Starting to download the update ("+url+") to "+targetFile.getAbsolutePath());
		programm:
		try {
			BungeeUtil.getInstance().setInformation(ChatColor.GREEN + "Downloading update " + ChatColor.GRAY + "[" + ChatColor.GREEN + "000%" + ChatColor.GRAY+ "]");
			BufferedInputStream in = null;
			FileOutputStream fout = null;
			try {
				URLConnection com = new URL(url).openConnection();
				int fileLength = com.getContentLength();
				in = new BufferedInputStream(com.getInputStream());
				File df;
				if (targetFile.exists()) {
					BungeeUtil.getInstance().setInformation(ChatColor.GREEN + "Using .download file ("+targetFile.getPath() + "BungeeUtil.download)!");
					fout = new FileOutputStream(df = new File(targetFile.getPath() + "BungeeUtil.download"));
				} else {
					fout = new FileOutputStream(df = targetFile);
				}
				final byte data[] = new byte[Configuration.getLoadingBufferSize()];
				int count;
				int readed = 0;
				while (true) {
					count = in.read(data, 0, data.length);
					if (count == -1) {
						break;
					}
					fout.write(data, 0, count);
					readed += count;
					String p = "000" + MathUtil.calculatePercentExact(readed, fileLength);
					p = p.substring(0, p.indexOf("."));
					p = p.substring(p.length() - 3, p.length());
					BungeeUtil.getInstance().setInformation(ChatColor.GREEN + "Downloading update " + ChatColor.GRAY + "[" + ChatColor.GREEN + p + "%" + ChatColor.GRAY+ "]");
				}
				fout.close();
				in.close();
				BungeeUtil.getInstance().setInformation(ChatColor.GREEN + "Download done!");
				BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "Update downloaded!");
				BungeeUtil.getInstance().setInformation(ChatColor.GREEN + "Check update for errors!");
				BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "Check update for errors!");
				try {
					JarInputStream is = new JarInputStream(new FileInputStream(df));
					while (null != is.getNextJarEntry()) {
					}
					is.close();
				}
				catch (Exception e) {
					errorMask.setBit(1);
					BungeeUtil.getInstance().sendMessage(ChatColor.RED + "The update contains an error. (Message: " + e.getLocalizedMessage() + ")");
					BungeeUtil.getInstance().sendMessage(ChatColor.RED + "Deleting the update!");
					try {
						df.delete();
					}
					catch (Exception ex) {
						errorMask.setBit(2);
					}
					break programm;
				}
				BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "Update valid.");
				BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "Installing update!");
				BungeeUtil.getInstance().setInformation(ChatColor.GREEN + "Installing update");
				if (!targetFile.equals(df) && !targetFile.delete()) {
					BungeeUtil.getInstance().sendMessage(ChatColor.GOLD + "Cant delete the old plugin jar.");
				}
				boolean deleteOld = !targetFile.equals(df);
				if(!targetFile.createNewFile()){
					deleteOld = false;
					BungeeUtil.getInstance().sendMessage(ChatColor.GOLD + "Cant create new jar.");
				}
				FileInputStream fis = new FileInputStream(df);
				FileOutputStream fos = new FileOutputStream(targetFile);
				while ((count = fis.read(data, 0, data.length)) != -1) {
					fos.write(data, 0, count);
				}
				fis.close();
				fos.close();
				if (deleteOld && !df.delete()) {
					BungeeUtil.getInstance().sendMessage(ChatColor.GOLD + "Cant delete the cache file!");
				}
				BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "Restarting bungeecord!");
				BungeeUtil.getInstance().setInformation(ChatColor.GREEN + "Update installed!");
			}
			catch (Exception e) {
				errorMask.setBit(3);
				e.printStackTrace();
				BungeeUtil.getInstance().sendMessage(ChatColor.RED + "An error happend while downloading the update");
			}
			finally {
				if (in != null) {
					in.close();
				}
				if (fout != null) {
					fout.close();
				}
			}
		}
		catch (Exception e) {
			errorMask.setBit(4);
			e.printStackTrace();
			BungeeUtil.getInstance().sendMessage(ChatColor.RED + "An error happend while downloading the update");
		}
		return errorMask.intValue();
	}

	public Version getCurrentVersion(){
		return new Version(Main.getMain().getDescription().getVersion());
	}

	public JSONObject getData() {
		this.updateData();
		return this.data;
	}

	public Version getNewestVersion() {
		this.updateData();
		return new Version(this.data.getString("CurrentVersion"));
	}

	public int installUpdate(){
		File ownFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile());
		return this.downloadUpdate(this.data.getString("Download"), ownFile);
	}

	public boolean isDevBuild(){
		return false;
	}

	public boolean isNewstVersion() {
		return this.getNewestVersion().compareTo(this.getCurrentVersion()) <= 0;
	}

	public boolean loadData() {
		this.last = System.currentTimeMillis();
		if(BungeeUtil.getInstance() != null) {
			BungeeUtil.getInstance().sendMessage(ChatColor.GREEN + "Fetching update data.");
		}
		try {
			URL i = new URL(this.url);
			HttpURLConnection c = (HttpURLConnection) i.openConnection();
			c.setRequestMethod("GET");
			BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			this.data = new JSONObject(response.toString());
			return true;
		}
		catch (Exception e) {
			BungeeUtil.debug(e);
		}
		return false;
	}

	public void updateData() {
		if (System.currentTimeMillis() - this.last > TimeUnit.MINUTES.toMillis(10)) {
			this.loadData();
		}
	}
	
	@Override
	public List<String> getBugs(Version version) {
		return new ArrayList<>();
	}
	
	@Override
	public List<String> getMOTD(Version version) {
		return new ArrayList<>();
	}
	
	@Override
	public List<String> getChangeNotes(Version version) {
		List<String> out = new ArrayList<>();
		if(this.data != null){
			JSONArray changelogArray = this.data.getJSONArray("Changelog");
			Iterator<Object> objects = changelogArray.iterator();
			while (objects.hasNext()) {
				JSONObject object = (JSONObject) objects.next();
				String sversion = object.getString("Verion");
				if(object.has("snapshot") && sversion.endsWith("-SNAPSHOT")) {
					sversion += "-SNAPSHOT";
				}

				Version uversion = new Version(sversion);
				if(uversion.compareTo(version) == 0){
					ArrayList<String> changes = new ArrayList<>();
					Iterator<Object> message = object.getJSONArray("Changed").iterator();
					while (message.hasNext()) {
						changes.add((String) message.next());
					}
					return changes;
				}
			}
		} else {
			out.addAll(Arrays.asList(ChatColor.RED+"Cant featch versions data.","Make shure you have an valid internet connection."));
		}		
		return out;
	}
	
	@Override
	public Version getOwnVersion() {
		return this.getCurrentVersion();
	}
	
	@Override
	public boolean isOfficialBuild() {
		return true;
	}
	
	@Override
	public List<Version> getVersionsBehind() {
		return new ArrayList<>(createChanges(getCurrentVersion()).keySet()); //its so bad code but its my v1 patches...
	}
	
	@Override
	public UpdateState updateTo(Version target) {
		return update();
	}
	
	@Override
	public UpdateState update() {
		switch (this.installUpdate()) {
		case 0:
			return UpdateState.SUCCESSFULL;
		default:
			return UpdateState.FAILED_DOWNLOAD;
		}
	}
	
	@Override
	public boolean isValid() {
		return getData() != null;
	}
	
	@Override
	public boolean hasUpdate() {
		return isNewstVersion();
	}
}
