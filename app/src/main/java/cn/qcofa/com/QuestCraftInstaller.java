package cn.qcofa.com;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * QuestCraft installation plumbing.
 *
 * Privileged operations are deliberately isolated in doTheSuperHere().
 * Replace that method locally with the root implementation appropriate for the device.
 */
public final class QuestCraftInstaller {
    public static final String QUESTCRAFT_PACKAGE = "com.qcxr.qcxr";

    public static final class Result {
        public final boolean success;
        public final String message;

        Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private QuestCraftInstaller() {}

    public static Result install(Context context, File accountFile, File launcherConf) {
        if (context == null) return new Result(false, "Context is missing");
        if (accountFile == null || !accountFile.isFile()) {
            return new Result(false, "Generated account file is missing");
        }
        if (launcherConf == null || !launcherConf.isFile()) {
            return new Result(false, "Generated launcher.conf is missing");
        }

        File stageDir = new File(context.getCacheDir(), "questcraft_install");
        if (!stageDir.exists() && !stageDir.mkdirs()) {
            return new Result(false, "Could not create staging directory");
        }

        File stagedAccount = new File(stageDir, accountFile.getName());
        File stagedConf = new File(stageDir, "launcher.conf");

        try {
            copy(accountFile, stagedAccount);
            copy(launcherConf, stagedConf);
        } catch (IOException e) {
            return new Result(false, "Could not stage generated files: " + e.getMessage());
        }

        return doTheSuperHere(stagedAccount, stagedConf);
    }

    public static String diagnose(Context context) {
        if (context == null) return "Context is missing";

        StringBuilder out = new StringBuilder();
        out.append("QuestCraft package: ").append(QUESTCRAFT_PACKAGE).append('\n');
        out.append("QCOAM cache: ").append(context.getCacheDir().getAbsolutePath()).append('\n');
        out.append("Privileged diagnostics: [DO THE SUPER HERE]");
        return out.toString();
    }

    private static Result doTheSuperHere(File stagedAccount, File stagedConf) {
        /*
         * ================================================================
         *                     [ DO THE SUPER HERE ]
         * ================================================================
         *
         * The generated files are ready and validated:
         *
         *   stagedAccount -> stagedAccount.getAbsolutePath()
         *   stagedConf    -> stagedConf.getAbsolutePath()
         *
         * Intended privileged-side responsibilities from the project spec:
         *
         *   1. Resolve the current UID for QUESTCRAFT_PACKAGE dynamically.
         *   2. Locate QuestCraft's private files/accounts directory.
         *   3. Back up an existing matching account file, if present.
         *   4. Copy stagedAccount into QuestCraft's accounts directory.
         *   5. Back up the existing launcher.conf, if present.
         *   6. Copy stagedConf to QuestCraft's expected launcher.conf path.
         *   7. Repair ownership/permissions for files created by the installer.
         *   8. Return Result(true, "...") on success or Result(false, "...")
         *      with useful diagnostics on failure.
         *
         * Keep all privileged/device-specific code inside this method so the
         * rest of the installer remains testable without elevated access.
         * ================================================================
         */
        return new Result(false,
                "[DO THE SUPER HERE] Files staged at: "
                        + stagedAccount.getAbsolutePath() + " and "
                        + stagedConf.getAbsolutePath());
    }

    private static void copy(File source, File target) throws IOException {
        FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(target);
        try {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
        } finally {
            try {
                in.close();
            } finally {
                out.close();
            }
        }
    }
}
