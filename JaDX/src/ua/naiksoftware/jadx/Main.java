package jadx;

import jadx.codegen.CodeGen;
import jadx.dex.info.ClassInfo;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.RootNode;
import jadx.dex.visitors.BlockMakerVisitor;
import jadx.dex.visitors.ClassModifier;
import jadx.dex.visitors.CodeShrinker;
import jadx.dex.visitors.ConstInlinerVisitor;
import jadx.dex.visitors.DotGraphVisitor;
import jadx.dex.visitors.EnumVisitor;
import jadx.dex.visitors.FallbackModeVisitor;
import jadx.dex.visitors.IDexTreeVisitor;
import jadx.dex.visitors.MethodInlinerVisitor;
import jadx.dex.visitors.ModVisitor;
import jadx.dex.visitors.regions.CheckRegions;
import jadx.dex.visitors.regions.CleanRegions;
import jadx.dex.visitors.regions.PostRegionVisitor;
import jadx.dex.visitors.regions.ProcessVariables;
import jadx.dex.visitors.regions.RegionMakerVisitor;
import jadx.dex.visitors.typeresolver.FinishTypeResolver;
import jadx.dex.visitors.typeresolver.TypeResolver;
import jadx.utils.ErrorsCounter;
import jadx.utils.exceptions.JadxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ua.naiksoftware.jadx.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class Main {

    private static final String tag = "Main";

    static {
        if (Consts.DEBUG) {
            MainActivity.decompiler.update(tag, "debug enabled", Level.INFO);
        }
        if (Main.class.desiredAssertionStatus()) {
            MainActivity.decompiler.update(tag, "assertions enabled", Level.INFO);
        }
    }

    public static int run(JadxArgs args) {
        int errorCount;
        try {
            RootNode root = new RootNode(args);
            MainActivity.decompiler.update(tag, "loading ...", Level.INFO);
            root.load();
            MainActivity.decompiler.update(tag, "processing ...", Level.INFO);
            root.init();

            int threadsCount = args.getThreadsCount();
            MainActivity.decompiler.update(tag, "processing threads count: {}" + threadsCount, Level.DEBUG);

            List<IDexTreeVisitor> passes = getPassesList(args);
            if (threadsCount == 1) {
                for (ClassNode cls : root.getClasses()) {
                    ProcessClass job = new ProcessClass(cls, passes);
                    job.run();
                }
            } else {
                ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
                for (ClassNode cls : root.getClasses()) {
                    ProcessClass job = new ProcessClass(cls, passes);
                    executor.execute(job);
                }
                executor.shutdown();
                executor.awaitTermination(100, TimeUnit.DAYS);
            }
        } catch (Throwable e) {
            MainActivity.decompiler.update(tag, "jadx error: " + e.getMessage(), Level.ERROR);
        } finally {
            errorCount = ErrorsCounter.getErrorCount();
            if (errorCount != 0) {
                ErrorsCounter.printReport();
            }

            // clear resources if we use jadx as a library
            ClassInfo.clearCache();
            ErrorsCounter.reset();
        }
        MainActivity.decompiler.update(tag, "done", Level.INFO);
        return errorCount;
    }

    private static List<IDexTreeVisitor> getPassesList(JadxArgs args) {
        List<IDexTreeVisitor> passes = new ArrayList<IDexTreeVisitor>();
        if (args.isFallbackMode()) {
            passes.add(new FallbackModeVisitor());
        } else {
            passes.add(new BlockMakerVisitor());

            passes.add(new TypeResolver());
            passes.add(new ConstInlinerVisitor());
            passes.add(new FinishTypeResolver());

            if (args.isRawCFGOutput()) {
                passes.add(new DotGraphVisitor(args.getOutDir(), false, true));
            }

            passes.add(new ModVisitor());
            passes.add(new EnumVisitor());

            if (args.isCFGOutput()) {
                passes.add(new DotGraphVisitor(args.getOutDir(), false));
            }

            passes.add(new RegionMakerVisitor());
            passes.add(new PostRegionVisitor());

            passes.add(new CodeShrinker());
            passes.add(new ProcessVariables());
            passes.add(new CheckRegions());
            if (args.isCFGOutput()) {
                passes.add(new DotGraphVisitor(args.getOutDir(), true));
            }

            passes.add(new MethodInlinerVisitor());
            passes.add(new ClassModifier());
            passes.add(new CleanRegions());
        }
        passes.add(new CodeGen(args));
        return passes;
    }

    public static void main(String[] args) {
        JadxArgs jadxArgs = new JadxArgs();
        try {
            jadxArgs.parse(args);
            if (jadxArgs.isPrintHelp()) {
                jadxArgs.printUsage();
                //System.exit(0);
            }
        } catch (JadxException e) {
            MainActivity.decompiler.update(tag, "Error: " + e.getMessage(), Level.ERROR);
            //System.exit(1);
        }

        if (jadxArgs.isVerbose()) {
            //ch.qos.logback.classic.Logger rootLogger =
           //         (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            //rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        }

        int result = run(jadxArgs);
        //System.exit(result);
    }
}
