package net.mightypork.rpw;


import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import javax.swing.JFrame;

import net.mightypork.rpw.gui.Icons;
import net.mightypork.rpw.gui.widgets.MenuMain;
import net.mightypork.rpw.gui.widgets.SidePanel;
import net.mightypork.rpw.gui.widgets.TreeDisplay;
import net.mightypork.rpw.gui.windows.Alerts;
import net.mightypork.rpw.gui.windows.WindowCrash;
import net.mightypork.rpw.gui.windows.WindowMain;
import net.mightypork.rpw.library.Sources;
import net.mightypork.rpw.project.NodeSourceProvider;
import net.mightypork.rpw.project.Projects;
import net.mightypork.rpw.tasks.TaskDevel;
import net.mightypork.rpw.tasks.Tasks;
import net.mightypork.rpw.utils.Log;
import net.mightypork.rpw.utils.OsUtils;

import org.jdesktop.swingx.JXFrame;


public class App {

	public static App inst;

	/** Main App Window */
	public WindowMain window;
	public MenuMain menu;

	public NodeSourceProvider activeProject;


	public static void main(String[] args) {

		// use crash dialog to uncaught errors
		Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());

		inst = new App();
		inst.start();
	}


	public App() {

	}


	public void start() {

		init();
	}


	public void init() {

		if (!lockInstance()) {

			//@formatter:off
			Alerts.error(
					null,
					"Couldn't lock workdir",
					"The application is already running.\n" +
					"\n" +
					"No more than one instance can run at a time."
			);
			//@formatter:on

			System.exit(1);
		}

		Log.init();

		Log.i("ResourcePack Workbench v." + Const.VERSION + " (#" + Const.VERSION_SERIAL + ")");

		Log.f1("Init started...");
		OsUtils.initDirs();
		Config.init();


		Icons.init();


		Tasks.taskLoadHelp();
		Tasks.taskCreateModConfigFiles();
		Sources.init();

		Tasks.checkUpdate();

		Log.f2("Building main window");

		int t = Tasks.taskBuildMainWindow();
		while (Tasks.isRunning(t)) {} // wait for completion on EDT

		Log.f2("Opening last project (if any)");

		Projects.openLastProject();

		TaskDevel.run();
	}


	public void deinit() {

		try {
			window.frame.dispose();
		} catch (Throwable t) {}
	}


	public static void die(String message) {

		Alerts.error(null, "Fatal Error", message);
		App.inst.deinit();
		System.exit(1);
	}


	private static boolean lockInstance() {

		final File lockFile = OsUtils.getAppDir(".lock");

		try {
			final RandomAccessFile randomAccessFile = new RandomAccessFile(lockFile, "rw");
			final FileLock fileLock = randomAccessFile.getChannel().tryLock();
			if (fileLock != null) {
				Runtime.getRuntime().addShutdownHook(new Thread() {

					@Override
					public void run() {

						try {
							fileLock.release();
							randomAccessFile.close();
							lockFile.delete();
						} catch (Exception e) {
							System.out.println("Unable to remove lock file.");
							e.printStackTrace();
						}
					}
				});
				return true;
			}
		} catch (Exception e) {
			System.out.println("Unable to create lock file.");
			e.printStackTrace();
		}
		return false;
	}


	/**
	 * Show crash report dialog with error stack trace.
	 * 
	 * @param error
	 */
	public static void onCrash(final Throwable error) {

		JFrame frame = new WindowCrash(error);
		frame.setVisible(true);
		while (true) {}
	}


	public static SidePanel getSidePanel() {

		if (inst == null || inst.window == null) return null;

		return inst.window.sidePanel;
	}


	public static TreeDisplay getTreeDisplay() {

		if (inst == null || inst.window == null) return null;

		return inst.window.treeDisplay;
	}


	public static JXFrame getFrame() {

		if (inst == null || inst.window == null) return null;

		return inst.window.frame;
	}


	public static WindowMain getWindow() {

		if (inst == null) return null;

		return inst.window;
	}


	public static MenuMain getMenu() {

		if (inst == null || inst.window == null) return null;

		return inst.window.menu;
	}


	public static void setWaiting(boolean state) {

		if (inst == null || inst.window == null) return;

		inst.window.setWaiting(state);

	}
}