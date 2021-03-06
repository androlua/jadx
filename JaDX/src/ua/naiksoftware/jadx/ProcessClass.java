package jadx;

import android.util.Log;
import jadx.dex.nodes.ClassNode;
import jadx.dex.visitors.DepthTraverser;
import jadx.dex.visitors.IDexTreeVisitor;
import jadx.utils.exceptions.DecodeException;

import java.util.List;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

class ProcessClass implements Runnable {
	private final String tag = getClass().getName();

	private final ClassNode cls;
	private final List<IDexTreeVisitor> passes;

	ProcessClass(ClassNode cls, List<IDexTreeVisitor> passes) {
		this.cls = cls;
		this.passes = passes;
	}

	@Override
	public void run() {
		try {
			cls.load();
			for (IDexTreeVisitor visitor : passes) {
				DepthTraverser.visit(visitor, cls);
			}
		} catch (DecodeException e) {
			Log.e(tag, "Decode exception: " + cls, e);
		} finally {
			cls.unload();
		}
	}
}
